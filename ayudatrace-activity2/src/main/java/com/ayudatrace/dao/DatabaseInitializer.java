package com.ayudatrace.dao;

import com.ayudatrace.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** Creates all required tables (if missing) before the CLI starts. */
public final class DatabaseInitializer {

    private static final String CREATE_USERS = """
            CREATE TABLE IF NOT EXISTS users (
                user_id       INTEGER PRIMARY KEY AUTOINCREMENT,
                username      TEXT NOT NULL UNIQUE COLLATE NOCASE,
                password_hash TEXT NOT NULL,
                full_name     TEXT NOT NULL,
                contact_no    TEXT,
                email         TEXT,
                role          TEXT NOT NULL CHECK (role IN ('ADMIN','STAFF','HOUSEHOLD_REP')),
                status        TEXT NOT NULL DEFAULT 'PENDING'
                              CHECK (status IN ('PENDING','APPROVED','INACTIVE'))
            )
            """;

    private static final String CREATE_HOUSEHOLDS = """
            CREATE TABLE IF NOT EXISTS households (
                household_id  INTEGER PRIMARY KEY AUTOINCREMENT,
                head_name     TEXT NOT NULL,
                purok_address TEXT NOT NULL,
                contact_no    TEXT,
                member_count  INTEGER NOT NULL DEFAULT 1,
                category      TEXT,
                user_id       INTEGER,
                status        TEXT NOT NULL DEFAULT 'ACTIVE',
                FOREIGN KEY (user_id) REFERENCES users(user_id)
            )
            """;

    private static final String CREATE_AID_PROGRAMS = """
            CREATE TABLE IF NOT EXISTS aid_programs (
                program_id      INTEGER PRIMARY KEY AUTOINCREMENT,
                program_name    TEXT NOT NULL,
                aid_type        TEXT NOT NULL CHECK (aid_type IN ('RELIEF_GOODS','CASH')),
                items_or_amount TEXT NOT NULL,
                target_purok    TEXT,
                program_date    TIMESTAMP,
                created_by      INTEGER NOT NULL,
                status          TEXT NOT NULL DEFAULT 'PLANNED'
                                CHECK (status IN ('PLANNED','ONGOING','COMPLETED','CANCELLED')),
                FOREIGN KEY (created_by) REFERENCES users(user_id)
            )
            """;

    private static final String CREATE_DISTRIBUTIONS = """
            CREATE TABLE IF NOT EXISTS distributions (
                distribution_id         INTEGER PRIMARY KEY AUTOINCREMENT,
                program_id              INTEGER NOT NULL,
                household_id            INTEGER NOT NULL,
                claimant_name           TEXT NOT NULL,
                items_or_amount_received TEXT NOT NULL,
                claim_date              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                released_by             INTEGER NOT NULL,
                status                  TEXT NOT NULL DEFAULT 'CLAIMED'
                                        CHECK (status IN ('CLAIMED','VOIDED')),
                FOREIGN KEY (program_id)   REFERENCES aid_programs(program_id),
                FOREIGN KEY (household_id) REFERENCES households(household_id),
                FOREIGN KEY (released_by)  REFERENCES users(user_id),
                UNIQUE (program_id, household_id)
            )
            """;

    private static final String CREATE_AUDIT_LOGS = """
            CREATE TABLE IF NOT EXISTS audit_logs (
                log_id        INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id       INTEGER NOT NULL,
                action        TEXT NOT NULL,
                table_name    TEXT,
                record_id     INTEGER,
                log_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (user_id) REFERENCES users(user_id)
            )
            """;

    private DatabaseInitializer() {
    }

    public static void initialize() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement()) {
            st.execute(CREATE_USERS);
            st.execute(CREATE_HOUSEHOLDS);
            st.execute(CREATE_AID_PROGRAMS);
            st.execute(CREATE_DISTRIBUTIONS);
            st.execute(CREATE_AUDIT_LOGS);
            seedDefaultAdmin(conn);
            System.out.println("[DB] Database ready.");
        } catch (SQLException e) {
            throw new IllegalStateException("Could not initialize the database: " + e.getMessage(), e);
        }
    }

    /** Makes sure at least one APPROVED admin exists so pending accounts can be approved. */
    private static void seedDefaultAdmin(Connection conn) throws SQLException {
        String countSql = "SELECT COUNT(*) FROM users WHERE role = 'ADMIN'";
        try (PreparedStatement ps = conn.prepareStatement(countSql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next() && rs.getInt(1) > 0) {
                return;
            }
        }

        String insertSql = "INSERT INTO users (username, password_hash, full_name, role, status) "
                + "VALUES (?, ?, ?, 'ADMIN', 'APPROVED')";
        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
            ps.setString(1, "admin");
            ps.setString(2, PasswordUtil.hash("admin123"));
            ps.setString(3, "System Administrator");
            ps.executeUpdate();
        }
        System.out.println("[DB] Default admin created -> username: admin, password: admin123 (change this later).");
    }
}
