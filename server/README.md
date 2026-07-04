# Сервер: общая база для бота и приложения

FastAPI + SQLite (режим WAL — бот и API спокойно работают с одним файлом одновременно).
Реализует весь API-контракт из корневого README.

## Как всё связано

```
                    ┌──────────────────┐
 Телеграм-бот ────► │                  │ ◄──── API-сервер (main.py) ◄──── Android-приложение
 (bot_bridge.py)    │    sales.db      │
                    │  users / sales   │
                    └──────────────────┘
```

Одна таблица `users` (с колонкой `telegram_id`), одна таблица `sales`
(с колонкой `source`: app/bot — видно, откуда внесена продажа).
Продажа из бота мгновенно видна в приложении и наоборот.

## Запуск (порядок действий)

```bash
cd server
python -m venv venv && source venv/bin/activate
pip install -r requirements.txt

export SALES_DB=/srv/bank/sales.db          # общий файл базы
export JWT_SECRET=$(openssl rand -hex 32)   # сохраните, при смене все выйдут из приложения
export SALES_TZ=Europe/Istanbul             # часовой пояс отделения

# 1. Перенос данных из старой базы бота (см. комментарии внутри скрипта —
#    подгоните SQL-запросы под схему вашего бота)
python migrate_from_bot.py /path/to/old_bot.db

# 2. Проставьте реальные табельные номера и выдайте пароли
python manage.py list
sqlite3 $SALES_DB "UPDATE users SET employee_id='vtb70336145', branch='Отделение №7' WHERE telegram_id=123456789;"
python manage.py reset vtb70336145 # выдаст временный пароль — передайте лично

# Либо создавайте пользователей с нуля:
python manage.py create vtb70336145 "Иванов Иван" --branch "Отделение №7" --telegram 123456789
python manage.py create vtb70336144 "Петрова Анна" --role ADMIN

# 3. Старт API
uvicorn main:app --host 0.0.0.0 --port 8000
```

Проверка: откройте `http://сервер:8000/docs` — там интерактивная документация всех эндпоинтов.

## Подключение существующего бота

1. Положите рядом с ботом файлы `db.py` и `bot_bridge.py` (или добавьте папку server в PYTHONPATH).
2. У процесса бота должна быть та же переменная `SALES_DB`.
3. Замените в боте запись продажи и формирование отчёта на вызовы:

```python
import bot_bridge

# вместо своего INSERT:
ok = bot_bridge.add_sale(message.from_user.id, "CREDIT_CARD", amount=150000)
if not ok:
    await message.answer("Ваш Telegram не привязан к учётной записи")

# вместо своего отчёта:
await message.answer(bot_bridge.today_report(message.from_user.id))

# для админа:
if bot_bridge.is_admin(message.from_user.id):
    await message.answer(bot_bridge.admin_today_report())
```

Так бот остаётся рабочим на переходный период, а данные уже общие.
Когда команда привыкнет к приложению — бота можно просто выключить.

## Прод-минимум

- HTTPS обязательно: поставьте перед uvicorn реверс-прокси (nginx/caddy) с TLS-сертификатом.
  В банковской сети без HTTPS приложение работать не должно (в манифесте cleartext запрещён).
- Бэкап: SQLite — это один файл; `sqlite3 sales.db ".backup backup-$(date +%F).db"` по крону.
- systemd-юнит для автозапуска uvicorn и бота.
- Если сотрудников станет сотни и появятся несколько отделений — миграция на PostgreSQL
  сводится к замене db.py, контракт API не изменится.
