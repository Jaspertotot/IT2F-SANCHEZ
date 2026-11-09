package com.ayudatrace.dao;

import com.ayudatrace.model.Distribution;
import com.ayudatrace.model.Household;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DistributionDao {

    /** Outcome of a claim operation, with a message the UI can show. */
    public record ClaimResult(boolean success, String message) { }

    private static final String SELECT_CLAIMS =
            "SELECT d.distribution_id, d.program_id, p.program_name, d.household_id, h.head_name, "
                    + "d.claimant_name, d.items_or_amount_received, d.claim_date, d.released_by, d.status "
                    + "FROM distributions d "
                    + "JOIN aid_programs p ON p.program_id = d.program_id "
                    + "JOIN households h ON h.household_id = d.household_id ";

    private final ActivityLogDao logDao = new ActivityLogDao();

    /**
     * CORE BUSINESS PROCESS: records an aid claim, blocking double claims.
     * Logs CLAIM_RECORDED on success, or CLAIM_BLOCKED when a duplicate is attempted.
     */
    public ClaimResult recordClaim(int staffId, int programId, int householdId,
                                   String claimantName, String itemsOrAmount) {
        return DatabaseConnection.inTransaction(c -> {
            String programStatus = scalar(c, "SELECT status FROM aid_programs WHERE program_id = ?", programId);
            if (programStatus == null) return new ClaimResult(false, "Program #" + programId + " not found.");
            if (!"ONGOING".equals(programStatus)) {
                return new ClaimResult(false, "Program #" + programId + " is " + programStatus
                        + ". Claims are only allowed while it is ONGOING.");
            }
            String householdStatus = scalar(c, "SELECT status FROM households WHERE household_id = ?", householdId);
            if (householdStatus == null) return new ClaimResult(false, "Household #" + householdId + " not found.");
            if (!"ACTIVE".equals(householdStatus)) {
                return new ClaimResult(false, "Household #" + householdId + " is not ACTIVE.");
            }

            // Double-claim check
            String existing = scalar(c, "SELECT distribution_id FROM distributions "
                    + "WHERE program_id = ? AND household_id = ? AND status = 'CLAIMED'", programId, householdId);
            if (existing != null) {
                logDao.logAction(c, staffId, "CLAIM_BLOCKED",
                        "Blocked duplicate claim: Household #" + householdId + " already claimed Program #"
                                + programId + " (Claim #" + existing + ")");
                return new ClaimResult(false, "DUPLICATE CLAIM BLOCKED: Household #" + householdId
                        + " already claimed from Program #" + programId + " (Claim #" + existing + ").");
            }

            int id;
            String sql = "INSERT INTO distributions (program_id, household_id, claimant_name, "
                    + "items_or_amount_received, released_by) VALUES (?,?,?,?,?)";
            try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, programId);
                ps.setInt(2, householdId);
                ps.setString(3, claimantName);
                ps.setString(4, itemsOrAmount);
                ps.setInt(5, staffId);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    id = keys.getInt(1);
                }
            }
            logDao.logAction(c, staffId, "CLAIM_RECORDED",
                    "Recorded Claim #" + id + ": Household #" + householdId + " received " + itemsOrAmount
                            + " from Program #" + programId);
            return new ClaimResult(true, "Claim #" + id + " recorded successfully.");
        });
    }

    /** LIFECYCLE CHANGE: voids an erroneous claim (kept in the table). Logs CLAIM_VOIDED. */
    public ClaimResult voidClaim(int staffId, int distributionId, String reason) {
        return DatabaseConnection.inTransaction(c -> {
            String status = scalar(c, "SELECT status FROM distributions WHERE distribution_id = ?", distributionId);
            if (status == null) return new ClaimResult(false, "Claim #" + distributionId + " not found.");
            if (!"CLAIMED".equals(status)) {
                return new ClaimResult(false, "Claim #" + distributionId + " is already " + status + ".");
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE distributions SET status = 'VOIDED' WHERE distribution_id = ?")) {
                ps.setInt(1, distributionId);
                ps.executeUpdate();
            }
            logDao.logAction(c, staffId, "CLAIM_VOIDED",
                    "Updated Claim #" + distributionId + " status from CLAIMED to VOIDED. Reason: " + reason);
            return new ClaimResult(true, "Claim #" + distributionId + " voided.");
        });
    }

    public List<Distribution> getAllClaims() {
        return selectClaims(SELECT_CLAIMS + "ORDER BY d.distribution_id", null);
    }

    public List<Distribution> getClaimsByHousehold(int householdId) {
        return selectClaims(SELECT_CLAIMS + "WHERE d.household_id = ? ORDER BY d.distribution_id", householdId);
    }

    /** Active households that have no CLAIMED entry for the given program. */
    public List<Household> getUnservedHouseholds(int programId) {
        String sql = "SELECT household_id, head_name, purok_address, contact_no, member_count, category, user_id, status "
                + "FROM households h WHERE h.status = 'ACTIVE' AND NOT EXISTS ("
                + "SELECT 1 FROM distributions d WHERE d.household_id = h.household_id "
                + "AND d.program_id = ? AND d.status = 'CLAIMED') ORDER BY household_id";
        List<Household> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, programId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(HouseholdDao.map(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not read unserved households: " + e.getMessage(), e);
        }
        return list;
    }

    // ----------------------------------------------------------------- helpers

    /** Returns the first column of the first row as text, or null when no row. */
    private static String scalar(Connection c, String sql, int... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setInt(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    private List<Distribution> selectClaims(String sql, Integer param) {
        List<Distribution> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (param != null) ps.setInt(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Distribution(rs.getInt("distribution_id"), rs.getInt("program_id"),
                            rs.getString("program_name"), rs.getInt("household_id"), rs.getString("head_name"),
                            rs.getString("claimant_name"), rs.getString("items_or_amount_received"),
                            rs.getString("claim_date"), rs.getInt("released_by"), rs.getString("status")));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not read claims: " + e.getMessage(), e);
        }
        return list;
    }
}
