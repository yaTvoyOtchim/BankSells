# Glass Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Apply the approved premium glass redesign to the Android app, using light iOS-style glass for system light mode and dark premium glass for system dark mode.

**Architecture:** Add reusable Compose design-system components instead of styling every screen from scratch. Keep server and data logic unchanged. Update the main screens to use `AppBackground`, `GlassSurface`, `MetricGlassCard`, and glass navigation/top panels while preserving dense, readable inputs and primary buttons.

**Tech Stack:** Android Kotlin, Jetpack Compose Material3, existing VTB palette, system dark/light theme via `isSystemInDarkTheme()`.

---

### Task 1: Theme And Glass Components

**Files:**
- Modify: `app/src/main/java/com/bank/salestracker/ui/theme/Theme.kt`
- Create: `app/src/main/java/com/bank/salestracker/ui/theme/GlassComponents.kt`

- [ ] **Step 1: Add premium glass tokens**

Add light/dark background brushes, glass colors, glass borders, and helper functions:

```kotlin
val VtbBlue = Color(0xFF1E4FFF)
val VtbCyan = Color(0xFF13B8E0)
val VtbMint = Color(0xFF00C9A7)
val DeepNavy = Color(0xFF071426)

@Composable
fun appBackgroundBrush(): Brush =
    if (isSystemInDarkTheme()) {
        Brush.linearGradient(listOf(Color(0xFF020918), Color(0xFF071D4D), Color(0xFF062028)))
    } else {
        Brush.linearGradient(listOf(Color(0xFFFFFFFF), Color(0xFFEEF6FF), Color(0xFFE9FFF9)))
    }
```

- [ ] **Step 2: Add glass components**

Create:

```kotlin
@Composable fun AppBackground(...)
@Composable fun GlassSurface(...)
@Composable fun MetricGlassCard(...)
@Composable fun GlassTopBar(...)
```

Use translucent colors, rounded corners, borders, and shadows. Use `Modifier.blur()` lightly only for background accents if needed; do not blur text.

- [ ] **Step 3: Build**

Run:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat assembleDebug --console=plain
```

Expected: `BUILD SUCCESSFUL`.

### Task 2: App Navigation And Dashboard

**Files:**
- Modify: `app/src/main/java/com/bank/salestracker/ui/navigation/AppNavHost.kt`
- Modify: `app/src/main/java/com/bank/salestracker/ui/screens/DashboardScreen.kt`

- [ ] **Step 1: Glass bottom navigation**

Wrap the `NavigationBar` in a glass surface and use transparent container colors:

```kotlin
NavigationBar(containerColor = Color.Transparent, tonalElevation = 0.dp)
```

- [ ] **Step 2: Dashboard glass layout**

Wrap dashboard content in `AppBackground`. Replace the hero card and metric cards with `GlassSurface` / `MetricGlassCard`. Keep the floating action button as a solid VTB gradient-style button.

- [ ] **Step 3: Build**

Run Android build and expect `BUILD SUCCESSFUL`.

### Task 3: Add Sale Screen

**Files:**
- Modify: `app/src/main/java/com/bank/salestracker/ui/screens/AddSaleScreen.kt`

- [ ] **Step 1: Apply background and top glass**

Use `AppBackground` and a `GlassTopBar`-style top section. Keep `OutlinedTextField` dense and readable.

- [ ] **Step 2: Product selector glass**

Place the product chips in a `GlassSurface`. Selected chips stay solid blue/mint.

- [ ] **Step 3: Add summary glass panel**

Show selected product count and expected points in a small glass panel above the save button:

```kotlin
Text("${vm.selectedProducts.size} продуктов · ${formatPoints(totalPoints)} б.")
```

- [ ] **Step 4: Build**

Run Android build and expect `BUILD SUCCESSFUL`.

### Task 4: Reports And Manager Screens

**Files:**
- Modify: `app/src/main/java/com/bank/salestracker/ui/screens/ReportsScreen.kt`
- Modify: `app/src/main/java/com/bank/salestracker/ui/screens/AdminScreen.kt`
- Modify: `app/src/main/java/com/bank/salestracker/ui/screens/ProductManagementScreen.kt`

- [ ] **Step 1: Reports**

Use `AppBackground`, `GlassSurface` for monthly product stats, and dense cards for today's sale list.

- [ ] **Step 2: Manager dashboard**

Use glass metric cards for office summary. Keep employee ranking rows readable.

- [ ] **Step 3: Product management**

Use premium glass cards for product settings. Keep fields and switches clear. Primary save button remains solid.

- [ ] **Step 4: Build**

Run Android build and expect `BUILD SUCCESSFUL`.

### Task 5: Verification, Install, Push

**Files:**
- Verify Android and server tests.

- [ ] **Step 1: Run verification**

Run:

```powershell
.\.venv\Scripts\python.exe -m unittest server.test_sales_batch server.test_product_catalog -v
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat assembleDebug --console=plain
```

Expected: server tests pass and Android build succeeds.

- [ ] **Step 2: Install and launch**

Install `app\build\outputs\apk\debug\app-debug.apk` on the physical phone if connected, otherwise on the emulator. Launch `com.bank.salestracker`.

- [ ] **Step 3: Commit and push**

Commit:

```powershell
git add app docs
git commit -m "Apply premium glass redesign"
git push
```
