package com.ayudatrace.ui;

import com.ayudatrace.dao.ActivityLogDao;
import com.ayudatrace.dao.UserDao;
import com.ayudatrace.model.User;

import java.util.List;

/** Login / logout / registration screens. Login and logout are written to the audit log. */
public class Authentication {
    private final UserDao userDao = new UserDao();
    private final ActivityLogDao logDao = new ActivityLogDao();

    /** Returns the logged-in user, or null when login fails. Logs USER_LOGIN on success. */
    public User login() {
        System.out.println("\n--- Login ---");
        String username = ConsoleInput.readRequired("Username: ");
        String password = ConsoleInput.readRequired("Password: ");

        User user = userDao.authenticate(username, password);
        if (user == null) {
            System.out.println("Invalid username or password.");
            return null;
        }
        if (User.STATUS_PENDING.equals(user.status())) {
            System.out.println("Your account is still waiting for administrator approval.");
            return null;
        }
        if (User.STATUS_INACTIVE.equals(user.status())) {
            System.out.println("Your account is inactive. Please contact the administrator.");
            return null;
        }
        logDao.logAction(user.userId(), "USER_LOGIN",
                "User " + user.username() + " (ID: " + user.userId() + ") logged in.");
        System.out.println("Welcome, " + user.fullName() + " (" + user.role() + ")!");
        return user;
    }

    /** Logs USER_LOGOUT. */
    public void logout(User user) {
        logDao.logAction(user.userId(), "USER_LOGOUT",
                "User " + user.username() + " (ID: " + user.userId() + ") logged out.");
        System.out.println("You have been logged out.");
    }

    /** Self-registration. New accounts are PENDING until an admin approves them. */
    public void register() {
        System.out.println("\n--- Register ---");
        String role = ConsoleInput.choose("Register as:", List.of(User.ROLE_STAFF, User.ROLE_HOUSEHOLD));

        String username;
        while (true) {
            username = ConsoleInput.readRequired("Username: ");
            if (!userDao.usernameExists(username)) break;
            System.out.println("  That username is already taken.");
        }
        String password;
        while (true) {
            password = ConsoleInput.readRequired("Password (min 6 characters): ");
            if (password.length() >= 6) break;
            System.out.println("  Password is too short.");
        }
        String fullName = ConsoleInput.readRequired("Full name: ");
        String contact = ConsoleInput.readLine("Contact no. (optional): ");
        String email = ConsoleInput.readLine("Email (optional): ");

        int id = userDao.registerUser(username, password, fullName, contact, email, role);
        System.out.println("Registered! Your User ID is " + id + ". Please wait for administrator approval.");
    }
}
