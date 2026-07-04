"""
Управление пользователями из консоли (запускается на сервере).

Примеры:
    python manage.py create vtb70336145 "Иванов Иван" --branch "Отделение №7" --telegram 123456789
    python manage.py create vtb70336144 "Петрова Анна" --role ADMIN
    python manage.py reset vtb70336145           # сбросить пароль (выдаст новый временный)
    python manage.py unlock vtb70336145          # снять блокировку после 5 неверных попыток
    python manage.py deactivate vtb70336145      # уволенный сотрудник
    python manage.py list
"""
import argparse
import secrets
import string

import auth as a
import db as d


def temp_password() -> str:
    alphabet = string.ascii_letters + string.digits
    return "".join(secrets.choice(alphabet) for _ in range(10))


def cmd_create(args):
    pwd = temp_password()
    with d.connect() as db:
        db.execute(
            "INSERT INTO users(id, employee_id, telegram_id, full_name, branch, role, "
            "password_hash, must_change_password) VALUES (?,?,?,?,?,?,?,1)",
            (d.new_id(), args.employee_id, args.telegram, args.full_name,
             args.branch, args.role, a.hash_password(pwd)),
        )
    print(f"Создан {args.full_name} (таб. {args.employee_id}, роль {args.role})")
    print(f"Временный пароль: {pwd}  — передайте сотруднику лично")


def cmd_reset(args):
    pwd = temp_password()
    with d.connect() as db:
        cur = db.execute(
            "UPDATE users SET password_hash=?, must_change_password=1, failed_attempts=0, locked=0 "
            "WHERE employee_id=?", (a.hash_password(pwd), args.employee_id))
        if cur.rowcount == 0:
            print("Сотрудник не найден"); return
        row = db.execute("SELECT id FROM users WHERE employee_id=?", (args.employee_id,)).fetchone()
        a.revoke_all_tokens(db, row["id"])
    print(f"Новый временный пароль: {pwd}")


def cmd_unlock(args):
    with d.connect() as db:
        db.execute("UPDATE users SET locked=0, failed_attempts=0 WHERE employee_id=?", (args.employee_id,))
    print("Разблокировано")


def cmd_deactivate(args):
    with d.connect() as db:
        row = db.execute("SELECT id FROM users WHERE employee_id=?", (args.employee_id,)).fetchone()
        if not row:
            print("Сотрудник не найден"); return
        db.execute("UPDATE users SET active=0 WHERE id=?", (row["id"],))
        a.revoke_all_tokens(db, row["id"])
    print("Деактивировано, все сессии отозваны")


def cmd_list(args):
    with d.connect() as db:
        for r in db.execute("SELECT employee_id, full_name, role, active, locked, telegram_id FROM users ORDER BY full_name"):
            flags = ("" if r["active"] else " [неактивен]") + (" [заблокирован]" if r["locked"] else "")
            tg = f" tg:{r['telegram_id']}" if r["telegram_id"] else ""
            print(f"{r['employee_id']:>6}  {r['full_name']:<30} {r['role']}{tg}{flags}")


if __name__ == "__main__":
    d.init_db()
    p = argparse.ArgumentParser()
    sub = p.add_subparsers(required=True)

    c = sub.add_parser("create"); c.add_argument("employee_id"); c.add_argument("full_name")
    c.add_argument("--branch", default=""); c.add_argument("--role", default="EMPLOYEE", choices=["EMPLOYEE", "ADMIN"])
    c.add_argument("--telegram", type=int, default=None); c.set_defaults(fn=cmd_create)

    r = sub.add_parser("reset"); r.add_argument("employee_id"); r.set_defaults(fn=cmd_reset)
    u = sub.add_parser("unlock"); u.add_argument("employee_id"); u.set_defaults(fn=cmd_unlock)
    x = sub.add_parser("deactivate"); x.add_argument("employee_id"); x.set_defaults(fn=cmd_deactivate)
    l = sub.add_parser("list"); l.set_defaults(fn=cmd_list)

    args = p.parse_args()
    args.fn(args)
