# AyudaTrace - Deliverable 8: Audit Log Implementation

Java 17 + Maven + SQLite (console app).

## Run in IntelliJ
1. File > Open > select this folder (the one containing `pom.xml`) > "Open as Project".
2. Wait for Maven to download dependencies (sqlite-jdbc). Project SDK: JDK 17 or newer.
3. Run `src/main/java/com/ayudatrace/Main.java`.
   The database file `ayudatrace.db` is created in the project root.
4. Or from a terminal: `mvn compile exec:java`

First login: `admin` / `admin123` (created automatically on first run).

## Where each requirement lives
| Requirement | File |
|---|---|
| `activity_logs` table (auto-created) | `dao/DatabaseInitializer.java` |
| `logAction`, `getAllLogs`, `getLogsByUserId` | `dao/ActivityLogDao.java` |
| Log record | `model/ActivityLog.java` |
| Login / logout logging | `ui/Authentication.java` -> `login()` / `logout()` |
| Account approval / role-status logging | `dao/UserDao.java` -> `approveUser()` / `updateUserRoleAndStatus()` |
| Core business process (record aid claim) | `dao/DistributionDao.java` -> `recordClaim()` |
| Lifecycle change (void claim) | `dao/DistributionDao.java` -> `voidClaim()` |
| Also logged | household registration, program creation/status change, blocked duplicate claims, registration |
| Admin audit log screen | `ui/AdminMenu.java` -> option 5 "View System Audit Logs" |

## Log action types
USER_REGISTERED, USER_LOGIN, USER_LOGOUT, USER_APPROVED, USER_UPDATED, HOUSEHOLD_ADDED,
PROGRAM_CREATED, PROGRAM_UPDATED, CLAIM_RECORDED, CLAIM_BLOCKED, CLAIM_VOIDED, LOGS_CLEARED

## Demo script for the screenshots
1. Login as `admin` (USER_LOGIN).
2. Menu 4 > register a household, create a program, set it to ONGOING (HOUSEHOLD_ADDED, PROGRAM_CREATED, PROGRAM_UPDATED).
3. Menu 4 > Record Aid Claim (CLAIM_RECORDED). Record the same household again (CLAIM_BLOCKED).
4. Menu 4 > Void Aid Claim (CLAIM_VOIDED).
5. Register a second account from the start menu, login as admin, menu 1 approve it (USER_APPROVED), menu 2 change its role (USER_UPDATED).
6. Logout (USER_LOGOUT), login again, menu 5 > 1 View All System Logs -> **Screenshot 1**.
   Also try typing letters at the "Enter User ID" prompt to show it re-prompts.
7. Open `ayudatrace.db` in DB Browser for SQLite / SQLite Studio > table `activity_logs` -> **Screenshot 2**.
