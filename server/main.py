import csv
import io
import os
import re
from datetime import datetime, timedelta, timezone
from zoneinfo import ZoneInfo

from fastapi import Depends, FastAPI, HTTPException, Query
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from pydantic import BaseModel, Field

import auth as a
import db as d

TZ = ZoneInfo(os.environ.get("SALES_TZ", "Europe/Istanbul"))

CATEGORY_TITLES = {code: title for code, title, _ in d.DEFAULT_PRODUCTS}
CATEGORIES = set(CATEGORY_TITLES)
REQUIRES_AMOUNT = set(d.AMOUNT_REQUIRED_PRODUCT_CODES)

EMPLOYEE_ID_RE = re.compile(r"^vtb\d+$")
PENDING_ASSIGNMENT = "PENDING_ASSIGNMENT"
ACTIVE = "ACTIVE"
BLOCKED = "BLOCKED"
DEACTIVATED = "DEACTIVATED"
MANAGER_ROLES = {"TEAM_LEAD", "OFFICE_MANAGER", "REGIONAL_MANAGER", "DIRECTOR", "ADMIN"}
WORK_ROLES = {"EMPLOYEE", "TEAM_LEAD", "OFFICE_MANAGER", "REGIONAL_MANAGER", "DIRECTOR", "ADMIN"}

app = FastAPI(title="VTB Sales API")
bearer = HTTPBearer(auto_error=False)
d.init_db()


class LoginIn(BaseModel):
    employeeId: str
    password: str
    deviceId: str


class RegisterIn(BaseModel):
    employeeId: str
    fullName: str = Field(min_length=2, max_length=120)
    password: str = Field(min_length=8)
    deviceId: str


class RefreshIn(BaseModel):
    refreshToken: str
    deviceId: str


class ChangePasswordIn(BaseModel):
    oldPassword: str
    newPassword: str = Field(min_length=8)


class SaleIn(BaseModel):
    category: str | None = None
    productId: str | None = None
    clientLast4: str
    amount: float | None = None
    quantity: int = Field(default=1, ge=1, le=99)
    comment: str | None = None


class SaleBatchItemIn(BaseModel):
    category: str | None = None
    productId: str | None = None
    amount: float | None = None
    quantity: int = Field(default=1, ge=1, le=99)
    comment: str | None = None


class SaleBatchIn(BaseModel):
    clientLast4: str
    items: list[SaleBatchItemIn] = Field(min_length=1)


class ProductSettingPatchIn(BaseModel):
    active: bool | None = None
    points: float | None = Field(default=None, ge=0)
    requiresAmount: bool | None = None
    countsTowardPlan: bool | None = None
    sortOrder: int | None = None


class ProductCreateIn(BaseModel):
    code: str = Field(min_length=2, max_length=60)
    title: str = Field(min_length=1, max_length=120)
    groupName: str = ""
    description: str | None = None


class AssignUserIn(BaseModel):
    orgUnitId: str | None = None
    role: str = "EMPLOYEE"
    comment: str | None = None


def now_utc() -> str:
    return datetime.now(timezone.utc).isoformat()


def normalize_employee_id(employee_id: str) -> str:
    return employee_id.strip().lower()


def validate_employee_id(employee_id: str) -> str:
    normalized = normalize_employee_id(employee_id)
    if not EMPLOYEE_ID_RE.fullmatch(normalized):
        raise HTTPException(400, "Табельный номер должен быть в формате vtb70336144")
    return normalized


def validate_password(password: str) -> None:
    if len(password) < 8:
        raise HTTPException(400, "Пароль должен быть минимум 8 символов")
    if not any(c.isdigit() for c in password) or not any(c.isalpha() for c in password):
        raise HTTPException(400, "Пароль должен содержать буквы и цифры")


def validate_client_last4(value: str) -> str:
    cleaned = value.strip()
    if not re.fullmatch(r"\d{4}", cleaned):
        raise HTTPException(400, "Укажите последние 4 цифры телефона клиента")
    return cleaned


def validate_sale_item(category: str, amount: float | None) -> None:
    if category not in CATEGORIES:
        raise HTTPException(400, "Неизвестная категория")
    if category in REQUIRES_AMOUNT and (amount is None or amount <= 0):
        title = CATEGORY_TITLES.get(category, category)
        raise HTTPException(400, f"Для продукта {title} нужно указать сумму")


def user_dto(row) -> dict:
    return {
        "id": row["id"],
        "employeeId": row["employee_id"],
        "fullName": row["full_name"],
        "branch": row["branch"],
        "role": row["role"],
        "registrationStatus": row["registration_status"],
        "orgUnitId": row["primary_org_unit_id"],
    }


def audit(db, user_id: str | None, action: str, details: str = ""):
    db.execute(
        "INSERT INTO audit_log(user_id, action, details, created_at) VALUES (?,?,?,?)",
        (user_id, action, details, now_utc()),
    )


def current_user(creds: HTTPAuthorizationCredentials = Depends(bearer)) -> dict:
    if not creds:
        raise HTTPException(401, "Нет токена")
    try:
        payload = a.decode_access_token(creds.credentials)
    except Exception:
        raise HTTPException(401, "Токен недействителен")
    with d.connect() as db:
        row = db.execute("SELECT * FROM users WHERE id=? AND active=1", (payload["sub"],)).fetchone()
    if not row:
        raise HTTPException(401, "Пользователь не найден или деактивирован")
    if row["registration_status"] in (BLOCKED, DEACTIVATED):
        raise HTTPException(403, "Учётная запись заблокирована")
    return dict(row)


def require_work_access(user: dict = Depends(current_user)) -> dict:
    if user["registration_status"] != ACTIVE:
        raise HTTPException(403, "Профиль ожидает привязки к офису")
    if user["role"] not in WORK_ROLES:
        raise HTTPException(403, "Недостаточно прав")
    return user


def require_sales_access(user: dict = Depends(require_work_access)) -> dict:
    if user["role"] != "EMPLOYEE":
        raise HTTPException(403, "Продажи может вносить только сотрудник")
    return user


def require_manager(user: dict = Depends(require_work_access)) -> dict:
    if user["role"] not in MANAGER_ROLES:
        raise HTTPException(403, "Только для руководителя")
    return user


def require_admin(user: dict = Depends(require_work_access)) -> dict:
    if user["role"] != "ADMIN":
        raise HTTPException(403, "Только для администратора")
    return user


def active_assignment(db, user_id: str):
    return db.execute(
        "SELECT * FROM user_assignments WHERE user_id=? AND ended_at IS NULL ORDER BY started_at DESC LIMIT 1",
        (user_id,),
    ).fetchone()


def descendant_org_ids(db, org_unit_id: str | None) -> list[str]:
    if not org_unit_id:
        return []
    rows = db.execute(
        """
        WITH RECURSIVE tree(id) AS (
            SELECT id FROM org_units WHERE id=?
            UNION ALL
            SELECT o.id FROM org_units o JOIN tree t ON o.parent_id=t.id
        )
        SELECT id FROM tree
        """,
        (org_unit_id,),
    ).fetchall()
    return [r["id"] for r in rows]


def visible_org_ids(db, user: dict) -> list[str] | None:
    if user["role"] == "ADMIN":
        return None
    return descendant_org_ids(db, user["primary_org_unit_id"])


def org_filter_sql(column: str, org_ids: list[str] | None) -> tuple[str, list]:
    if org_ids is None:
        return "", []
    if not org_ids:
        return " AND 1=0", []
    return f" AND {column} IN ({','.join('?' for _ in org_ids)})", list(org_ids)


def org_display_name(db, org_unit_id: str | None) -> str:
    if not org_unit_id:
        return ""
    row = db.execute("SELECT name FROM org_units WHERE id=?", (org_unit_id,)).fetchone()
    return row["name"] if row else ""


def office_for_org_unit(db, org_unit_id: str | None) -> str | None:
    current = org_unit_id
    while current:
        row = db.execute("SELECT id, type, parent_id FROM org_units WHERE id=?", (current,)).fetchone()
        if not row:
            return None
        if row["type"] == "OFFICE":
            return row["id"]
        current = row["parent_id"]
    return None


def resolve_org_unit(db, identifier: str):
    return db.execute(
        "SELECT * FROM org_units WHERE id=? OR code=?",
        (identifier, identifier),
    ).fetchone()


def ensure_visible_org(db, manager: dict, org_unit_id: str) -> None:
    ids = visible_org_ids(db, manager)
    if ids is not None and org_unit_id not in ids:
        raise HTTPException(403, "Подразделение вне вашей структуры")


def product_setting_dto(row) -> dict:
    return {
        "id": row["setting_id"],
        "productId": row["product_id"],
        "code": row["code"],
        "title": row["title"],
        "groupName": row["group_name"] or "",
        "active": bool(row["active"]),
        "points": row["points"],
        "requiresAmount": bool(row["requires_amount"]),
        "countsTowardPlan": bool(row["counts_toward_plan"]),
        "sortOrder": row["sort_order"],
    }


def product_dto(row) -> dict:
    return {
        "id": row["id"],
        "productId": row["id"],
        "code": row["code"],
        "title": row["title"],
        "groupName": row["group_name"] or "",
        "description": row["description"],
        "archived": bool(row["archived"]),
    }


def product_settings_rows(db, office_id: str, only_active: bool = False):
    query = (
        "SELECT ops.id setting_id, ops.product_id, ops.active, ops.points, ops.requires_amount, "
        "ops.counts_toward_plan, ops.sort_order, p.code, p.title, p.group_name, p.archived "
        "FROM office_product_settings ops JOIN products p ON p.id=ops.product_id "
        "WHERE ops.org_unit_id=? AND p.archived=0"
    )
    params: list = [office_id]
    if only_active:
        query += " AND ops.active=1"
    query += " ORDER BY ops.sort_order, p.title"
    return db.execute(query, params).fetchall()


def resolve_sale_product(db, *, product_id: str | None, category: str | None, org_unit_id: str, amount: float | None):
    office_id = office_for_org_unit(db, org_unit_id)
    if not office_id:
        raise HTTPException(403, "Не найден офис сотрудника")
    if not product_id and not category:
        raise HTTPException(400, "Не выбран продукт")

    where = "p.id=?" if product_id else "p.code=?"
    value = product_id if product_id else category
    row = db.execute(
        "SELECT p.id product_id, p.code, p.title, p.group_name, p.archived, "
        "ops.id setting_id, ops.active, ops.points, ops.requires_amount, ops.counts_toward_plan, ops.sort_order "
        "FROM products p JOIN office_product_settings ops ON ops.product_id=p.id "
        f"WHERE {where} AND ops.org_unit_id=?",
        (value, office_id),
    ).fetchone()
    if not row or row["archived"] or not row["active"]:
        raise HTTPException(400, "Продукт недоступен для этого офиса")
    if row["requires_amount"] and (amount is None or amount <= 0):
        raise HTTPException(400, f"Для продукта {row['title']} нужно указать сумму")
    return row


def resolve_user(db, identifier: str):
    normalized = normalize_employee_id(identifier)
    return db.execute(
        "SELECT * FROM users WHERE id=? OR employee_id=?",
        (identifier, normalized),
    ).fetchone()


def day_bounds_utc(local_date) -> tuple[str, str]:
    start = datetime.combine(local_date, datetime.min.time(), TZ)
    end = start + timedelta(days=1)
    return start.astimezone(timezone.utc).isoformat(), end.astimezone(timezone.utc).isoformat()


def period_bounds(period: str) -> tuple[str, str]:
    today = datetime.now(TZ).date()
    if period == "today":
        return day_bounds_utc(today)
    if period == "week":
        start = today - timedelta(days=today.weekday())
    else:
        start = today.replace(day=1)
    start_utc, _ = day_bounds_utc(start)
    _, end_utc = day_bounds_utc(today)
    return start_utc, end_utc


@app.post("/api/auth/register")
def register(body: RegisterIn):
    employee_id = validate_employee_id(body.employeeId)
    validate_password(body.password)
    created = now_utc()
    with d.connect() as db:
        existing = db.execute("SELECT id FROM users WHERE employee_id=?", (employee_id,)).fetchone()
        if existing:
            raise HTTPException(409, "Такой табельный номер уже зарегистрирован")
        user_id = d.new_id()
        db.execute(
            "INSERT INTO users(id, employee_id, full_name, branch, role, registration_status, password_hash, "
            "must_change_password, active, created_at, updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?)",
            (
                user_id, employee_id, body.fullName.strip(), "", "EMPLOYEE", PENDING_ASSIGNMENT,
                a.hash_password(body.password), 0, 1, created, created,
            ),
        )
        row = db.execute("SELECT * FROM users WHERE id=?", (user_id,)).fetchone()
        refresh = a.issue_refresh_token(db, user_id, body.deviceId)
        audit(db, user_id, "registered", employee_id)
        return {
            "accessToken": a.make_access_token(user_id, "EMPLOYEE"),
            "refreshToken": refresh,
            "mustChangePassword": False,
            "user": user_dto(row),
        }


@app.post("/api/auth/login")
def login(body: LoginIn):
    employee_id = normalize_employee_id(body.employeeId)
    with d.connect() as db:
        row = db.execute("SELECT * FROM users WHERE employee_id=?", (employee_id,)).fetchone()
        if not row or not row["active"] or row["registration_status"] == DEACTIVATED:
            raise HTTPException(401, "Неверный табельный номер или пароль")
        if row["locked"] or row["registration_status"] == BLOCKED:
            raise HTTPException(423, "Учётная запись заблокирована")
        if not a.check_password(body.password, row["password_hash"]):
            failed = row["failed_attempts"] + 1
            locked = 1 if failed >= a.MAX_FAILED_ATTEMPTS else 0
            db.execute("UPDATE users SET failed_attempts=?, locked=?, updated_at=? WHERE id=?", (failed, locked, now_utc(), row["id"]))
            audit(db, row["id"], "login_failed", f"attempt {failed}")
            raise HTTPException(423 if locked else 401, "Учётная запись заблокирована" if locked else "Неверный табельный номер или пароль")
        db.execute("UPDATE users SET failed_attempts=0, updated_at=? WHERE id=?", (now_utc(), row["id"]))
        refresh = a.issue_refresh_token(db, row["id"], body.deviceId)
        audit(db, row["id"], "login", body.deviceId)
        return {
            "accessToken": a.make_access_token(row["id"], row["role"]),
            "refreshToken": refresh,
            "mustChangePassword": bool(row["must_change_password"]),
            "user": user_dto(row),
        }


@app.post("/api/auth/refresh")
def refresh(body: RefreshIn):
    with d.connect() as db:
        user_id = a.consume_refresh_token(db, body.refreshToken, body.deviceId)
        if not user_id:
            raise HTTPException(401, "Refresh-токен недействителен")
        row = db.execute(
            "SELECT * FROM users WHERE id=? AND active=1 AND locked=0 AND registration_status NOT IN (?,?)",
            (user_id, BLOCKED, DEACTIVATED),
        ).fetchone()
        if not row:
            raise HTTPException(401, "Доступ запрещён")
        new_refresh = a.issue_refresh_token(db, user_id, body.deviceId)
        return {
            "accessToken": a.make_access_token(row["id"], row["role"]),
            "refreshToken": new_refresh,
            "mustChangePassword": bool(row["must_change_password"]),
            "user": user_dto(row),
        }


@app.post("/api/auth/change-password")
def change_password(body: ChangePasswordIn, user: dict = Depends(current_user)):
    validate_password(body.newPassword)
    if not a.check_password(body.oldPassword, user["password_hash"]):
        raise HTTPException(400, "Текущий пароль неверен")
    with d.connect() as db:
        db.execute(
            "UPDATE users SET password_hash=?, must_change_password=0, updated_at=? WHERE id=?",
            (a.hash_password(body.newPassword), now_utc(), user["id"]),
        )
        audit(db, user["id"], "password_changed")
    return {"ok": True}


@app.post("/api/auth/logout")
def logout(user: dict = Depends(current_user)):
    with d.connect() as db:
        a.revoke_all_tokens(db, user["id"])
        audit(db, user["id"], "logout")
    return {"ok": True}


@app.get("/api/profile")
def profile(user: dict = Depends(current_user)):
    return user_dto(user)


@app.get("/api/products/active")
def active_products(user: dict = Depends(require_work_access)):
    with d.connect() as db:
        assignment = active_assignment(db, user["id"])
        if not assignment:
            raise HTTPException(403, "Сотрудник не привязан к офису")
        office_id = office_for_org_unit(db, assignment["org_unit_id"])
        if not office_id:
            raise HTTPException(403, "Не найден офис сотрудника")
        rows = product_settings_rows(db, office_id, only_active=True)
    return [product_setting_dto(row) for row in rows]


@app.get("/api/management/offices/{org_unit_id}/products")
def management_office_products(org_unit_id: str, manager: dict = Depends(require_manager)):
    with d.connect() as db:
        org = resolve_org_unit(db, org_unit_id)
        if not org:
            raise HTTPException(404, "Офис не найден")
        office_id = office_for_org_unit(db, org["id"]) if org["type"] != "OFFICE" else org["id"]
        if not office_id:
            raise HTTPException(400, "Выберите офис или команду внутри офиса")
        ensure_visible_org(db, manager, office_id)
        rows = product_settings_rows(db, office_id, only_active=False)
    return [product_setting_dto(row) for row in rows]


@app.patch("/api/management/offices/{org_unit_id}/products/{product_id}")
def update_office_product(org_unit_id: str, product_id: str, body: ProductSettingPatchIn,
                          manager: dict = Depends(require_manager)):
    with d.connect() as db:
        org = resolve_org_unit(db, org_unit_id)
        if not org:
            raise HTTPException(404, "Офис не найден")
        office_id = office_for_org_unit(db, org["id"]) if org["type"] != "OFFICE" else org["id"]
        if not office_id:
            raise HTTPException(400, "Выберите офис или команду внутри офиса")
        ensure_visible_org(db, manager, office_id)
        row = db.execute(
            "SELECT ops.*, p.code, p.title, p.group_name FROM office_product_settings ops "
            "JOIN products p ON p.id=ops.product_id WHERE ops.org_unit_id=? AND ops.product_id=?",
            (office_id, product_id),
        ).fetchone()
        if not row:
            raise HTTPException(404, "Настройка продукта не найдена")

        changes: list[tuple[str, object, object]] = []
        updates: list[str] = []
        params: list = []
        field_map = [
            ("active", body.active, lambda v: 1 if v else 0),
            ("points", body.points, float),
            ("requires_amount", body.requiresAmount, lambda v: 1 if v else 0),
            ("counts_toward_plan", body.countsTowardPlan, lambda v: 1 if v else 0),
            ("sort_order", body.sortOrder, int),
        ]
        for field_name, value, convert in field_map:
            if value is None:
                continue
            converted = convert(value)
            if row[field_name] != converted:
                changes.append((field_name, row[field_name], converted))
                updates.append(f"{field_name}=?")
                params.append(converted)
        changed_at = now_utc()
        if updates:
            updates.extend(["updated_by_user_id=?", "updated_at=?"])
            params.extend([manager["id"], changed_at, row["id"]])
            db.execute(f"UPDATE office_product_settings SET {', '.join(updates)} WHERE id=?", params)
            for field_name, old_value, new_value in changes:
                db.execute(
                    "INSERT INTO product_change_history(id, product_id, org_unit_id, changed_by_user_id, "
                    "changed_at, field_name, old_value, new_value) VALUES (?,?,?,?,?,?,?,?)",
                    (
                        d.new_id(), product_id, office_id, manager["id"], changed_at, field_name,
                        str(old_value), str(new_value),
                    ),
                )
            audit(db, manager["id"], "product_setting_updated", f"{row['code']} {office_id}")
        updated = db.execute(
            "SELECT ops.id setting_id, ops.product_id, ops.active, ops.points, ops.requires_amount, "
            "ops.counts_toward_plan, ops.sort_order, p.code, p.title, p.group_name "
            "FROM office_product_settings ops JOIN products p ON p.id=ops.product_id WHERE ops.id=?",
            (row["id"],),
        ).fetchone()
    return product_setting_dto(updated)


@app.get("/api/admin/products")
def admin_products(admin: dict = Depends(require_admin)):
    with d.connect() as db:
        rows = db.execute("SELECT * FROM products ORDER BY archived, title").fetchall()
    return [product_dto(row) for row in rows]


@app.post("/api/admin/products")
def create_product(body: ProductCreateIn, admin: dict = Depends(require_admin)):
    code = body.code.strip().upper()
    if not re.fullmatch(r"[A-Z0-9_]+", code):
        raise HTTPException(400, "Код продукта должен содержать латинские буквы, цифры или _")
    title = body.title.strip()
    group_name = body.groupName.strip()
    created = now_utc()
    with d.connect() as db:
        existing = db.execute("SELECT id FROM products WHERE code=?", (code,)).fetchone()
        if existing:
            raise HTTPException(409, "Продукт с таким кодом уже существует")
        product_id = d.new_id()
        db.execute(
            "INSERT INTO products(id, code, title, group_name, description, archived, created_by_user_id, created_at, updated_at) "
            "VALUES (?,?,?,?,?,0,?,?,?)",
            (product_id, code, title, group_name, body.description, admin["id"], created, created),
        )
        offices = db.execute("SELECT id FROM org_units WHERE type='OFFICE' AND active=1").fetchall()
        for office in offices:
            max_order = db.execute(
                "SELECT COALESCE(MAX(sort_order), 0) value FROM office_product_settings WHERE org_unit_id=?",
                (office["id"],),
            ).fetchone()["value"]
            db.execute(
                "INSERT INTO office_product_settings(id, product_id, org_unit_id, active, points, "
                "requires_amount, counts_toward_plan, sort_order, updated_by_user_id, created_at, updated_at) "
                "VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                (d.new_id(), product_id, office["id"], 0, 1, 0, 1, max_order + 10, admin["id"], created, created),
            )
        audit(db, admin["id"], "product_created", code)
        row = db.execute("SELECT * FROM products WHERE id=?", (product_id,)).fetchone()
    return product_dto(row)


@app.post("/api/sales")
def add_sale(body: SaleIn, user: dict = Depends(require_sales_access)):
    client_last4 = validate_client_last4(body.clientLast4)
    sale_id = d.new_id()
    created = now_utc()
    with d.connect() as db:
        assignment = active_assignment(db, user["id"])
        if not assignment:
            raise HTTPException(403, "Сотрудник не привязан к офису")
        product = resolve_sale_product(
            db,
            product_id=body.productId,
            category=body.category,
            org_unit_id=assignment["org_unit_id"],
            amount=body.amount,
        )
        db.execute(
            "INSERT INTO sales(id, user_id, category, amount, quantity, comment, created_at, source, "
            "org_unit_id, manager_id, client_last4, product_id, product_code_snapshot, product_title_snapshot, "
            "product_group_snapshot, points_snapshot, requires_amount_snapshot, counts_toward_plan_snapshot, updated_at) "
            "VALUES (?,?,?,?,?,?,?, 'app', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            (
                sale_id, user["id"], product["code"], body.amount, body.quantity, body.comment, created,
                assignment["org_unit_id"], assignment["assigned_by_user_id"], client_last4,
                product["product_id"], product["code"], product["title"], product["group_name"],
                product["points"], product["requires_amount"], product["counts_toward_plan"], created,
            ),
        )
        audit(db, user["id"], "sale_added", f"{product['code']} x{body.quantity} client:{client_last4}")
    return {
        "id": sale_id,
        "category": product["code"],
        "productId": product["product_id"],
        "productTitle": product["title"],
        "productGroup": product["group_name"],
        "points": product["points"],
        "pointsTotal": product["points"] * body.quantity,
        "requiresAmount": bool(product["requires_amount"]),
        "countsTowardPlan": bool(product["counts_toward_plan"]),
        "clientLast4": client_last4,
        "amount": body.amount,
        "quantity": body.quantity,
        "comment": body.comment,
        "createdAt": created,
        "employeeId": user["employee_id"],
        "employeeName": user["full_name"],
    }


@app.post("/api/sales/batch")
def add_sales_batch(body: SaleBatchIn, user: dict = Depends(require_sales_access)):
    client_last4 = validate_client_last4(body.clientLast4)
    sale_group_id = d.new_id()
    created = now_utc()
    result = []
    with d.connect() as db:
        assignment = active_assignment(db, user["id"])
        if not assignment:
            raise HTTPException(403, "Сотрудник не привязан к офису")
        resolved_products = []
        for item in body.items:
            resolved_products.append(resolve_sale_product(
                db,
                product_id=item.productId,
                category=item.category,
                org_unit_id=assignment["org_unit_id"],
                amount=item.amount,
            ))
        for item, product in zip(body.items, resolved_products):
            sale_id = d.new_id()
            db.execute(
                "INSERT INTO sales(id, user_id, category, amount, quantity, comment, created_at, source, "
                "org_unit_id, manager_id, client_last4, sale_group_id, product_id, product_code_snapshot, "
                "product_title_snapshot, product_group_snapshot, points_snapshot, requires_amount_snapshot, "
                "counts_toward_plan_snapshot, updated_at) "
                "VALUES (?,?,?,?,?,?,?, 'app', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                (
                    sale_id, user["id"], product["code"], item.amount, item.quantity, item.comment, created,
                    assignment["org_unit_id"], assignment["assigned_by_user_id"], client_last4, sale_group_id,
                    product["product_id"], product["code"], product["title"], product["group_name"],
                    product["points"], product["requires_amount"], product["counts_toward_plan"], created,
                ),
            )
            result.append({
                "id": sale_id,
                "category": product["code"],
                "productId": product["product_id"],
                "productTitle": product["title"],
                "productGroup": product["group_name"],
                "points": product["points"],
                "pointsTotal": product["points"] * item.quantity,
                "requiresAmount": bool(product["requires_amount"]),
                "countsTowardPlan": bool(product["counts_toward_plan"]),
                "clientLast4": client_last4,
                "saleGroupId": sale_group_id,
                "amount": item.amount,
                "quantity": item.quantity,
                "comment": item.comment,
                "createdAt": created,
                "employeeId": user["employee_id"],
                "employeeName": user["full_name"],
            })
        audit(db, user["id"], "sales_batch_added", f"{len(body.items)} products client:{client_last4}")
    return result


def _sales_rows_to_dto(rows) -> list[dict]:
    result = []
    for r in rows:
        category = r["product_code_snapshot"] or r["category"]
        title = r["product_title_snapshot"] or CATEGORY_TITLES.get(category, category)
        points = r["points_snapshot"] if r["points_snapshot"] is not None else 1
        quantity = r["quantity"]
        result.append({
            "id": r["id"],
            "category": category,
            "productId": r["product_id"],
            "productTitle": title,
            "productGroup": r["product_group_snapshot"] or "",
            "points": points,
            "pointsTotal": points * quantity,
            "requiresAmount": bool(r["requires_amount_snapshot"]),
            "countsTowardPlan": bool(r["counts_toward_plan_snapshot"]),
            "clientLast4": r["client_last4"],
            "saleGroupId": r["sale_group_id"],
            "amount": r["amount"],
            "quantity": quantity,
            "comment": r["comment"],
            "createdAt": r["created_at"],
            "employeeId": r["employee_id"],
            "employeeName": r["full_name"],
        })
    return result


@app.get("/api/sales/my")
def my_sales(user: dict = Depends(require_work_access),
             from_: str | None = Query(None, alias="from"), to: str | None = None):
    start, end = None, None
    if from_:
        start, _ = day_bounds_utc(datetime.fromisoformat(from_).date())
    if to:
        _, end = day_bounds_utc(datetime.fromisoformat(to).date())
    query = "SELECT s.*, u.employee_id, u.full_name FROM sales s JOIN users u ON u.id=s.user_id WHERE s.user_id=?"
    params: list = [user["id"]]
    if start:
        query += " AND s.created_at>=?"
        params.append(start)
    if end:
        query += " AND s.created_at<?"
        params.append(end)
    query += " ORDER BY s.created_at DESC"
    with d.connect() as db:
        rows = db.execute(query, params).fetchall()
    return _sales_rows_to_dto(rows)


@app.delete("/api/sales/{sale_id}")
def delete_sale(sale_id: str, user: dict = Depends(require_work_access)):
    start, end = day_bounds_utc(datetime.now(TZ).date())
    with d.connect() as db:
        row = db.execute("SELECT * FROM sales WHERE id=?", (sale_id,)).fetchone()
        if not row:
            raise HTTPException(404, "Продажа не найдена")
        if row["user_id"] != user["id"]:
            raise HTTPException(403, "Можно удалять только свои продажи")
        if not (start <= row["created_at"] < end):
            raise HTTPException(403, "Удалять можно только продажи за сегодня")
        db.execute("DELETE FROM sales WHERE id=?", (sale_id,))
        audit(db, user["id"], "sale_deleted", sale_id)
    return {"ok": True}


def _agg(db, user_id: str | None, start: str, end: str, org_ids: list[str] | None = None) -> tuple[int, float]:
    query = "SELECT COALESCE(SUM(quantity),0) c, COALESCE(SUM(amount*quantity),0) a FROM sales WHERE created_at>=? AND created_at<?"
    params: list = [start, end]
    if user_id:
        query += " AND user_id=?"
        params.append(user_id)
    extra, extra_params = org_filter_sql("org_unit_id", org_ids)
    query += extra
    params.extend(extra_params)
    row = db.execute(query, params).fetchone()
    return row["c"], row["a"]


def _points(db, user_id: str | None, start: str, end: str, org_ids: list[str] | None = None) -> float:
    query = "SELECT COALESCE(SUM(points_snapshot*quantity),0) p FROM sales WHERE created_at>=? AND created_at<?"
    params: list = [start, end]
    if user_id:
        query += " AND user_id=?"
        params.append(user_id)
    extra, extra_params = org_filter_sql("org_unit_id", org_ids)
    query += extra
    params.extend(extra_params)
    row = db.execute(query, params).fetchone()
    return row["p"]


@app.get("/api/reports/my")
def my_report(user: dict = Depends(require_work_access)):
    t_start, t_end = period_bounds("today")
    m_start, m_end = period_bounds("month")
    with d.connect() as db:
        today_c, today_a = _agg(db, user["id"], t_start, t_end)
        month_c, month_a = _agg(db, user["id"], m_start, m_end)
        today_points = _points(db, user["id"], t_start, t_end)
        month_points = _points(db, user["id"], m_start, m_end)
        total = db.execute("SELECT COALESCE(SUM(quantity),0) c FROM sales WHERE user_id=?", (user["id"],)).fetchone()["c"]
        by_cat = db.execute(
            "SELECT COALESCE(product_code_snapshot, category) category, "
            "COALESCE(product_title_snapshot, category) product_title, SUM(quantity) c, "
            "COALESCE(SUM(amount*quantity),0) a, COALESCE(SUM(points_snapshot*quantity),0) p FROM sales "
            "WHERE user_id=? AND created_at>=? AND created_at<? "
            "GROUP BY COALESCE(product_code_snapshot, category), COALESCE(product_title_snapshot, category)",
            (user["id"], m_start, m_end),
        ).fetchall()
        days = []
        for i in range(13, -1, -1):
            day = datetime.now(TZ).date() - timedelta(days=i)
            s, e = day_bounds_utc(day)
            count, _ = _agg(db, user["id"], s, e)
            days.append({"date": day.isoformat(), "count": count})
    return {
        "todayCount": today_c,
        "todayAmount": today_a,
        "todayPoints": today_points,
        "monthCount": month_c,
        "monthAmount": month_a,
        "monthPoints": month_points,
        "totalCount": total,
        "byCategory": [
            {
                "category": r["category"],
                "productTitle": r["product_title"],
                "count": r["c"],
                "totalAmount": r["a"],
                "totalPoints": r["p"],
            } for r in by_cat
        ],
        "last14Days": days,
        "monthlyGoal": user["monthly_goal"],
    }


@app.get("/api/org/units")
def org_units(manager: dict = Depends(require_manager)):
    with d.connect() as db:
        ids = visible_org_ids(db, manager)
        query = "SELECT * FROM org_units WHERE active=1"
        params: list = []
        extra, extra_params = org_filter_sql("id", ids)
        query += extra + " ORDER BY type, name"
        params.extend(extra_params)
        rows = db.execute(query, params).fetchall()
    return [{
        "id": r["id"],
        "name": r["name"],
        "type": r["type"],
        "parentId": r["parent_id"],
        "code": r["code"],
    } for r in rows]


@app.get("/api/admin/report")
def admin_report(period: str = "month", manager: dict = Depends(require_manager)):
    if period not in ("today", "week", "month"):
        raise HTTPException(400, "period: today|week|month")
    p_start, p_end = period_bounds(period)
    t_start, t_end = period_bounds("today")
    m_start, m_end = period_bounds("month")
    with d.connect() as db:
        org_ids = visible_org_ids(db, manager)
        b_today, _ = _agg(db, None, t_start, t_end, org_ids)
        b_month_c, b_month_a = _agg(db, None, m_start, m_end, org_ids)
        b_today_points = _points(db, None, t_start, t_end, org_ids)
        b_month_points = _points(db, None, m_start, m_end, org_ids)
        employee_filter, employee_params = org_filter_sql("primary_org_unit_id", org_ids)
        employees = []
        users = db.execute(
            "SELECT * FROM users WHERE active=1 AND registration_status=? " + employee_filter + " ORDER BY full_name",
            [ACTIVE] + employee_params,
        ).fetchall()
        for user_row in users:
            today_count, _ = _agg(db, user_row["id"], t_start, t_end)
            period_count, period_amount = _agg(db, user_row["id"], p_start, p_end)
            period_points = _points(db, user_row["id"], p_start, p_end)
            month_count, _ = _agg(db, user_row["id"], m_start, m_end)
            goal = user_row["monthly_goal"]
            employees.append({
                "user": user_dto(user_row),
                "todayCount": today_count,
                "monthCount": period_count,
                "monthAmount": period_amount,
                "monthPoints": period_points,
                "goalProgress": (month_count / goal) if goal else None,
            })
        sale_filter, sale_params = org_filter_sql("org_unit_id", org_ids)
        by_cat = db.execute(
            "SELECT COALESCE(product_code_snapshot, category) category, "
            "COALESCE(product_title_snapshot, category) product_title, SUM(quantity) c, "
            "COALESCE(SUM(amount*quantity),0) a, COALESCE(SUM(points_snapshot*quantity),0) p FROM sales "
            "WHERE created_at>=? AND created_at<? " + sale_filter +
            " GROUP BY COALESCE(product_code_snapshot, category), COALESCE(product_title_snapshot, category)",
            [p_start, p_end] + sale_params,
        ).fetchall()
    return {
        "branchTodayCount": b_today,
        "branchTodayPoints": b_today_points,
        "branchMonthCount": b_month_c,
        "branchMonthAmount": b_month_a,
        "branchMonthPoints": b_month_points,
        "employees": employees,
        "byCategory": [
            {
                "category": r["category"],
                "productTitle": r["product_title"],
                "count": r["c"],
                "totalAmount": r["a"],
                "totalPoints": r["p"],
            } for r in by_cat
        ],
    }


@app.get("/api/admin/employee/{employee_id}/sales")
def employee_sales(employee_id: str, manager: dict = Depends(require_manager),
                   from_: str | None = Query(None, alias="from"), to: str | None = None):
    m_start, m_end = period_bounds("month")
    start = day_bounds_utc(datetime.fromisoformat(from_).date())[0] if from_ else m_start
    end = day_bounds_utc(datetime.fromisoformat(to).date())[1] if to else m_end
    with d.connect() as db:
        employee = resolve_user(db, employee_id)
        if not employee:
            raise HTTPException(404, "Сотрудник не найден")
        ids = visible_org_ids(db, manager)
        if ids is not None and employee["primary_org_unit_id"] not in ids:
            raise HTTPException(403, "Сотрудник вне вашей структуры")
        rows = db.execute(
            "SELECT s.*, u.employee_id, u.full_name FROM sales s JOIN users u ON u.id=s.user_id "
            "WHERE s.user_id=? AND s.created_at>=? AND s.created_at<? ORDER BY s.created_at DESC",
            (employee["id"], start, end),
        ).fetchall()
    return _sales_rows_to_dto(rows)


@app.post("/api/admin/employee/{employee_id}/goal")
def set_goal(employee_id: str, goal: int, manager: dict = Depends(require_manager)):
    with d.connect() as db:
        employee = resolve_user(db, employee_id)
        if not employee:
            raise HTTPException(404, "Сотрудник не найден")
        ids = visible_org_ids(db, manager)
        if ids is not None and employee["primary_org_unit_id"] not in ids:
            raise HTTPException(403, "Сотрудник вне вашей структуры")
        db.execute("UPDATE users SET monthly_goal=?, updated_at=? WHERE id=?", (goal, now_utc(), employee["id"]))
        audit(db, manager["id"], "goal_set", f"{employee['employee_id']}: {goal}")
    return {"ok": True}


@app.get("/api/admin/export")
def export_csv(period: str = "month", manager: dict = Depends(require_manager)):
    p_start, p_end = period_bounds(period)
    with d.connect() as db:
        org_ids = visible_org_ids(db, manager)
        extra, params = org_filter_sql("s.org_unit_id", org_ids)
        rows = db.execute(
            "SELECT s.created_at, u.employee_id, u.full_name, s.client_last4, s.category, s.quantity, s.amount, s.comment, s.source "
            "FROM sales s JOIN users u ON u.id=s.user_id "
            "WHERE s.created_at>=? AND s.created_at<? " + extra + " ORDER BY s.created_at",
            [p_start, p_end] + params,
        ).fetchall()
    out = io.StringIO()
    writer = csv.writer(out, delimiter=";")
    writer.writerow(["Дата", "Табельный", "Сотрудник", "Клиент 4", "Продукт", "Кол-во", "Сумма", "Комментарий", "Источник"])
    for row in rows:
        local = datetime.fromisoformat(row["created_at"]).astimezone(TZ).strftime("%d.%m.%Y %H:%M")
        writer.writerow([
            local,
            row["employee_id"],
            row["full_name"],
            row["client_last4"] or "",
            CATEGORY_TITLES.get(row["category"], row["category"]),
            row["quantity"],
            row["amount"] or "",
            row["comment"] or "",
            row["source"],
        ])
    return out.getvalue()


@app.get("/api/management/pending-users")
def pending_users(manager: dict = Depends(require_manager)):
    with d.connect() as db:
        rows = db.execute(
            "SELECT * FROM users WHERE active=1 AND registration_status=? ORDER BY created_at DESC",
            (PENDING_ASSIGNMENT,),
        ).fetchall()
    return [user_dto(row) for row in rows]


@app.get("/api/management/users/{employee_id}")
def management_user(employee_id: str, manager: dict = Depends(require_manager)):
    with d.connect() as db:
        row = db.execute("SELECT * FROM users WHERE employee_id=?", (validate_employee_id(employee_id),)).fetchone()
        if not row:
            raise HTTPException(404, "Сотрудник не найден")
    return user_dto(row)


@app.post("/api/management/users/{employee_id}/assign")
def assign_user(employee_id: str, body: AssignUserIn, manager: dict = Depends(require_manager)):
    target_role = body.role.strip().upper()
    if target_role not in WORK_ROLES:
        raise HTTPException(400, "Неизвестная роль")
    if target_role != "EMPLOYEE" and manager["role"] != "ADMIN":
        raise HTTPException(403, "Назначать руководящие роли может только администратор")
    with d.connect() as db:
        employee = db.execute("SELECT * FROM users WHERE employee_id=?", (validate_employee_id(employee_id),)).fetchone()
        if not employee:
            raise HTTPException(404, "Сотрудник не найден")
        target_org_id = body.orgUnitId or manager["primary_org_unit_id"]
        if not target_org_id:
            raise HTTPException(400, "У руководителя не задан офис для привязки")
        target_org = db.execute("SELECT * FROM org_units WHERE id=? AND active=1", (target_org_id,)).fetchone()
        if not target_org:
            raise HTTPException(404, "Подразделение не найдено")
        visible_ids = visible_org_ids(db, manager)
        if visible_ids is not None and target_org_id not in visible_ids:
            raise HTTPException(403, "Нельзя назначить сотрудника вне вашей структуры")

        changed_at = now_utc()
        current = active_assignment(db, employee["id"])
        if current:
            db.execute("UPDATE user_assignments SET ended_at=? WHERE id=?", (changed_at, current["id"]))
        db.execute(
            "INSERT INTO user_assignments(id, user_id, org_unit_id, role_at_unit, assigned_by_user_id, started_at, comment) "
            "VALUES (?,?,?,?,?,?,?)",
            (d.new_id(), employee["id"], target_org_id, target_role, manager["id"], changed_at, body.comment),
        )
        db.execute(
            "UPDATE users SET role=?, registration_status=?, primary_org_unit_id=?, branch=?, updated_at=? WHERE id=?",
            (target_role, ACTIVE, target_org_id, org_display_name(db, target_org_id), changed_at, employee["id"]),
        )
        audit(db, manager["id"], "user_assigned", f"{employee['employee_id']} -> {target_org['name']} as {target_role}")
        row = db.execute("SELECT * FROM users WHERE id=?", (employee["id"],)).fetchone()
    return user_dto(row)
