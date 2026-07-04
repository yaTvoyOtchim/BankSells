import importlib
import os
import sys
import tempfile
import unittest
from pathlib import Path

from fastapi.testclient import TestClient


class ProductCatalogTest(unittest.TestCase):
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

    def product_id(self, code: str) -> str:
        with self.main.d.connect() as db:
            row = db.execute("SELECT id FROM products WHERE code=?", (code,)).fetchone()
        self.assertIsNotNone(row)
        return row["id"]

    def test_default_products_are_seeded_for_demo_office(self):
        with self.main.d.connect() as db:
            pds = db.execute("SELECT * FROM products WHERE code='PDS'").fetchone()
            self.assertIsNotNone(pds)
            setting = db.execute(
                "SELECT ops.* FROM office_product_settings ops "
                "JOIN org_units o ON o.id=ops.org_unit_id "
                "WHERE ops.product_id=? AND o.code='office-7'",
                (pds["id"],),
            ).fetchone()
            self.assertIsNotNone(setting)
            self.assertEqual(setting["requires_amount"], 1)
            self.assertEqual(setting["active"], 1)

    def test_employee_gets_active_products_for_own_office(self):
        response = self.client.get(
            "/api/products/active",
            headers={"Authorization": f"Bearer {self.employee_token}"},
        )

        self.assertEqual(response.status_code, 200, response.text)
        products = response.json()
        pds = next(product for product in products if product["code"] == "PDS")
        self.assertEqual(pds["title"], "ПДС")
        self.assertEqual(pds["points"], 1)
        self.assertEqual(pds["requiresAmount"], True)
        self.assertEqual(pds["active"], True)

    def test_employee_cannot_edit_office_product(self):
        response = self.client.patch(
            f"/api/management/offices/office-7/products/{self.product_id('PDS')}",
            headers={"Authorization": f"Bearer {self.employee_token}"},
            json={"points": 5},
        )

        self.assertEqual(response.status_code, 403)

    def test_manager_updates_office_product_and_history(self):
        product_id = self.product_id("PDS")
        response = self.client.patch(
            f"/api/management/offices/office-7/products/{product_id}",
            headers={"Authorization": f"Bearer {self.admin_token}"},
            json={"points": 5, "requiresAmount": True, "active": True},
        )

        self.assertEqual(response.status_code, 200, response.text)
        updated = response.json()
        self.assertEqual(updated["points"], 5)
        self.assertEqual(updated["requiresAmount"], True)
        with self.main.d.connect() as db:
            rows = db.execute(
                "SELECT * FROM product_change_history WHERE product_id=? AND field_name='points'",
                (product_id,),
            ).fetchall()
        self.assertGreaterEqual(len(rows), 1)

    def test_admin_creates_product_hidden_for_office(self):
        response = self.client.post(
            "/api/admin/products",
            headers={"Authorization": f"Bearer {self.admin_token}"},
            json={"code": "NEW_PRODUCT", "title": "Новый продукт", "groupName": "Тест"},
        )

        self.assertEqual(response.status_code, 200, response.text)
        product = response.json()
        self.assertEqual(product["code"], "NEW_PRODUCT")
        office_products = self.client.get(
            "/api/management/offices/office-7/products",
            headers={"Authorization": f"Bearer {self.admin_token}"},
        ).json()
        created_setting = next(item for item in office_products if item["code"] == "NEW_PRODUCT")
        self.assertEqual(created_setting["active"], False)

    def test_employee_cannot_create_product(self):
        response = self.client.post(
            "/api/admin/products",
            headers={"Authorization": f"Bearer {self.employee_token}"},
            json={"code": "NOPE", "title": "Нельзя", "groupName": "Тест"},
        )

        self.assertEqual(response.status_code, 403)


if __name__ == "__main__":
    unittest.main()
