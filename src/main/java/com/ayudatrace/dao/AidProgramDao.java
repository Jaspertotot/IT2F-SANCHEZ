package com.ayudatrace.dao;

import com.ayudatrace.model.AidProgram;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class AidProgramDao {

    private static final String COLS =
            "program_id, program_name, aid_type, items_or_amount, target_purok, program_date, created_by, status";
    private final ActivityLogDao logDao = new ActivityLogDao();

    /** Creates a program (status PLANNED) and logs PROGRAM_CREATED. */
    public int createProgram(int actorId, String name, String aidType, String itemsOrAmount,
                             String targetPurok, String programDate) {
        return DatabaseConnection.inTransaction(c -> {
            String sql = "INSERT INTO aid_programs (program_name, aid_type, items_or_amount, target_purok, "
                    + "program_date, created_by) VALUES (?,?,?,?,?,?)";
            int id;
            try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, name);
                ps.setString(2, aidType);
                ps.setString(3, itemsOrAmount);
                ps.setString(4, targetPurok);
                ps.setString(5, programDate);
                ps.setInt(6, actorId);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    id = keys.getInt(1);
                }
            }
            logDao.logAction(c, actorId, "PROGRAM_CREATED",
                    "User " + actorId + " created Program #" + id + " '" + name + "' (" + aidType + ")");
            return id;
        });
    }

    /** Lifecycle change (e.g. PLANNED -> ONGOING). Logs PROGRAM_UPDATED. Returns false if unchanged. */
    public boolean updateStatus(int actorId, int programId, String newStatus) {
        if (!AidProgram.STATUSES.contains(newStatus)) {
            throw new IllegalArgumentException("Invalid program status.");
        }
        return DatabaseConnection.inTransaction(c -> {
            String oldStatus;
            try (PreparedStatement ps = c.prepareStatement("SELECT status FROM aid_programs WHERE program_id = ?")) {
                ps.setInt(1, programId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new IllegalArgumentException("Program #" + programId + " not found.");
                    oldStatus = rs.getString(1);
                }
            }
            if (oldStatus.equals(newStatus)) return false;
            try (PreparedStatement ps = c.prepareStatement("UPDATE aid_programs SET status = ? WHERE program_id = ?")) {
                ps.setString(1, newStatus);
                ps.setInt(2, programId);
                ps.executeUpdate();
            }
            logDao.logAction(c, actorId, "PROGRAM_UPDATED",
                    "Updated Program #" + programId + " status from " + oldStatus + " to " + newStatus);
            return true;
        });
    }

    public AidProgram findById(int programId) {
        List<AidProgram> list = select("SELECT " + COLS + " FROM aid_programs WHERE program_id = ?", programId);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<AidProgram> getAll() {
        return select("SELECT " + COLS + " FROM aid_programs ORDER BY program_id", null);
    }

    /** Programs households can currently claim from. */
    public List<AidProgram> getOngoing() {
        return select("SELECT " + COLS + " FROM aid_programs WHERE status = 'ONGOING' ORDER BY program_id", null);
    }

    private List<AidProgram> select(String sql, Integer param) {
        List<AidProgram> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (param != null) ps.setInt(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new AidProgram(rs.getInt("program_id"), rs.getString("program_name"),
                            rs.getString("aid_type"), rs.getString("items_or_amount"),
                            rs.getString("target_purok"), rs.getString("program_date"),
                            rs.getInt("created_by"), rs.getString("status")));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not read aid programs: " + e.getMessage(), e);
        }
        return list;
    }
}
