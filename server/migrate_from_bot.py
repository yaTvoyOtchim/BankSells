"""
Перенос данных из ТЕКУЩЕЙ базы телеграм-бота в новую общую базу.

ВАЖНО: я не знаю точную схему вашего бота, поэтому ниже — шаблон с
ПРЕДПОЛАГАЕМЫМИ названиями таблиц/колонок. Откройте свою базу:

    sqlite3 old_bot.db ".schema"

и поправьте константы OLD_USERS_SQL / OLD_SALES_SQL под свои названия.
Скрипт идемпотентный по telegram_id — повторный запуск не создаст дублей пользователей.

Запуск:
    export SALES_DB=sales.db          # новая общая база
    python migrate_from_bot.py old_bot.db
"""
import sqlite3
import sys
from datetime import datetime, timezone

import db as d

# ====== ПОДГОНИТЕ ПОД СХЕМУ ВАШЕГО БОТА ======
# Должен вернуть: telegram_id, full_name (как сможете), is_admin (0/1)
OLD_USERS_SQL = """
    SELECT user_id AS telegram_id,
           COALESCE(name, username, CAST(user_id AS TEXT)) AS full_name,
           COALESCE(is_admin, 0) AS is_admin
    FROM users
"""

# Должен вернуть: telegram_id, product (текст), amount (или NULL), created_at
OLD_SALES_SQL = """
    SELECT user_id AS telegram_id,
           product,
           amount,
           date AS created_at
    FROM sales
"""

# Сопоставление названий продуктов бота с категориями приложения.
# Всё, что не нашлось, попадёт в OTHER (с исходным названием в комментарии).
PRODUCT_MAP = {
    "дебетовая карта": "DEBIT_CARD", "дк": "DEBIT_CARD",
    "кредитная карта": "CREDIT_CARD", "кк": "CREDIT_CARD",
    "кредит": "CONSUMER_LOAN", "потреб": "CONSUMER_LOAN",
    "ипотека": "MORTGAGE",
    "вклад": "DEPOSIT", "депозит": "DEPOSIT",
    "страховка": "INSURANCE", "страхование": "INSURANCE",
    "инвестиции": "INVESTMENT", "иис": "INVESTMENT",
    "мп": "MOBILE_APP", "мобильное приложение": "MOBILE_APP", "онлайн-банк": "MOBILE_APP",
}
# =============================================


def map_category(product: str) -> tuple[str, str | None]:
    key = (product or "").strip().lower()
    for needle, cat in PRODUCT_MAP.items():
        if needle in key:
            return cat, None
    return "OTHER", product


def parse_dt(value) -> str:
    """Приводим дату бота к UTC ISO. Поддерживает unix-время и популярные строковые форматы."""
    if value is None:
        return datetime.now(timezone.utc).isoformat()
    if isinstance(value, (int, float)):
        return datetime.fromtimestamp(value, tz=timezone.utc).isoformat()
    s = str(value).strip()
    for fmt in ("%Y-%m-%d %H:%M:%S", "%Y-%m-%dT%H:%M:%S", "%d.%m.%Y %H:%M", "%Y-%m-%d", "%d.%m.%Y"):
        try:
            return datetime.strptime(s, fmt).replace(tzinfo=timezone.utc).isoformat()
        except ValueError:
            continue
    try:
        return datetime.fromisoformat(s).astimezone(timezone.utc).isoformat()
    except ValueError:
        return datetime.now(timezone.utc).isoformat()


def main(old_path: str):
    d.init_db()
    old = sqlite3.connect(old_path)
    old.row_factory = sqlite3.Row

    with d.connect() as db:
        # --- пользователи ---
        tg_to_id: dict[int, str] = {}
        for r in old.execute(OLD_USERS_SQL):
            existing = db.execute("SELECT id FROM users WHERE telegram_id=?", (r["telegram_id"],)).fetchone()
            if existing:
                tg_to_id[r["telegram_id"]] = existing["id"]
                continue
            uid = d.new_id()
            db.execute(
                "INSERT INTO users(id, employee_id, telegram_id, full_name, role, must_change_password) "
                "VALUES (?,?,?,?,?,1)",
                (uid, f"tg{r['telegram_id']}", r["telegram_id"], r["full_name"],
                 "ADMIN" if r["is_admin"] else "EMPLOYEE"),
            )
            tg_to_id[r["telegram_id"]] = uid
        print(f"Пользователей: {len(tg_to_id)}")
        print("⚠ employee_id выставлен как tg<id> — замените на реальные табельные номера:")
        print("  UPDATE users SET employee_id='vtb70336145' WHERE telegram_id=123456789;")
        print("  Пароли выдайте через: python manage.py reset <employee_id>")

        # --- продажи ---
        count = skipped = 0
        for r in old.execute(OLD_SALES_SQL):
            uid = tg_to_id.get(r["telegram_id"])
            if not uid:
                skipped += 1
                continue
            cat, comment = map_category(r["product"])
            db.execute(
                "INSERT INTO sales(id, user_id, category, amount, quantity, comment, created_at, source) "
                "VALUES (?,?,?,?,1,?,?, 'bot')",
                (d.new_id(), uid, cat, r["amount"], comment, parse_dt(r["created_at"])),
            )
            count += 1
        print(f"Продаж перенесено: {count}, пропущено (нет пользователя): {skipped}")


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Использование: python migrate_from_bot.py old_bot.db"); sys.exit(1)
    main(sys.argv[1])
