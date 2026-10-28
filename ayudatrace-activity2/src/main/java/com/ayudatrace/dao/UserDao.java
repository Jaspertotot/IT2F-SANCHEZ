package com.ayudatrace.dao;

import com.ayudatrace.model.User;
import com.ayudatrace.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** All user-related SQL lives here. Every query uses a parameterized PreparedStatement. */
public class UserDao {

    /** Registers a new account. New accounts always start as PENDING. */
    public boolean registerUser(String username, String plainPassword, String fullName,
                                String contactNo, String email, String role) {
        String sql = "INSERT INTO users (username, password_hash, full_name, contact_no, email, role, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, 'PENDING')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, PasswordUtil.hash(plainPassword));
            ps.setString(3, fullName);
            ps.setString(4, contactNo);
            ps.setString(5, email);
            ps.setString(6, role.toUpperCase());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            System.err.println("Could not register user: " + e.getMessage());
            return false;
        }
    }

    public boolean usernameExists(String username) {
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("Could not check username: " + e.getMessage());
            return false;
        }
    }

    /** Returns the user if the username and password match (any status). */
    public Optional<User> findByCredentials(String username, String plainPassword) {
        String sql = "SELECT user_id, username, full_name, contact_no, email, role, status "
                + "FROM users WHERE username = ? AND password_hash = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, PasswordUtil.hash(plainPassword));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            System.err.println("Could not log in: " + e.getMessage());
            return Optional.empty();
        }
    }

    /** All registered accounts regardless of role or status. */
    public List<User> getAllUsers() {
        String sql = "SELECT user_id, username, full_name, contact_no, email, role, status "
                + "FROM users ORDER BY user_id";
        return queryUsers(sql);
    }

    /** All accounts with status = 'PENDING', across every role. */
    public List<User> getPendingUsers() {
        String sql = "SELECT user_id, username, full_name, contact_no, email, role, status "
                + "FROM users WHERE status = 'PENDING' ORDER BY user_id";
        return queryUsers(sql);
    }

    public boolean approveUser(int userId) {
        String sql = "UPDATE users SET status = 'APPROVED' WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            System.err.println("Could not approve user: " + e.getMessage());
            return false;
        }
    }

    private List<User> queryUsers(String sql) {
        List<User> users = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("Could not load users: " + e.getMessage());
        }
        return users;
    }

    private User mapRow(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("user_id"),
                rs.getString("username"),
                rs.getString("full_name"),
                rs.getString("contact_no"),
                rs.getString("email"),
                rs.getString("role"),
                rs.getString("status"));
    }
}
