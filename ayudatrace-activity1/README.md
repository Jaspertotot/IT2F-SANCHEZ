# AyudaTrace - Activity 1: Single entry point, DatabaseInitializer, Authentication

## Run (terminal)
    mvn compile exec:java

## Run (IntelliJ)
Open the folder (it detects pom.xml), let Maven import, then run `com.ayudatrace.Main`.

Requires JDK 17 or newer. The database file `ayudatrace.db` is created in the project folder on first run.
Default admin created on first run: `admin` / `admin123`.

## Proof of execution
1. Run Main, choose Register, create an account (terminal screenshot showing the DB ready message and "Registration successful").
2. Open ayudatrace.db in SQLite Studio / DB Browser, open the users table, screenshot the new row with status PENDING.
