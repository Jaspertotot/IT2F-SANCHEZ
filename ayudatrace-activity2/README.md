# AyudaTrace - Activity 2: Admin menu: view users and approve pending accounts

## Run (terminal)
    mvn compile exec:java

## Run (IntelliJ)
Open the folder (it detects pom.xml), let Maven import, then run `com.ayudatrace.Main`.

Requires JDK 17 or newer. The database file `ayudatrace.db` is created in the project folder on first run.
Default admin created on first run: `admin` / `admin123`.

## Proof of execution
1. Register a test account, log in as admin, choose 2 (pending list) then 3 (approve), enter the User ID. Screenshot the table and the SUCCESS message.
2. Open ayudatrace.db, users table, screenshot the row now showing APPROVED.
