package com.ayudatrace.dao;

import com.ayudatrace.model.User;
import com.ayudatrace.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class UserDao {

    private static final String COLS = "user_id, username, full_name, contact_no, email, role, status";
    private final ActivityLogDao logDao = new ActivityLogDao();

    // ------------------------------------------------------------------ create

    /** Registers a new account (status PENDING) and logs USER_REGISTERED. Returns the new user ID. */
    public int registerUser(String username, String password, String fullName,
                            String contactNo, String email, String role) {
        return DatabaseConnection.inTransaction(c -> {
            String sql = "INSERT INTO users (username, password_hash, full_name, contact_no, email, role, status) "
                    + "VALUES (?,?,?,?,?,?,?)";
            int newId;
            try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, username);
                ps.setString(2, PasswordUtil.hash(password));
                ps.setString(3, fullName);
                ps.setString(4, contactNo);
                ps.setString(5, email);
                ps.setString(6, role);
                ps.setString(7, User.STATUS_PENDING);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    newId = keys.getInt(1);
                }
            }
            logDao.logAction(c, newId, "USER_REGISTERED",
                    "User " + username + " (ID: " + newId + ") registered as " + role + ", pending approval.");
            return newId;
        });
    }

    // -------------------------------------------------------------------- read

    /** Returns the user if username/password match (any status), otherwise null. */
    public User authenticate(String username, String password) {
        String sql = "SELECT " + COLS + ", password_hash FROM users WHERE username = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && PasswordUtil.verify(password, rs.getString("password_hash"))) {
                    return map(rs);
                }
            }
            return null;
        } catch (SQLException e) {
            throw new DataAccessException("Login lookup failed: " + e.getMessage(), e);
        }
    }

    public boolean usernameExists(String username) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT 1 FROM users WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Username check failed: " + e.getMessage(), e);
        }
    }

    public User findById(int userId) {
        List<User> list = select("SELECT " + COLS + " FROM users WHERE user_id = ?", userId);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<User> getAllUsers() {
        return select("SELECT " + COLS + " FROM users ORDER BY user_id", null);
    }

    public List<User> getPendingUsers() {
        return select("SELECT " + COLS + " FROM users WHERE status = 'PENDING' ORDER BY user_id", null);
    }

    // ------------------------------------------------------------------ update

    /** Approves a PENDING account and logs USER_APPROVED. Returns false if the user is not pending. */
    public boolean approveUser(int adminId, int userId) {
        return DatabaseConnection.inTransaction(c -> {
            String sql = "UPDATE users SET status = 'ACTIVE' WHERE user_id = ? AND status = 'PENDING'";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, userId);
                if (ps.executeUpdate() == 0) return false;
            }
            logDao.logAction(c, adminId, "USER_APPROVED",
                    "Admin " + adminId + " approved account for User ID " + userId);
            return true;
        });
    }

    /**
     * Changes a user's role and/or status and logs USER_UPDATED.
     * Returns false when nothing changed. Admins cannot edit their own account.
     */
    public boolean updateUserRoleAndStatus(int adminId, int userId, String newRole, String newStatus) {
        if (adminId == userId) {
            throw new IllegalArgumentException("You cannot edit your own account.");
        }
        if (!User.ROLES.contains(newRole) || !User.STATUSES.contains(newStatus)) {
            throw new IllegalArgumentException("Invalid role or status.");
        }
        return DatabaseConnection.inTransaction(c -> {
            String oldRole;
            String oldStatus;
            try (PreparedStatement ps = c.prepareStatement("SELECT role, status FROM users WHERE user_id = ?")) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new IllegalArgumentException("User ID " + userId + " not found.");
                    oldRole = rs.getString("role");
                    oldStatus = rs.getString("status");
                }
            }
            List<String> changes = new ArrayList<>();
            if (!oldRole.equals(newRole)) {
                changes.add("changed role of User " + userId + " from " + oldRole + " to " + newRole);
            }
            if (!oldStatus.equals(newStatus)) {
                changes.add("changed status of User " + userId + " from " + oldStatus + " to " + newStatus);
            }
            if (changes.isEmpty()) return false;

            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE users SET role = ?, status = ? WHERE user_id = ?")) {
                ps.setString(1, newRole);
                ps.setString(2, newStatus);
                ps.setInt(3, userId);
                ps.executeUpdate();
            }
            logDao.logAction(c, adminId, "USER_UPDATED",
                    "Admin " + adminId + " " + String.join("; ", changes));
            return true;
        });
    }

    // ----------------------------------------------------------------- helpers

    private List<User> select(String sql, Integer param) {
        List<User> users = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (param != null) ps.setInt(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) users.add(map(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not read users: " + e.getMessage(), e);
        }
        return users;
    }

    private User map(ResultSet rs) throws SQLException {
        return new User(rs.getInt("user_id"), rs.getString("username"), rs.getString("full_name"),
                rs.getString("contact_no"), rs.getString("email"), rs.getString("role"), rs.getString("status"));
    }
}
