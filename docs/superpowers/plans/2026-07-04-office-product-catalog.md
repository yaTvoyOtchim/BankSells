# Office Product Catalog Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the hardcoded product list with office-specific product settings, point snapshots in sales, manager product editing, and role-aware sale access.

**Architecture:** Keep the existing FastAPI + SQLite server shape and add product tables, idempotent migrations, and management endpoints. Keep Android Compose/Retrofit patterns, replacing the product enum usage in sale entry with server-driven products while preserving legacy category fields for reports and old rows. Implement in compatibility mode first: new dynamic products are authoritative, but old `category` values keep working during migration.

**Tech Stack:** Android Kotlin/Compose, Retrofit, Kotlin serialization, Room, FastAPI, SQLite, Python unittest.

---

### Task 1: Server Product Catalog Schema And Seeds

**Files:**
- Modify: `server/db.py`
- Test: `server/test_product_catalog.py`

- [ ] **Step 1: Write failing migration test**

Create `server/test_product_catalog.py` with a temp database. The test imports `main`, reads `products` and `office_product_settings`, and expects the current catalog to be seeded for `office-7`.

```python
def test_default_products_are_seeded_for_demo_office(self):
    with self.main.d.connect() as db:
        pds = db.execute("SELECT * FROM products WHERE code='PDS'").fetchone()
        self.assertIsNotNone(pds)
        setting = db.execute(
            "SELECT * FROM office_product_settings WHERE product_id=? AND org_unit_id='office-7'",
            (pds["id"],),
        ).fetchone()
        self.assertIsNotNone(setting)
        self.assertEqual(setting["requires_amount"], 1)
        self.assertEqual(setting["active"], 1)
```

- [ ] **Step 2: Run failing test**

Run: `.\.venv\Scripts\python.exe -m unittest server.test_product_catalog -v`

Expected before implementation: fails with missing `products` table.

- [ ] **Step 3: Add schema and migration**

Add tables:

```sql
CREATE TABLE IF NOT EXISTS products (
    id TEXT PRIMARY KEY,
    code TEXT UNIQUE NOT NULL,
    title TEXT NOT NULL,
    group_name TEXT NOT NULL DEFAULT '',
    description TEXT,
    archived INTEGER NOT NULL DEFAULT 0,
    created_by_user_id TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS office_product_settings (
    id TEXT PRIMARY KEY,
    product_id TEXT NOT NULL REFERENCES products(id),
    org_unit_id TEXT NOT NULL REFERENCES org_units(id),
    active INTEGER NOT NULL DEFAULT 1,
    points REAL NOT NULL DEFAULT 1,
    requires_amount INTEGER NOT NULL DEFAULT 0,
    counts_toward_plan INTEGER NOT NULL DEFAULT 1,
    sort_order INTEGER NOT NULL DEFAULT 0,
    updated_by_user_id TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    UNIQUE(product_id, org_unit_id)
);
```

Seed all current `CATEGORY_TITLES` into `products`, and create default settings for office org units. Set `requires_amount = 1` for `PDS`, `OPIF`, `KSP`.

- [ ] **Step 4: Run test to green**

Run: `.\.venv\Scripts\python.exe -m unittest server.test_product_catalog -v`

Expected: catalog migration test passes.

### Task 2: Server Sale Snapshots And Dynamic Validation

**Files:**
- Modify: `server/db.py`
- Modify: `server/main.py`
- Modify: `server/test_sales_batch.py`
- Test: `server/test_product_catalog.py`

- [ ] **Step 1: Write failing batch snapshot test**

Extend `server/test_sales_batch.py` so `POST /api/sales/batch` with `productId` stores a sale with `productTitle`, `points`, `pointsTotal`, and `requiresAmount`.

```python
def test_batch_sale_uses_office_product_snapshot(self):
    token = self.login_employee()
    product_id = self.product_id("PDS")
    response = self.client.post(
        "/api/sales/batch",
        headers={"Authorization": f"Bearer {token}"},
        json={"clientLast4": "1111", "items": [{"productId": product_id, "amount": 10000}]},
    )
    self.assertEqual(response.status_code, 200)
    sale = response.json()[0]
    self.assertEqual(sale["productTitle"], "ПДС")
    self.assertEqual(sale["points"], 1)
    self.assertEqual(sale["pointsTotal"], 1)
```

- [ ] **Step 2: Run failing test**

Run: `.\.venv\Scripts\python.exe -m unittest server.test_sales_batch -v`

Expected before implementation: fails because `productId` is ignored and snapshot fields do not exist.

- [ ] **Step 3: Add snapshot columns**

Add to `sales` via idempotent migration:

```sql
product_id TEXT,
product_code_snapshot TEXT,
product_title_snapshot TEXT,
product_group_snapshot TEXT,
points_snapshot REAL NOT NULL DEFAULT 1,
requires_amount_snapshot INTEGER NOT NULL DEFAULT 0,
counts_toward_plan_snapshot INTEGER NOT NULL DEFAULT 1
```

Backfill old rows from `category` and `CATEGORY_TITLES`.

- [ ] **Step 4: Update input/output models**

Change `SaleBatchItemIn` to accept:

```python
productId: str | None = None
category: str | None = None
```

Keep `category` compatibility. Resolve item by `productId` first, then by legacy `category`.

- [ ] **Step 5: Enforce employee-only sales**

Change sales endpoints so only `role == "EMPLOYEE"` can create/delete personal sales. Manager roles still use report and management endpoints.

- [ ] **Step 6: Run tests**

Run: `.\.venv\Scripts\python.exe -m unittest server.test_sales_batch server.test_product_catalog -v`

Expected: all server product and batch tests pass.

### Task 3: Server Product APIs And Permission Scope

**Files:**
- Modify: `server/main.py`
- Test: `server/test_product_catalog.py`

- [ ] **Step 1: Write failing API tests**

Add tests for active products, manager editing own office, and forbidding employee edits.

```python
def test_employee_gets_active_products_for_own_office(self):
    token = self.login_employee()
    response = self.client.get("/api/products/active", headers={"Authorization": f"Bearer {token}"})
    self.assertEqual(response.status_code, 200)
    self.assertTrue(any(p["code"] == "PDS" for p in response.json()))

def test_employee_cannot_edit_office_product(self):
    token = self.login_employee()
    product_id = self.product_id("PDS")
    response = self.client.patch(
        f"/api/management/offices/office-7/products/{product_id}",
        headers={"Authorization": f"Bearer {token}"},
        json={"points": 5},
    )
    self.assertEqual(response.status_code, 403)
```

- [ ] **Step 2: Run failing tests**

Run: `.\.venv\Scripts\python.exe -m unittest server.test_product_catalog -v`

Expected: fails with 404 for new endpoints.

- [ ] **Step 3: Implement DTOs and endpoints**

Add:

```python
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
```

Add routes:

- `GET /api/products/active`
- `GET /api/management/offices/{org_unit_id}/products`
- `PATCH /api/management/offices/{org_unit_id}/products/{product_id}`
- `GET /api/admin/products`
- `POST /api/admin/products`

- [ ] **Step 4: Add history**

Create `product_change_history` table and write rows for changed fields in `PATCH`.

- [ ] **Step 5: Run tests**

Run: `.\.venv\Scripts\python.exe -m unittest server.test_product_catalog -v`

Expected: product API tests pass.

### Task 4: Android Models, API, Repository, And Offline Queue

**Files:**
- Modify: `app/src/main/java/com/bank/salestracker/data/model/Models.kt`
- Modify: `app/src/main/java/com/bank/salestracker/data/api/ApiService.kt`
- Modify: `app/src/main/java/com/bank/salestracker/data/repository/Repositories.kt`
- Modify: `app/src/main/java/com/bank/salestracker/data/local/AppDb.kt`

- [ ] **Step 1: Add dynamic product models**

Add:

```kotlin
@Serializable
data class ProductSetting(
    val id: String,
    val productId: String,
    val code: String,
    val title: String,
    val groupName: String = "",
    val active: Boolean = true,
    val points: Double = 1.0,
    val requiresAmount: Boolean = false,
    val countsTowardPlan: Boolean = true,
    val sortOrder: Int = 0
)
```

Make `SaleBatchItem` use `productId: String?` while keeping `category: ProductCategory?` for compatibility.

- [ ] **Step 2: Add Retrofit calls**

Add:

```kotlin
@GET("products/active")
suspend fun activeProducts(): List<ProductSetting>

@GET("management/offices/{orgUnitId}/products")
suspend fun officeProducts(@Path("orgUnitId") orgUnitId: String): List<ProductSetting>

@PATCH("management/offices/{orgUnitId}/products/{productId}")
suspend fun updateOfficeProduct(
    @Path("orgUnitId") orgUnitId: String,
    @Path("productId") productId: String,
    @Body body: ProductSettingPatch
): ProductSetting
```

- [ ] **Step 3: Add repository methods**

Add `activeProducts()`, `officeProducts()`, and `updateOfficeProduct(...)`.

- [ ] **Step 4: Room offline compatibility**

Add `productId`, `productTitleSnapshot`, `pointsSnapshot`, and `requiresAmountSnapshot` columns to `PendingSale`. Keep legacy category fallback for old pending rows.

- [ ] **Step 5: Build**

Run: `.\gradlew.bat assembleDebug --console=plain` with Android Studio JBR.

Expected: build succeeds after model/API changes.

### Task 5: Android Dynamic Add Sale And Role-Aware Navigation

**Files:**
- Modify: `app/src/main/java/com/bank/salestracker/ui/screens/AddSaleScreen.kt`
- Modify: `app/src/main/java/com/bank/salestracker/ui/screens/DashboardScreen.kt`
- Modify: `app/src/main/java/com/bank/salestracker/ui/navigation/AppNavHost.kt`

- [ ] **Step 1: Load products from server**

`AddSaleVm` loads `ServiceLocator.salesRepo.activeProducts()` on init. UI shows loading and empty states.

- [ ] **Step 2: Select dynamic products**

Replace `ProductCategory.entries` with `vm.products.sortedBy { it.sortOrder }`.

- [ ] **Step 3: Amount validation**

Use `product.requiresAmount` instead of hardcoded `PDS/OPIF/KSP`.

- [ ] **Step 4: Send product IDs**

Build `SaleBatchItem(productId = product.productId, amount = amount, quantity = 1, comment = ...)`.

- [ ] **Step 5: Hide sale actions for managers**

Add `Role.canCreateSales(): Boolean = this == Role.EMPLOYEE`. Use it to hide bottom tab `AddSale`, block direct route with a simple access message, and hide dashboard add-sale button for managers.

- [ ] **Step 6: Build**

Run: `.\gradlew.bat assembleDebug --console=plain` with Android Studio JBR.

Expected: build succeeds.

### Task 6: Android Office Products Screen

**Files:**
- Create: `app/src/main/java/com/bank/salestracker/ui/screens/ProductManagementScreen.kt`
- Modify: `app/src/main/java/com/bank/salestracker/ui/navigation/AppNavHost.kt`
- Modify: `app/src/main/java/com/bank/salestracker/ui/screens/AdminScreen.kt`

- [ ] **Step 1: Add route**

Add `Dest.Products` labelled `Продукты`.

- [ ] **Step 2: Add entry point**

In `AdminScreen`, add a button/card `Продукты офиса`, opening the product route.

- [ ] **Step 3: Implement screen**

Screen loads the current user's `orgUnitId`, shows products, search, active switch, points field, requires-amount switch, and save action.

- [ ] **Step 4: Build**

Run: `.\gradlew.bat assembleDebug --console=plain` with Android Studio JBR.

Expected: build succeeds.

### Task 7: Reports, Verification, Install, Commit

**Files:**
- Modify: `server/main.py`
- Modify: `app/src/main/java/com/bank/salestracker/ui/screens/ReportsScreen.kt`
- Modify: `app/src/main/java/com/bank/salestracker/ui/screens/AdminScreen.kt`

- [ ] **Step 1: Include points in reports**

Server returns `todayPoints`, `monthPoints`, category points, and employee points using `points_snapshot * quantity`.

- [ ] **Step 2: Render points**

Android report screens show points next to counts and amounts.

- [ ] **Step 3: Full verification**

Run:

```powershell
.\.venv\Scripts\python.exe -m unittest server.test_sales_batch server.test_product_catalog -v
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat assembleDebug --console=plain
```

Expected: server tests pass and Android build succeeds.

- [ ] **Step 4: Restart server and install APK**

Restart FastAPI on `0.0.0.0:8000`, install `app\build\outputs\apk\debug\app-debug.apk` to the physical phone, and launch `com.bank.salestracker`.

- [ ] **Step 5: Commit and push**

Commit implementation:

```powershell
git add server app docs
git commit -m "Add office product catalog management"
git push
```
