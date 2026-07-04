import os
import sqlite3
import uuid
from contextlib import contextmanager
from datetime import datetime, timezone

DB_PATH = os.environ.get("SALES_DB", "sales.db")

SCHEMA = """
CREATE TABLE IF NOT EXISTS users (
    id                   TEXT PRIMARY KEY,
    employee_id          TEXT UNIQUE NOT NULL,
    telegram_id          INTEGER UNIQUE,
    full_name            TEXT NOT NULL,
    branch               TEXT NOT NULL DEFAULT '',
    role                 TEXT NOT NULL DEFAULT 'EMPLOYEE',
    registration_status  TEXT NOT NULL DEFAULT 'ACTIVE',
    primary_org_unit_id  TEXT,
    password_hash        TEXT,
    must_change_password INTEGER NOT NULL DEFAULT 1,
    failed_attempts      INTEGER NOT NULL DEFAULT 0,
    locked               INTEGER NOT NULL DEFAULT 0,
    active               INTEGER NOT NULL DEFAULT 1,
    monthly_goal         INTEGER,
    created_at           TEXT,
    updated_at           TEXT
);

CREATE TABLE IF NOT EXISTS org_units (
    id         TEXT PRIMARY KEY,
    name       TEXT NOT NULL,
    type       TEXT NOT NULL,
    parent_id  TEXT REFERENCES org_units(id),
    code       TEXT UNIQUE,
    active     INTEGER NOT NULL DEFAULT 1,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_org_units_parent ON org_units(parent_id);

CREATE TABLE IF NOT EXISTS user_assignments (
    id                  TEXT PRIMARY KEY,
    user_id             TEXT NOT NULL REFERENCES users(id),
    org_unit_id         TEXT NOT NULL REFERENCES org_units(id),
    role_at_unit        TEXT NOT NULL DEFAULT 'EMPLOYEE',
    assigned_by_user_id TEXT REFERENCES users(id),
    started_at          TEXT NOT NULL,
    ended_at            TEXT,
    comment             TEXT
);
CREATE INDEX IF NOT EXISTS idx_user_assignments_user_active ON user_assignments(user_id, ended_at);
CREATE INDEX IF NOT EXISTS idx_user_assignments_org_active ON user_assignments(org_unit_id, ended_at);

CREATE TABLE IF NOT EXISTS sales (
    id           TEXT PRIMARY KEY,
    user_id      TEXT NOT NULL REFERENCES users(id),
    category     TEXT NOT NULL,
    amount       REAL,
    quantity     INTEGER NOT NULL DEFAULT 1,
    comment      TEXT,
    created_at   TEXT NOT NULL,
    source       TEXT NOT NULL DEFAULT 'app',
    org_unit_id  TEXT REFERENCES org_units(id),
    manager_id   TEXT REFERENCES users(id),
    client_last4 TEXT,
    updated_at   TEXT
);
CREATE INDEX IF NOT EXISTS idx_sales_user_date ON sales(user_id, created_at);
CREATE INDEX IF NOT EXISTS idx_sales_date ON sales(created_at);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    token_hash TEXT PRIMARY KEY,
    user_id    TEXT NOT NULL REFERENCES users(id),
    device_id  TEXT NOT NULL,
    expires_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS audit_log (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id    TEXT,
    action     TEXT NOT NULL,
    details    TEXT,
    created_at TEXT NOT NULL
);
"""


def init_db() -> None:
    with connect() as db:
        db.executescript(SCHEMA)
        migrate(db)
        seed_demo_hierarchy(db)


def _now() -> str:
    return datetime.now(timezone.utc).isoformat()


def _columns(db, table: str) -> set[str]:
    return {row["name"] for row in db.execute(f"PRAGMA table_info({table})")}


def _add_column(db, table: str, column: str, ddl: str) -> None:
    if column not in _columns(db, table):
        db.execute(f"ALTER TABLE {table} ADD COLUMN {ddl}")


def migrate(db) -> None:
    now = _now()
    _add_column(db, "users", "registration_status", "registration_status TEXT NOT NULL DEFAULT 'ACTIVE'")
    _add_column(db, "users", "primary_org_unit_id", "primary_org_unit_id TEXT")
    _add_column(db, "users", "created_at", "created_at TEXT")
    _add_column(db, "users", "updated_at", "updated_at TEXT")
    _add_column(db, "sales", "org_unit_id", "org_unit_id TEXT")
    _add_column(db, "sales", "manager_id", "manager_id TEXT")
    _add_column(db, "sales", "client_last4", "client_last4 TEXT")
    _add_column(db, "sales", "updated_at", "updated_at TEXT")
    db.execute("CREATE INDEX IF NOT EXISTS idx_sales_org_date ON sales(org_unit_id, created_at)")
    db.execute("UPDATE users SET registration_status='ACTIVE' WHERE registration_status IS NULL OR registration_status=''")
    db.execute("UPDATE users SET created_at=? WHERE created_at IS NULL", (now,))
    db.execute("UPDATE users SET updated_at=? WHERE updated_at IS NULL", (now,))
    db.execute("UPDATE sales SET updated_at=created_at WHERE updated_at IS NULL")
    legacy_categories = {
        "DEBIT_CARD": "DEBIT_CARD_STICKER_APPLICATION",
        "CREDIT_CARD": "CREDIT_CARD_SALE",
        "CONSUMER_LOAN": "CASH_LOAN_SALE",
        "MORTGAGE": "CASH_LOAN_SALE",
        "DEPOSIT": "SAVINGS_ACCOUNT",
        "INSURANCE": "CREDIT_CARD_INSURANCE",
        "INVESTMENT": "OPIF",
        "MOBILE_APP": "SUBSCRIPTION",
        "OTHER": "SOM",
    }
    for old, new in legacy_categories.items():
        db.execute("UPDATE sales SET category=? WHERE category=?", (new, old))


def _get_or_create_org_unit(db, name: str, unit_type: str, code: str, parent_id: str | None = None) -> str:
    row = db.execute("SELECT id FROM org_units WHERE code=?", (code,)).fetchone()
    if row:
        return row["id"]
    unit_id = new_id()
    now = _now()
    db.execute(
        "INSERT INTO org_units(id, name, type, parent_id, code, active, created_at, updated_at) "
        "VALUES (?,?,?,?,?,1,?,?)",
        (unit_id, name, unit_type, parent_id, code, now, now),
    )
    return unit_id


def seed_demo_hierarchy(db) -> None:
    directorate_id = _get_or_create_org_unit(db, "ВТБ", "DIRECTORATE", "vtb")
    region_id = _get_or_create_org_unit(db, "Крымский регион", "REGION", "crimea-region", directorate_id)
    office_id = _get_or_create_org_unit(db, "Отделение №7", "OFFICE", "office-7", region_id)
    team_id = _get_or_create_org_unit(db, "Основная команда", "TEAM", "office-7-main", office_id)
    now = _now()

    for user in db.execute("SELECT * FROM users WHERE active=1").fetchall():
        active_assignment = db.execute(
            "SELECT id FROM user_assignments WHERE user_id=? AND ended_at IS NULL",
            (user["id"],),
        ).fetchone()
        target_id = office_id if user["role"] in ("ADMIN", "OFFICE_MANAGER", "REGIONAL_MANAGER", "DIRECTOR") else team_id
        if not active_assignment:
            db.execute(
                "INSERT INTO user_assignments(id, user_id, org_unit_id, role_at_unit, assigned_by_user_id, started_at, comment) "
                "VALUES (?,?,?,?,?,?,?)",
                (new_id(), user["id"], target_id, user["role"], None, now, "Initial demo assignment"),
            )
        db.execute(
            "UPDATE users SET primary_org_unit_id=?, branch=?, registration_status='ACTIVE', updated_at=? "
            "WHERE id=? AND (primary_org_unit_id IS NULL OR primary_org_unit_id='')",
            (target_id, "Отделение №7", now, user["id"]),
        )

    db.execute(
        "UPDATE sales SET org_unit_id=(SELECT primary_org_unit_id FROM users WHERE users.id=sales.user_id) "
        "WHERE org_unit_id IS NULL"
    )


@contextmanager
def connect():
    db = sqlite3.connect(DB_PATH, timeout=10)
    db.row_factory = sqlite3.Row
    db.execute("PRAGMA journal_mode=WAL")
    db.execute("PRAGMA busy_timeout=5000")
    db.execute("PRAGMA foreign_keys=ON")
    try:
        yield db
        db.commit()
    except Exception:
        db.rollback()
        raise
    finally:
        db.close()


def new_id() -> str:
    return str(uuid.uuid4())
