# Self Registration Hierarchy Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add VTB-branded self-registration, pending assignment, and manager attachment to the organization hierarchy.

**Architecture:** Extend the existing FastAPI + SQLite server with organization units, assignment history, user status, and self-registration. Extend the Android Compose app with registration, waiting state, role-aware routing, and manager add-by-employee-number flow. Keep the existing screens and repository style.

**Tech Stack:** Android Kotlin/Compose, Retrofit, FastAPI, SQLite, bcrypt, adaptive launcher icon resources.

---

### Task 1: VTB Branding Assets

**Files:**
- Create: `app/src/main/res/drawable/vtb_icon.webp`
- Modify: `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/java/com/bank/salestracker/ui/screens/LoginScreen.kt`

- [ ] Copy `C:\Users\sigma52\Desktop\VTB ICON.webp` to `app/src/main/res/drawable/vtb_icon.webp`.
- [ ] Point the adaptive launcher foreground at `@drawable/vtb_icon`.
- [ ] Rename app label from `Продажи Банка` to `ВТБ Продажи`.
- [ ] Replace the login `AccountBalance` icon with the VTB bitmap.
- [ ] Run `gradle assembleDebug` and expect `BUILD SUCCESSFUL`.

### Task 2: Server Data Model

**Files:**
- Modify: `server/db.py`
- Modify: `server/main.py`

- [ ] Add SQLite tables `org_units` and `user_assignments`.
- [ ] Add user columns `registration_status`, `primary_org_unit_id`, `created_at`, `updated_at`.
- [ ] Add sales columns `org_unit_id`, `manager_id`, `client_last4`, `updated_at`.
- [ ] Add idempotent migration helpers using `PRAGMA table_info`.
- [ ] Seed demo hierarchy `ВТБ -> Крымский регион -> Отделение №7 -> Основная команда`.
- [ ] Assign existing demo users to the demo office/team.

### Task 3: Server Registration And Permissions

**Files:**
- Modify: `server/main.py`

- [ ] Add `POST /api/auth/register` with employee number validation `vtb\d+`.
- [ ] Return user registration status in `user_dto`.
- [ ] Add `require_work_access` so pending users cannot create sales or open reports.
- [ ] Add `manager_org_scope` helpers for manager-visible subtrees.
- [ ] Update report and admin queries to filter by visible organization scope.

### Task 4: Server Management API

**Files:**
- Modify: `server/main.py`

- [ ] Add `GET /api/management/pending-users`.
- [ ] Add `GET /api/management/users/{employee_id}`.
- [ ] Add `POST /api/management/users/{employee_id}/assign`.
- [ ] Close the old active assignment when assigning a user.
- [ ] Mark assigned users as `ACTIVE`.
- [ ] Audit registration, assignment, and transfer actions.

### Task 5: Android Models And API

**Files:**
- Modify: `app/src/main/java/com/bank/salestracker/data/model/Models.kt`
- Modify: `app/src/main/java/com/bank/salestracker/data/api/ApiService.kt`
- Modify: `app/src/main/java/com/bank/salestracker/data/repository/Repositories.kt`
- Modify: `app/src/main/java/com/bank/salestracker/data/local/TokenStore.kt`

- [ ] Add `RegisterRequest`, `RegistrationStatus`, `OrgUnit`, `AssignmentRequest`.
- [ ] Extend `Role` with `TEAM_LEAD`, `OFFICE_MANAGER`, `REGIONAL_MANAGER`, `DIRECTOR`, `ADMIN`.
- [ ] Persist user registration status and organization display fields.
- [ ] Add repository functions for register, pending users, lookup user, assignment, and org units.

### Task 6: Android Registration And Waiting Flow

**Files:**
- Modify: `app/src/main/java/com/bank/salestracker/ui/navigation/AppNavHost.kt`
- Modify: `app/src/main/java/com/bank/salestracker/ui/screens/LoginScreen.kt`
- Create: `app/src/main/java/com/bank/salestracker/ui/screens/WaitingScreen.kt`

- [ ] Add registration route and button from login.
- [ ] Register with employee number, full name, password, repeat password.
- [ ] Route `PENDING_ASSIGNMENT` users to the waiting screen after login.
- [ ] Waiting screen shows employee number and logout action.
- [ ] Hide work tabs while user is pending.

### Task 7: Android Manager Assignment Flow

**Files:**
- Modify: `app/src/main/java/com/bank/salestracker/ui/screens/AdminScreen.kt`
- Modify: `app/src/main/java/com/bank/salestracker/ui/navigation/AppNavHost.kt`

- [ ] Treat `TEAM_LEAD`, `OFFICE_MANAGER`, `REGIONAL_MANAGER`, `DIRECTOR`, and `ADMIN` as management roles.
- [ ] Add an employee-number input on the team screen.
- [ ] Look up a registered employee by `vtb...`.
- [ ] Show found employee status.
- [ ] Assign the employee to the manager's default office/team.
- [ ] Refresh the management report after assignment.

### Task 8: Verification

**Files:**
- Test through server and Android build.

- [ ] Run server registration for a new `vtb...` account and expect `PENDING_ASSIGNMENT`.
- [ ] Verify pending login succeeds but `/api/reports/my` returns 403.
- [ ] Verify manager assignment changes status to `ACTIVE`.
- [ ] Verify assigned user can open `/api/reports/my`.
- [ ] Run Android `assembleDebug` and expect `BUILD SUCCESSFUL`.
