package com.ayudatrace.dao;

import com.ayudatrace.model.User;
import com.ayudatrace.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseInitializer {

    private DatabaseInitializer() {}

    public static void initialize() {
        try (Connection c = DatabaseConnection.getConnection();
             Statement st = c.createStatement()) {

            st.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    user_id       INTEGER PRIMARY KEY AUTOINCREMENT,
                    username      TEXT NOT NULL UNIQUE,
                    password_hash TEXT NOT NULL,
                    full_name     TEXT NOT NULL,
                    contact_no    TEXT,
                    email         TEXT,
                    role          TEXT NOT NULL CHECK (role IN ('ADMINISTRATOR','BARANGAY_STAFF','HOUSEHOLD_REPRESENTATIVE')),
                    status        TEXT NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','ACTIVE','INACTIVE'))
                )""");

            st.execute("""
                CREATE TABLE IF NOT EXISTS households (
                    household_id  INTEGER PRIMARY KEY AUTOINCREMENT,
                    head_name     TEXT NOT NULL,
                    purok_address TEXT NOT NULL,
                    contact_no    TEXT,
                    member_count  INTEGER NOT NULL CHECK (member_count > 0),
                    category      TEXT NOT NULL,
                    user_id       INTEGER REFERENCES users(user_id) ON DELETE SET NULL,
                    status        TEXT NOT NULL DEFAULT 'ACTIVE'
                )""");

            st.execute("""
                CREATE TABLE IF NOT EXISTS aid_programs (
                    program_id       INTEGER PRIMARY KEY AUTOINCREMENT,
                    program_name     TEXT NOT NULL,
                    aid_type         TEXT NOT NULL CHECK (aid_type IN ('RELIEF_GOODS','CASH')),
                    items_or_amount  TEXT NOT NULL,
                    target_purok     TEXT,
                    program_date     TEXT NOT NULL,
                    created_by       INTEGER NOT NULL REFERENCES users(user_id),
                    status           TEXT NOT NULL DEFAULT 'PLANNED'
                                     CHECK (status IN ('PLANNED','ONGOING','COMPLETED','CANCELLED'))
                )""");

            st.execute("""
                CREATE TABLE IF NOT EXISTS distributions (
                    distribution_id          INTEGER PRIMARY KEY AUTOINCREMENT,
                    program_id               INTEGER NOT NULL REFERENCES aid_programs(program_id),
                    household_id             INTEGER NOT NULL REFERENCES households(household_id),
                    claimant_name            TEXT NOT NULL,
                    items_or_amount_received TEXT NOT NULL,
                    claim_date               TIMESTAMP NOT NULL DEFAULT (datetime('now','localtime')),
                    released_by              INTEGER NOT NULL REFERENCES users(user_id),
                    status                   TEXT NOT NULL DEFAULT 'CLAIMED' CHECK (status IN ('CLAIMED','VOIDED'))
                )""");

            // One ACTIVE claim per household per program (a voided claim may be re-claimed).
            st.execute("""
                CREATE UNIQUE INDEX IF NOT EXISTS idx_one_active_claim
                ON distributions(program_id, household_id) WHERE status = 'CLAIMED'""");

            // ---- Deliverable 8: audit trail table ----
            st.execute("""
                CREATE TABLE IF NOT EXISTS activity_logs (
                    id          INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id     INTEGER NOT NULL,
                    action_type TEXT NOT NULL,
                    description TEXT NOT NULL,
                    timestamp   TIMESTAMP NOT NULL DEFAULT (datetime('now','localtime')),
                    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
                )""");
            st.execute("CREATE INDEX IF NOT EXISTS idx_activity_logs_user ON activity_logs(user_id)");

            seedDefaultAdmin(c);
        } catch (SQLException e) {
            throw new DataAccessException("Database initialization failed: " + e.getMessage(), e);
        }
    }

    /** Creates a first administrator so someone can log in and approve other accounts. */
    private static void seedDefaultAdmin(Connection c) throws SQLException {
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM users")) {
            if (rs.next() && rs.getInt(1) > 0) return;
        }
        String sql = "INSERT INTO users (username, password_hash, full_name, role, status) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, "admin");
            ps.setString(2, PasswordUtil.hash("admin123"));
            ps.setString(3, "System Administrator");
            ps.setString(4, User.ROLE_ADMIN);
            ps.setString(5, User.STATUS_ACTIVE);
            ps.executeUpdate();
        }
        System.out.println("[Setup] Default admin created -> username: admin | password: admin123");
    }
}
