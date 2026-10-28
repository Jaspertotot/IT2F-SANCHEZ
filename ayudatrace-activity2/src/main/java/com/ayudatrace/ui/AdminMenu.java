package com.ayudatrace.ui;

import com.ayudatrace.dao.UserDao;
import com.ayudatrace.model.User;

import java.util.List;
import java.util.Scanner;

/** Administrator menu. No SQL here; all data access goes through UserDao. */
public class AdminMenu {

    private final UserDao userDao;
    private final Scanner scanner;

    public AdminMenu(UserDao userDao, Scanner scanner) {
        this.userDao = userDao;
        this.scanner = scanner;
    }

    public void show(User currentAdmin) {
        boolean running = true;
        while (running) {
            System.out.println("\n===== ADMIN MENU =====");
            System.out.println("1. View all system users");
            System.out.println("2. View pending registrations");
            System.out.println("3. Approve user account");
            System.out.println("0. Logout");

            int choice = readIntInput("Choose an option: ");
            switch (choice) {
                case 1 -> printUserTable(userDao.getAllUsers(), "ALL SYSTEM USERS");
                case 2 -> printUserTable(userDao.getPendingUsers(), "PENDING REGISTRATIONS");
                case 3 -> approveUser();
                case 0 -> {
                    System.out.println("Logged out.");
                    running = false;
                }
                default -> System.out.println("Invalid option. Choose 0-3.");
            }
        }
    }

    private void approveUser() {
        List<User> pending = userDao.getPendingUsers();
        printUserTable(pending, "PENDING REGISTRATIONS");
        if (pending.isEmpty()) {
            return;
        }

        while (true) {
            int targetId = readIntInput("Enter the User ID to approve (0 to cancel): ");
            if (targetId == 0) {
                System.out.println("Approval cancelled.");
                return;
            }
            boolean isPending = pending.stream().anyMatch(u -> u.id() == targetId);
            if (!isPending) {
                System.out.println("User ID " + targetId + " is not in the pending list. Try again.");
                continue;
            }
            if (userDao.approveUser(targetId)) {
                System.out.println("SUCCESS: User ID " + targetId + " has been APPROVED.");
            } else {
                System.out.println("Approval failed. Please try again.");
            }
            return;
        }
    }

    // ---------- Input validation ----------

    /** Keeps asking until the user types a whole number. */
    private int readIntInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }
    }

    // ---------- Formatted table output ----------

    private void printUserTable(List<User> users, String title) {
        String border = "+-------+-----------------+-----------------+------------+";
        String rowFormat = "| %-5s | %-15s | %-15s | %-10s |%n";

        System.out.println("\n" + title);
        System.out.println(border);
        System.out.printf(rowFormat, "ID", "USERNAME", "ROLE", "STATUS");
        System.out.println(border);
        if (users.isEmpty()) {
            System.out.printf("| %-54s |%n", "No records found.");
        } else {
            for (User u : users) {
                System.out.printf(rowFormat, u.id(), truncate(u.username(), 15), u.role(), u.status());
            }
        }
        System.out.println(border);
    }

    private String truncate(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 3) + "...";
    }
}
