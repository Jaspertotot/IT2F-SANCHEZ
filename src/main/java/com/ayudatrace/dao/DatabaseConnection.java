package com.ayudatrace.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseConnection {
    /** The SQLite file is created in the project root (IntelliJ's default working directory). */
    private static final String URL = "jdbc:sqlite:ayudatrace.db";

    private DatabaseConnection() {}

    public static Connection getConnection() throws SQLException {
        Connection c = DriverManager.getConnection(URL);
        // SQLite ignores foreign keys unless this is enabled on every connection.
        try (Statement st = c.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
        }
        return c;
    }

    @FunctionalInterface
    public interface SqlWork<T> {
        T run(Connection c) throws SQLException;
    }

    /**
     * Runs the work in one transaction. Business changes and their audit log row are
     * committed together, or rolled back together.
     */
    public static <T> T inTransaction(SqlWork<T> work) {
        try (Connection c = getConnection()) {
            c.setAutoCommit(false);
            try {
                T result = work.run(c);
                c.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Database error: " + e.getMessage(), e);
        }
    }
}
