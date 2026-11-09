package com.ayudatrace.dao;

import com.ayudatrace.model.ActivityLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** All SQL for the activity_logs table lives here (no SQL in the UI layer). */
public class ActivityLogDao {

    private static final String INSERT_SQL =
            "INSERT INTO activity_logs (user_id, action_type, description) VALUES (?, ?, ?)";

    /** Inserts one log entry using its own connection. */
    public void logAction(int userId, String actionType, String description) {
        try (Connection c = DatabaseConnection.getConnection()) {
            logAction(c, userId, actionType, description);
        } catch (SQLException e) {
            throw new DataAccessException("Could not write activity log: " + e.getMessage(), e);
        }
    }

    /**
     * Inserts a log entry on an existing connection, so a business change and its log
     * row share one transaction (this also avoids SQLite "database is locked" errors).
     */
    public void logAction(Connection c, int userId, String actionType, String description) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(INSERT_SQL)) {
            ps.setInt(1, userId);
            ps.setString(2, actionType);
            ps.setString(3, description);
            ps.executeUpdate();
        }
    }

    /** All logs, newest first. */
    public List<ActivityLog> getAllLogs() {
        return query("SELECT id, user_id, action_type, description, timestamp FROM activity_logs "
                + "ORDER BY timestamp DESC, id DESC", null);
    }

    /** Logs of one user, newest first. */
    public List<ActivityLog> getLogsByUserId(int userId) {
        return query("SELECT id, user_id, action_type, description, timestamp FROM activity_logs "
                + "WHERE user_id = ? ORDER BY timestamp DESC, id DESC", userId);
    }

    /**
     * Deletes all historical logs, then records who cleared them (so the clearing itself is audited).
     * Returns how many entries were removed.
     */
    public int clearAllLogs(int adminId) {
        return DatabaseConnection.inTransaction(c -> {
            int deleted;
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM activity_logs")) {
                deleted = ps.executeUpdate();
            }
            logAction(c, adminId, "LOGS_CLEARED",
                    "Admin " + adminId + " cleared " + deleted + " log entries.");
            return deleted;
        });
    }

    private List<ActivityLog> query(String sql, Integer userId) {
        List<ActivityLog> logs = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (userId != null) ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(new ActivityLog(rs.getInt("id"), rs.getInt("user_id"),
                            rs.getString("action_type"), rs.getString("description"),
                            rs.getString("timestamp")));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not read activity logs: " + e.getMessage(), e);
        }
        return logs;
    }
}
