package com.ayudatrace.dao;

import com.ayudatrace.model.Household;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class HouseholdDao {

    private static final String COLS =
            "household_id, head_name, purok_address, contact_no, member_count, category, user_id, status";
    private final ActivityLogDao logDao = new ActivityLogDao();

    /** Registers a household and logs HOUSEHOLD_ADDED. linkedUserId may be null. */
    public int createHousehold(int actorId, String headName, String purok, String contactNo,
                               int memberCount, String category, Integer linkedUserId) {
        return DatabaseConnection.inTransaction(c -> {
            String sql = "INSERT INTO households (head_name, purok_address, contact_no, member_count, category, user_id) "
                    + "VALUES (?,?,?,?,?,?)";
            int id;
            try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, headName);
                ps.setString(2, purok);
                ps.setString(3, contactNo);
                ps.setInt(4, memberCount);
                ps.setString(5, category);
                if (linkedUserId == null) ps.setNull(6, Types.INTEGER);
                else ps.setInt(6, linkedUserId);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    id = keys.getInt(1);
                }
            }
            logDao.logAction(c, actorId, "HOUSEHOLD_ADDED",
                    "User " + actorId + " registered Household #" + id + " (head: " + headName
                            + ", " + purok + ", " + category + ")");
            return id;
        });
    }

    public Household findById(int householdId) {
        List<Household> list = select("SELECT " + COLS + " FROM households WHERE household_id = ?",
                ps -> ps.setInt(1, householdId));
        return list.isEmpty() ? null : list.get(0);
    }

    public Household findByUserId(int userId) {
        List<Household> list = select("SELECT " + COLS + " FROM households WHERE user_id = ?",
                ps -> ps.setInt(1, userId));
        return list.isEmpty() ? null : list.get(0);
    }

    public List<Household> getAll() {
        return select("SELECT " + COLS + " FROM households ORDER BY household_id", ps -> { });
    }

    /** Search by head name, purok, or category. */
    public List<Household> search(String keyword) {
        String like = "%" + keyword + "%";
        return select("SELECT " + COLS + " FROM households WHERE head_name LIKE ? OR purok_address LIKE ? "
                + "OR category LIKE ? ORDER BY household_id", ps -> {
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
        });
    }

    @FunctionalInterface
    private interface Binder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    private List<Household> select(String sql, Binder binder) {
        List<Household> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not read households: " + e.getMessage(), e);
        }
        return list;
    }

    static Household map(ResultSet rs) throws SQLException {
        int uid = rs.getInt("user_id");
        Integer userId = rs.wasNull() ? null : uid;
        return new Household(rs.getInt("household_id"), rs.getString("head_name"),
                rs.getString("purok_address"), rs.getString("contact_no"), rs.getInt("member_count"),
                rs.getString("category"), userId, rs.getString("status"));
    }
}
