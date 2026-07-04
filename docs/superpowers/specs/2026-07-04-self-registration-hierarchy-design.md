# Self Registration And Organization Hierarchy Design

## Goal

Build a registration and access model where bank employees install the app, register themselves with their known VTB employee number, choose their own password, and wait until a manager attaches them to the correct organization unit.

The system should support a full hierarchy from the start:

`Directorate -> Region -> Office/Branch -> Team`

The interface can stay simple at first, but the data model and permissions must be ready for office, regional, and director-level reporting.

## Core Decisions

- Employee numbers are unique strings in the format `vtb` plus digits, for example `vtb70336144`.
- Employees register themselves. Managers do not manually create every account.
- A newly registered employee is active for login but blocked from work features until assigned.
- Each employee has exactly one active assignment at a time.
- Office/branch assignment is required.
- Team assignment is optional.
- Transfers are supported by closing the old assignment and opening a new one.
- Sales keep the organization unit from the moment of sale, so historical reports do not change after transfers.

## Roles

`EMPLOYEE`

Can see and manage their own sales, reports, profile, and assignment status.

`TEAM_LEAD`

Can see their team and all employees assigned to that team.

`OFFICE_MANAGER`

Can see the office/branch and all teams inside it.

`REGIONAL_MANAGER`

Can see the region and all offices/branches inside it.

`DIRECTOR`

Can see one or more directorates and everything below them.

`ADMIN`

Has full technical access for support, recovery, and configuration.

## Organization Units

Use one flexible table for the whole hierarchy.

Fields:

- `id`
- `name`
- `type`: `DIRECTORATE`, `REGION`, `OFFICE`, `TEAM`
- `parent_id`
- `code`
- `active`
- `created_at`
- `updated_at`

Rules:

- `DIRECTORATE` has no parent.
- `REGION` parent is `DIRECTORATE`.
- `OFFICE` parent is `REGION`.
- `TEAM` parent is `OFFICE`.
- Reports can be calculated for any node by including all descendant nodes.

## Users

User fields:

- `id`
- `employee_id`
- `full_name`
- `password_hash`
- `role`
- `registration_status`: `PENDING_ASSIGNMENT`, `ACTIVE`, `BLOCKED`, `DEACTIVATED`
- `primary_org_unit_id`
- `created_at`
- `updated_at`

Registration status rules:

- After self-registration, status is `PENDING_ASSIGNMENT`.
- Pending users can log in but only see the waiting screen and profile.
- After manager assignment, status becomes `ACTIVE`.
- Blocked or deactivated users cannot access work features.

## Assignments

Assignments track where a user belongs and preserve transfer history.

Fields:

- `id`
- `user_id`
- `org_unit_id`
- `role_at_unit`
- `assigned_by_user_id`
- `started_at`
- `ended_at`
- `comment`

Rules:

- Only one assignment with `ended_at = null` is allowed per user.
- Transferring an employee sets `ended_at` on the current assignment and creates a new assignment.
- Managers can only assign employees into organization units they are allowed to manage.

## Sales

Sales should store the employee and the historical organization unit.

Fields added or preserved:

- `user_id`
- `employee_id`
- `client_last4`
- `product_id`
- `quantity`
- `amount`
- `org_unit_id`
- `manager_id`
- `created_at`
- `updated_at`
- `source`

Rules:

- `client_last4` stores only the last four digits of the phone number.
- Duplicate last-four values are allowed because different clients can share them.
- `org_unit_id` is copied from the employee active assignment at sale creation time.
- Edited sales must keep edit history.

## Registration Flow

1. Employee opens the app and taps registration.
2. Employee enters employee number, full name, password, and password confirmation.
3. Server normalizes employee number to lowercase.
4. If the employee number already exists, registration is rejected.
5. Account is created with `PENDING_ASSIGNMENT`.
6. Employee logs in and sees the waiting screen.
7. Employee copies or sends their employee number to the manager.

## Waiting Screen

Show:

- full name;
- employee number;
- status;
- copy employee number button;
- message that the manager must add the employee to an office.

No sales, reports, or team screens are available while pending.

## Manager Assignment Flow

1. Manager opens `Team -> Add employee`.
2. Manager enters the employee number.
3. App shows the registered employee card.
4. Manager selects an office/branch. If the manager only has one office, it is selected automatically.
5. Manager optionally selects a team.
6. Manager confirms assignment.
7. Employee status changes to `ACTIVE`.
8. Employee gets access to sales and reports on next refresh/login.

## Higher-Level Management Flow

Higher-level managers see a tree:

`Directorate -> Region -> Office/Branch -> Team -> Employees`

They can open:

- regional summary;
- office summary;
- team summary;
- employee card;
- employee sales;
- export for their visible scope.

Visibility is calculated from the manager active assignment and role.

## API Additions

Authentication:

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/change-password`

Profile:

- `GET /api/profile`
- `GET /api/profile/assignment`

Organization:

- `GET /api/org/tree`
- `GET /api/org/units`
- `POST /api/org/units`
- `PATCH /api/org/units/{id}`

Management:

- `GET /api/management/pending-users`
- `GET /api/management/users/{employee_id}`
- `POST /api/management/users/{employee_id}/assign`
- `POST /api/management/users/{employee_id}/transfer`
- `POST /api/management/users/{employee_id}/block`

Reports:

- `GET /api/reports/scope?orgUnitId=&period=`
- `GET /api/reports/export?orgUnitId=&period=&format=csv|pdf|xlsx`

## Android App Changes

Navigation:

- Add registration screen.
- Add waiting-for-assignment screen.
- Route pending users away from sales/reports.
- Add profile assignment details.
- Add management screens for managers.

Screens:

- Login
- Registration
- Waiting for assignment
- Profile
- Add employee by employee number
- Organization tree
- Scope reports
- Employee detail
- Transfer employee

## Validation And Security

- Employee number must match `vtb` plus digits.
- Password must meet local password requirements.
- Registration must reject duplicate employee numbers.
- Pending users cannot create sales.
- Managers cannot assign users outside their allowed subtree.
- Every assignment, transfer, block, and role change is written to audit history.
- Full phone numbers are not stored for sales, only `client_last4`.

## Migration From Current Prototype

Current `users.branch` should become a derived display value from the active assignment.

Existing users should be migrated:

- current admin user becomes `ADMIN`;
- current employee user becomes `EMPLOYEE`;
- demo office hierarchy is created;
- both users receive active assignments.

Existing reports should be updated to filter by organization subtree instead of all users.

## First Implementation Scope

Implement first:

- database schema extension;
- self-registration API;
- pending assignment status;
- Android registration screen;
- Android waiting screen;
- manager add-by-employee-number flow;
- organization-unit tree basics;
- report filtering by visible organization scope.

Defer:

- QR invite codes;
- employee self-request to a selected office;
- push notifications for manager approvals;
- advanced multi-directorate access rules beyond one active manager node.
