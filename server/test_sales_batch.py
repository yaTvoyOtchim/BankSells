import importlib
import os
import sys
import tempfile
import unittest
from pathlib import Path

from fastapi.testclient import TestClient


class SalesBatchTest(unittest.TestCase):
    def setUp(self):
        self.tmpdir = tempfile.TemporaryDirectory()
        os.environ["SALES_DB"] = str(Path(self.tmpdir.name) / "sales.db")
        os.environ["SALES_TZ"] = "Europe/Istanbul"
        os.environ["JWT_SECRET"] = "test-secret-with-at-least-thirty-two-bytes"
        sys.path.insert(0, str(Path(__file__).resolve().parent))
        for name in ("main", "db", "auth"):
            sys.modules.pop(name, None)
        self.main = importlib.import_module("main")
        self.client = TestClient(self.main.app)
        self._create_active_user("vtb70336144", "Admin VTB", "ADMIN", "VtbAdmin123", "office-7")
        self._create_active_user("vtb70336145", "Employee VTB", "EMPLOYEE", "VtbUser123", "office-7-main")

        self.admin_token = self._login("vtb70336144", "VtbAdmin123")
        self.employee_token = self._login("vtb70336145", "VtbUser123")

    def tearDown(self):
        self.tmpdir.cleanup()

    def _login(self, employee_id: str, password: str) -> str:
        response = self.client.post(
            "/api/auth/login",
            json={"employeeId": employee_id, "password": password, "deviceId": "test"},
        )
        self.assertEqual(response.status_code, 200, response.text)
        return response.json()["accessToken"]

    def _create_active_user(self, employee_id: str, full_name: str, role: str, password: str, org_code: str) -> None:
        with self.main.d.connect() as db:
            org = db.execute("SELECT id, name FROM org_units WHERE code=?", (org_code,)).fetchone()
            self.assertIsNotNone(org)
            user_id = self.main.d.new_id()
            now = self.main.now_utc()
            db.execute(
                "INSERT INTO users(id, employee_id, full_name, branch, role, registration_status, "
                "primary_org_unit_id, password_hash, must_change_password, active, created_at, updated_at) "
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                (
                    user_id, employee_id, full_name, org["name"], role, "ACTIVE", org["id"],
                    self.main.a.hash_password(password), 0, 1, now, now,
                ),
            )
            db.execute(
                "INSERT INTO user_assignments(id, user_id, org_unit_id, role_at_unit, started_at, comment) "
                "VALUES (?,?,?,?,?,?)",
                (self.main.d.new_id(), user_id, org["id"], role, now, "test user"),
            )

    def _product_id(self, code: str) -> str:
        with self.main.d.connect() as db:
            row = db.execute("SELECT id FROM products WHERE code=?", (code,)).fetchone()
        self.assertIsNotNone(row)
        return row["id"]

    def test_batch_sale_creates_multiple_products_for_one_client(self):
        response = self.client.post(
            "/api/sales/batch",
            headers={"Authorization": f"Bearer {self.employee_token}"},
            json={
                "clientLast4": "1234",
                "items": [
                    {"category": "DEBIT_CARD_STICKER_APPLICATION", "quantity": 1},
                    {"category": "STICKER", "quantity": 1},
                    {"category": "AUTO_PAYMENTS", "quantity": 1},
                    {"category": "PDS", "quantity": 1, "amount": 50000},
                ],
            },
        )

        self.assertEqual(response.status_code, 200, response.text)
        sales = response.json()
        self.assertEqual(len(sales), 4)
        self.assertEqual({sale["clientLast4"] for sale in sales}, {"1234"})
        self.assertEqual(len({sale["saleGroupId"] for sale in sales}), 1)
        self.assertEqual(
            [sale["category"] for sale in sales],
            ["DEBIT_CARD_STICKER_APPLICATION", "STICKER", "AUTO_PAYMENTS", "PDS"],
        )
        self.assertEqual(sales[-1]["amount"], 50000)

    def test_batch_sale_requires_amount_for_money_products(self):
        response = self.client.post(
            "/api/sales/batch",
            headers={"Authorization": f"Bearer {self.employee_token}"},
            json={
                "clientLast4": "5678",
                "items": [{"category": "OPIF", "quantity": 1}],
            },
        )

        self.assertEqual(response.status_code, 400)
        self.assertIn("сумм", response.text.lower())

    def test_batch_sale_uses_office_product_snapshot(self):
        response = self.client.post(
            "/api/sales/batch",
            headers={"Authorization": f"Bearer {self.employee_token}"},
            json={
                "clientLast4": "1111",
                "items": [{"productId": self._product_id("PDS"), "quantity": 1, "amount": 10000}],
            },
        )

        self.assertEqual(response.status_code, 200, response.text)
        sale = response.json()[0]
        self.assertEqual(sale["category"], "PDS")
        self.assertEqual(sale["productTitle"], "ПДС")
        self.assertEqual(sale["points"], 1)
        self.assertEqual(sale["pointsTotal"], 1)
        self.assertEqual(sale["requiresAmount"], True)

    def test_manager_cannot_add_sales(self):
        response = self.client.post(
            "/api/sales/batch",
            headers={"Authorization": f"Bearer {self.admin_token}"},
            json={
                "clientLast4": "2222",
                "items": [{"productId": self._product_id("PDS"), "quantity": 1, "amount": 10000}],
            },
        )

        self.assertEqual(response.status_code, 403)

    def test_my_report_sums_points_from_sale_snapshots(self):
        product_id = self._product_id("PDS")
        with self.main.d.connect() as db:
            db.execute(
                "UPDATE office_product_settings SET points=4 WHERE product_id=?",
                (product_id,),
            )
        response = self.client.post(
            "/api/sales/batch",
            headers={"Authorization": f"Bearer {self.employee_token}"},
            json={
                "clientLast4": "3333",
                "items": [{"productId": product_id, "quantity": 2, "amount": 10000}],
            },
        )
        self.assertEqual(response.status_code, 200, response.text)

        report = self.client.get(
            "/api/reports/my",
            headers={"Authorization": f"Bearer {self.employee_token}"},
        )

        self.assertEqual(report.status_code, 200, report.text)
        body = report.json()
        self.assertEqual(body["todayPoints"], 8)
        self.assertEqual(body["monthPoints"], 8)
        pds = next(item for item in body["byCategory"] if item["category"] == "PDS")
        self.assertEqual(pds["totalPoints"], 8)


if __name__ == "__main__":
    unittest.main()
