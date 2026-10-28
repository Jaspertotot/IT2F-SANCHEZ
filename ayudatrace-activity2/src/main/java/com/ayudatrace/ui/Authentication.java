package com.ayudatrace.ui;

import com.ayudatrace.dao.UserDao;
import com.ayudatrace.model.Roles;
import com.ayudatrace.model.Statuses;
import com.ayudatrace.model.User;

import java.util.Optional;
import java.util.Scanner;

/** Handles login, registration, and role-based routing. No SQL here. */
public class Authentication {

    private final UserDao userDao;
    private final Scanner scanner;
    private final AdminMenu adminMenu;
    private final StaffMenu staffMenu;
    private final HouseholdMenu householdMenu;

    public Authentication(UserDao userDao, Scanner scanner) {
        this.userDao = userDao;
        this.scanner = scanner;
        // The same UserDao and Scanner are shared with every menu.
        this.adminMenu = new AdminMenu(userDao, scanner);
        this.staffMenu = new StaffMenu(userDao, scanner);
        this.householdMenu = new HouseholdMenu(userDao, scanner);
    }

    public void start() {
        boolean running = true;
        while (running) {
            System.out.println("\n===== AYUDATRACE =====");
            System.out.println("1. Login");
            System.out.println("2. Register");
            System.out.println("3. Exit");
            System.out.print("Choose an option: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> login();
                case "2" -> register();
                case "3" -> {
                    System.out.println("Goodbye!");
                    running = false;
                }
                default -> System.out.println("Invalid option. Enter 1, 2, or 3.");
            }
        }
    }

    private void login() {
        System.out.println("\n--- LOGIN ---");
        System.out.print("Username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Password: ");
        String password = scanner.nextLine();

        Optional<User> result = userDao.findByCredentials(username, password);
        if (result.isEmpty()) {
            System.out.println("Invalid username or password.");
            return;
        }

        User user = result.get();
        if (Statuses.PENDING.equals(user.status())) {
            System.out.println("Your account is still PENDING. Please wait for an administrator to approve it.");
        } else if (!Statuses.APPROVED.equals(user.status())) {
            System.out.println("Your account is not active. Please contact an administrator.");
        } else {
            System.out.println("Welcome, " + user.fullName() + "! (" + user.role() + ")");
            routeByRole(user);
        }
    }

    private void routeByRole(User user) {
        switch (user.role()) {
            case Roles.ADMIN -> adminMenu.show(user);
            case Roles.STAFF -> staffMenu.show(user);
            case Roles.HOUSEHOLD_REP -> householdMenu.show(user);
            default -> System.out.println("Unknown role: " + user.role());
        }
    }

    private void register() {
        System.out.println("\n--- REGISTER ---");

        String fullName = readRequired("Full name: ");

        String username;
        while (true) {
            username = readRequired("Username (3-20 characters, no spaces): ");
            if (username.length() < 3 || username.length() > 20 || username.contains(" ")) {
                System.out.println("Username must be 3-20 characters with no spaces.");
            } else if (userDao.usernameExists(username)) {
                System.out.println("That username is already taken.");
            } else {
                break;
            }
        }

        String password;
        while (true) {
            password = readRequired("Password (at least 6 characters): ");
            if (password.length() >= 6) {
                break;
            }
            System.out.println("Password is too short.");
        }

        System.out.print("Contact number (optional): ");
        String contactNo = scanner.nextLine().trim();
        System.out.print("Email (optional): ");
        String email = scanner.nextLine().trim();

        String role = chooseRole();

        if (userDao.registerUser(username, password, fullName, contactNo, email, role)) {
            System.out.println("Registration successful! Status: PENDING. "
                    + "An administrator must approve your account before you can log in.");
        } else {
            System.out.println("Registration failed. Please try again.");
        }
    }

    private String chooseRole() {
        while (true) {
            System.out.println("Select your role:");
            System.out.println("  1. Administrator");
            System.out.println("  2. Barangay Staff / Distribution Officer");
            System.out.println("  3. Household Representative");
            System.out.print("Choice: ");
            switch (scanner.nextLine().trim()) {
                case "1": return Roles.ADMIN;
                case "2": return Roles.STAFF;
                case "3": return Roles.HOUSEHOLD_REP;
                default: System.out.println("Invalid choice. Enter 1, 2, or 3.");
            }
        }
    }

    private String readRequired(String prompt) {
        while (true) {
            System.out.print(prompt);
            String value = scanner.nextLine().trim();
            if (!value.isEmpty()) {
                return value;
            }
            System.out.println("This field is required.");
        }
    }
}
