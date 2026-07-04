"""
Подключение СУЩЕСТВУЮЩЕГО телеграм-бота к общей базе.

Идея: бот перестаёт писать в свои таблицы и вызывает функции отсюда.
Тогда продажа, внесённая через бота, сразу видна в приложении и наоборот.

В коде бота:

    import bot_bridge

    # пользователь нажал кнопку «Кредитная карта» и ввёл сумму:
    bot_bridge.add_sale(message.from_user.id, "CREDIT_CARD", amount=150000)

    # команда /report:
    text = bot_bridge.today_report(message.from_user.id)
    await message.answer(text)

Файл базы тот же: переменная окружения SALES_DB у бота и у API должна
указывать на один и тот же путь.
"""
from datetime import datetime, timedelta, timezone
from zoneinfo import ZoneInfo
import os

import db as d

TZ = ZoneInfo(os.environ.get("SALES_TZ", "Europe/Istanbul"))

TITLES = {
    "DEBIT_CARD": "Дебетовая карта", "CREDIT_CARD": "Кредитная карта",
    "CONSUMER_LOAN": "Потреб. кредит", "MORTGAGE": "Ипотека",
    "DEPOSIT": "Вклад", "INSURANCE": "Страховка",
    "INVESTMENT": "Инвестиции", "MOBILE_APP": "Моб. приложение", "OTHER": "Другое",
}


def _user_id(db, telegram_id: int) -> str | None:
    row = db.execute("SELECT id FROM users WHERE telegram_id=? AND active=1", (telegram_id,)).fetchone()
    return row["id"] if row else None


def is_registered(telegram_id: int) -> bool:
    with d.connect() as db:
        return _user_id(db, telegram_id) is not None


def is_admin(telegram_id: int) -> bool:
    with d.connect() as db:
        row = db.execute("SELECT role FROM users WHERE telegram_id=? AND active=1", (telegram_id,)).fetchone()
        return bool(row) and row["role"] == "ADMIN"


def add_sale(telegram_id: int, category: str, amount: float | None = None,
             quantity: int = 1, comment: str | None = None) -> bool:
    """Возвращает False, если телеграм-аккаунт не привязан к сотруднику."""
    with d.connect() as db:
        uid = _user_id(db, telegram_id)
        if not uid:
            return False
        db.execute(
            "INSERT INTO sales(id, user_id, category, amount, quantity, comment, created_at, source) "
            "VALUES (?,?,?,?,?,?,?, 'bot')",
            (d.new_id(), uid, category, amount, quantity, comment,
             datetime.now(timezone.utc).isoformat()),
        )
    return True


def _bounds(day) -> tuple[str, str]:
    start = datetime.combine(day, datetime.min.time(), TZ)
    return (start.astimezone(timezone.utc).isoformat(),
            (start + timedelta(days=1)).astimezone(timezone.utc).isoformat())


def today_report(telegram_id: int) -> str:
    """Текст отчёта за сегодня — в том же духе, что бот отдавал раньше."""
    with d.connect() as db:
        uid = _user_id(db, telegram_id)
        if not uid:
            return "Ваш Telegram не привязан к учётной записи. Обратитесь к администратору."
        s, e = _bounds(datetime.now(TZ).date())
        rows = db.execute(
            "SELECT category, SUM(quantity) c, COALESCE(SUM(amount*quantity),0) a FROM sales "
            "WHERE user_id=? AND created_at>=? AND created_at<? GROUP BY category",
            (uid, s, e)).fetchall()
    if not rows:
        return "Сегодня продаж пока нет."
    total = sum(r["c"] for r in rows)
    lines = [f"📊 Продажи за сегодня: {total}"]
    for r in sorted(rows, key=lambda r: -r["c"]):
        amt = f" на {r['a']:,.0f} ₽".replace(",", " ") if r["a"] else ""
        lines.append(f"• {TITLES.get(r['category'], r['category'])}: {r['c']}{amt}")
    return "\n".join(lines)


def admin_today_report() -> str:
    """Сводка по всем сотрудникам за сегодня (для админа в боте)."""
    s, e = _bounds(datetime.now(TZ).date())
    with d.connect() as db:
        rows = db.execute(
            "SELECT u.full_name, COALESCE(SUM(s.quantity),0) c FROM users u "
            "LEFT JOIN sales s ON s.user_id=u.id AND s.created_at>=? AND s.created_at<? "
            "WHERE u.active=1 GROUP BY u.id ORDER BY c DESC", (s, e)).fetchall()
    lines = ["👥 Продажи отделения за сегодня:"]
    for r in rows:
        lines.append(f"• {r['full_name']}: {r['c']}")
    return "\n".join(lines)
