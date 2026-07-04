# Продажи Банка — Android-приложение

Замена телеграм-бота для учёта продаж сотрудников банковского отделения.
Kotlin + Jetpack Compose + Material 3, архитектура MVVM.

## Возможности

**Сотрудник**
- Быстрое внесение продажи: выбор продукта чипом, сумма, количество, комментарий (2–3 касания)
- Дашборд: продажи за сегодня и за месяц, прогресс по плану, график за 14 дней
- Отчёты: разбивка по продуктам, список продаж за сегодня с удалением (только своих и только за сегодня)
- Кнопка «Поделиться» — текстовый отчёт в привычном формате бота, можно отправить в любой чат
- Офлайн-режим: при потере связи продажи сохраняются локально (Room) и отправляются автоматически

**Администратор** (всё то же + дополнительно)
- Сводка по отделению: сегодня / неделя / месяц
- Рейтинг сотрудников с продажами и прогрессом по плану
- Детализация продаж любого сотрудника
- Установка месячного плана каждому сотруднику
- Экспорт CSV за период

## Авторизация — как сделано и почему

Для банка **лучший вариант — учётные записи, которые создаёт администратор**, а не самостоятельная регистрация:

1. **Админ создаёт аккаунт** по табельному номеру и выдаёт временный пароль лично.
   Никакой регистрации «с улицы» — в системе только реальные сотрудники.
2. **Первый вход** → приложение принудительно требует сменить пароль (флаг `mustChangePassword` в ответе логина).
3. **JWT access (15 мин) + refresh (30 дней)** — access короткоживущий, refresh привязан
   к `deviceId` устройства. Украденный refresh с другого устройства не сработает.
4. **Биометрия при каждом открытии** — если сессия активна, приложение просит отпечаток/Face Unlock
   или PIN устройства. Отмена = сброс сессии и вход по паролю.
5. **Токены в EncryptedSharedPreferences** (ключ в Android Keystore), никогда в открытом виде.
6. **Блокировка после 5 неверных попыток** (на сервере, HTTP 423) — разблокирует админ.
7. Уволился сотрудник → админ деактивирует аккаунт, все его токены отзываются.

Альтернативы, если у банка есть инфраструктура:
- **Корпоративный SSO / Active Directory (LDAP, Keycloak, ADFS)** — идеально, если ИТ-отдел даст доступ:
  один пароль со всеми системами банка, централизованное увольнение. Тогда `auth/login` на бэкенде
  просто проверяет креды в AD.
- **SMS-OTP** — не рекомендую как единственный фактор (SIM-swap, стоимость SMS), но хорош как второй фактор для админа.

В проде обязательно добавьте **certificate pinning** (заготовка в `ServiceLocator.kt`) и поставьте свой `API_BASE_URL` в `app/build.gradle.kts`.

## Спецификация REST API (для бэкенда)

Бэкенд можно написать на чём угодно: Python/FastAPI, Kotlin/Spring Boot, Node/NestJS.
Все ответы — JSON. Авторизация — заголовок `Authorization: Bearer <accessToken>`.

### Auth
| Метод | Путь | Тело | Ответ |
|---|---|---|---|
| POST | `/api/auth/login` | `{employeeId, password, deviceId}` | `{accessToken, refreshToken, mustChangePassword, user}` · 401 неверно · 423 заблокирован |
| POST | `/api/auth/refresh` | `{refreshToken, deviceId}` | новая пара токенов |
| POST | `/api/auth/change-password` | `{oldPassword, newPassword}` | 200 |
| POST | `/api/auth/logout` | — | 200, refresh-токен отозван |

`user = {id, employeeId, fullName, branch, role: "EMPLOYEE"|"ADMIN"}`

### Продажи
| Метод | Путь | Описание |
|---|---|---|
| POST | `/api/sales` | `{category, amount?, quantity, comment?}` → созданная продажа с `id`, `createdAt` |
| GET | `/api/sales/my?from=&to=` | мои продажи за период (ISO-даты) |
| DELETE | `/api/sales/{id}` | удалить **свою** продажу **за сегодня**, иначе 403 |
| GET | `/api/reports/my` | `{todayCount, todayAmount, monthCount, monthAmount, totalCount, byCategory[], last14Days[], monthlyGoal?}` |

`category` — одно из: `DEBIT_CARD, CREDIT_CARD, CONSUMER_LOAN, MORTGAGE, DEPOSIT, INSURANCE, INVESTMENT, MOBILE_APP, OTHER`

### Админ (только role=ADMIN, иначе 403)
| Метод | Путь | Описание |
|---|---|---|
| GET | `/api/admin/report?period=today\|week\|month` | сводка отделения + `employees[]` с рейтингом |
| GET | `/api/admin/employee/{id}/sales?from=&to=` | продажи конкретного сотрудника |
| POST | `/api/admin/employee/{id}/goal?goal=N` | задать месячный план |
| GET | `/api/admin/export?period=` | CSV-текст всех продаж за период |

Рекомендации серверу:
- Пароли — bcrypt/argon2, никаких MD5/SHA без соли
- `createdAt` ставит **сервер** (UTC) — клиентскому времени не доверяем
- Refresh-токены хранить в БД с привязкой к deviceId; при logout/деактивации — удалять
- Аудит-лог: кто, когда, что внёс/удалил — в банке это пригодится

## Сборка

1. Android Studio (Koala+), JDK 17
2. Открыть папку проекта, дождаться Gradle sync
3. Указать адрес вашего сервера в `app/build.gradle.kts` → `API_BASE_URL`
4. Run ▶ (minSdk 26 = Android 8.0+)

Иконку `mipmap/ic_launcher` Android Studio сгенерирует через New → Image Asset
(или временно замените в манифесте на `@android:drawable/sym_def_app_icon`).

## Структура

```
app/src/main/java/com/bank/salestracker/
├── MainActivity.kt            — вход в приложение + биометрический замок
├── di/ServiceLocator.kt       — сборка зависимостей (Retrofit, Room, репозитории)
├── data/
│   ├── model/Models.kt        — все DTO и enum продуктов
│   ├── api/ApiService.kt      — контракт REST API
│   ├── api/AuthInterceptor.kt — Bearer-токен + автообновление по refresh
│   ├── local/TokenStore.kt    — шифрованное хранилище сессии
│   ├── local/AppDb.kt         — Room: офлайн-очередь продаж
│   └── repository/            — Auth и Sales репозитории
└── ui/
    ├── theme/Theme.kt
    ├── navigation/AppNavHost.kt — навигация + нижнее меню (вкладка «Команда» только админу)
    └── screens/                 — Login, ChangePassword, Dashboard, AddSale, Reports, Admin
```

## Идеи на следующие версии
- Push-напоминание в 17:30 «внесите продажи за день» (Firebase Cloud Messaging)
- Виджет на рабочий стол с числом продаж за сегодня
- Конкурсы между отделениями, если бот обслуживал несколько точек
- Прикрепление продажи к номеру заявки/договора для сверки с CRM
