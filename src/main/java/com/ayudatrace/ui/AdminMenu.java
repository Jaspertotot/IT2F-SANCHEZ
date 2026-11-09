package com.ayudatrace.ui;

import com.ayudatrace.dao.ActivityLogDao;
import com.ayudatrace.dao.DataAccessException;
import com.ayudatrace.dao.UserDao;
import com.ayudatrace.model.ActivityLog;
import com.ayudatrace.model.User;

import java.util.ArrayList;
import java.util.List;

public class AdminMenu {
    /** Required audit table layout: ID | User ID | Action Type | Description | Timestamp */
    private static final String LOG_FORMAT = "%-5s | %-8s | %-18s | %-35s | %-20s%n";
    private static final int DESC_WIDTH = 35;

    private final User admin;
    private final UserDao userDao = new UserDao();
    private final ActivityLogDao logDao = new ActivityLogDao();

    public AdminMenu(User admin) {
        this.admin = admin;
    }

    public void run() {
        while (true) {
            System.out.println("\n===== ADMINISTRATOR MENU (" + admin.fullName() + ") =====");
            System.out.println("1. View Pending Accounts / Approve");
            System.out.println("2. Update User Role / Status");
            System.out.println("3. View All Users");
            System.out.println("4. Aid Operations (households, programs, claims)");
            System.out.println("5. View System Audit Logs");
            System.out.println("0. Logout");
            int choice = ConsoleInput.readIntInRange("Choice: ", 0, 5);
            try {
                switch (choice) {
                    case 1 -> approvePending();
                    case 2 -> updateUser();
                    case 3 -> ConsolePrinter.users(userDao.getAllUsers());
                    case 4 -> new StaffMenu(admin).run();
                    case 5 -> auditLogMenu();
                    default -> { return; }
                }
            } catch (DataAccessException | IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    // ------------------------------------------------------------ account tools

    private void approvePending() {
        List<User> pending = userDao.getPendingUsers();
        if (pending.isEmpty()) {
            System.out.println("No pending accounts.");
            return;
        }
        ConsolePrinter.users(pending);
        while (true) {
            int id = ConsoleInput.readInt("Enter User ID to approve (0 to cancel): ");
            if (id == 0) return;
            if (pending.stream().noneMatch(u -> u.userId() == id)) {
                System.out.println("  That ID is not in the pending list.");
                continue;
            }
            if (userDao.approveUser(admin.userId(), id)) {
                System.out.println("User " + id + " approved.");
            } else {
                System.out.println("Could not approve User " + id + " (no longer pending).");
            }
            return;
        }
    }

    private void updateUser() {
        ConsolePrinter.users(userDao.getAllUsers());
        int id = ConsoleInput.readInt("Enter User ID to edit (0 to cancel): ");
        if (id == 0) return;
        if (id == admin.userId()) {
            System.out.println("You cannot edit your own account.");
            return;
        }
        User target = userDao.findById(id);
        if (target == null) {
            System.out.println("User ID " + id + " not found.");
            return;
        }
        System.out.println("Current role: " + target.role() + " | Current status: " + target.status());

        System.out.println("New role (0 = keep current):");
        for (int i = 0; i < User.ROLES.size(); i++) System.out.println("  " + (i + 1) + ". " + User.ROLES.get(i));
        int r = ConsoleInput.readIntInRange("Choice: ", 0, User.ROLES.size());
        String newRole = r == 0 ? target.role() : User.ROLES.get(r - 1);

        System.out.println("New status (0 = keep current):");
        for (int i = 0; i < User.STATUSES.size(); i++) System.out.println("  " + (i + 1) + ". " + User.STATUSES.get(i));
        int s = ConsoleInput.readIntInRange("Choice: ", 0, User.STATUSES.size());
        String newStatus = s == 0 ? target.status() : User.STATUSES.get(s - 1);

        if (!ConsoleInput.confirm("Set User " + id + " to " + newRole + " / " + newStatus + "?")) {
            System.out.println("Cancelled.");
            return;
        }
        System.out.println(userDao.updateUserRoleAndStatus(admin.userId(), id, newRole, newStatus)
                ? "User updated." : "No changes were made.");
    }

    // --------------------------------------------------------------- audit logs

    private void auditLogMenu() {
        while (true) {
            System.out.println("\n--- System Audit Logs ---");
            System.out.println("1. View All System Logs");
            System.out.println("2. Filter Logs by User ID");
            System.out.println("3. Clear All Logs");
            System.out.println("0. Back");
            int choice = ConsoleInput.readIntInRange("Choice: ", 0, 3);
            switch (choice) {
                case 1 -> printLogs(logDao.getAllLogs());
                case 2 -> {
                    int userId = ConsoleInput.readInt("Enter User ID: ");
                    List<ActivityLog> logs = logDao.getLogsByUserId(userId);
                    if (logs.isEmpty()) System.out.println("No logs found for User ID " + userId + ".");
                    else printLogs(logs);
                }
                case 3 -> clearLogs();
                default -> { return; }
            }
        }
    }

    private void clearLogs() {
        System.out.println("WARNING: this permanently deletes ALL audit log history.");
        String answer = ConsoleInput.readLine("Type CLEAR to confirm (anything else cancels): ");
        if (!answer.equals("CLEAR")) {
            System.out.println("Cancelled. No logs were deleted.");
            return;
        }
        int deleted = logDao.clearAllLogs(admin.userId());
        System.out.println(deleted + " log entries deleted. This action was recorded as LOGS_CLEARED.");
    }

    private void printLogs(List<ActivityLog> logs) {
        System.out.println();
        System.out.printf(LOG_FORMAT, "ID", "User ID", "Action Type", "Description", "Timestamp");
        System.out.println("-".repeat(5 + 3 + 8 + 3 + 18 + 3 + DESC_WIDTH + 3 + 20));
        for (ActivityLog log : logs) {
            List<String> lines = wrap(log.description(), DESC_WIDTH);
            for (int i = 0; i < lines.size(); i++) {
                boolean first = i == 0;
                System.out.printf(LOG_FORMAT,
                        first ? log.id() : "",
                        first ? log.userId() : "",
                        first ? log.actionType() : "",
                        lines.get(i),
                        first ? log.timestamp() : "");
            }
        }
        System.out.println("Total entries: " + logs.size());
    }

    /** Word-wraps long descriptions so the table columns stay aligned and nothing is cut off. */
    private static List<String> wrap(String text, int width) {
        List<String> lines = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String word : text.split("\\s+")) {
            while (word.length() > width) {
                if (cur.length() > 0) {
                    lines.add(cur.toString());
                    cur.setLength(0);
                }
                lines.add(word.substring(0, width));
                word = word.substring(width);
            }
            if (cur.length() == 0) cur.append(word);
            else if (cur.length() + 1 + word.length() <= width) cur.append(' ').append(word);
            else {
                lines.add(cur.toString());
                cur.setLength(0);
                cur.append(word);
            }
        }
        if (cur.length() > 0 || lines.isEmpty()) lines.add(cur.toString());
        return lines;
    }
}
