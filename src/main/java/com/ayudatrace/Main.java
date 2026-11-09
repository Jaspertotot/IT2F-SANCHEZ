package com.ayudatrace;

import com.ayudatrace.dao.DataAccessException;
import com.ayudatrace.dao.DatabaseInitializer;
import com.ayudatrace.model.User;
import com.ayudatrace.ui.AdminMenu;
import com.ayudatrace.ui.Authentication;
import com.ayudatrace.ui.ConsoleInput;
import com.ayudatrace.ui.HouseholdMenu;
import com.ayudatrace.ui.StaffMenu;

import java.util.NoSuchElementException;

/** Single entry point for AyudaTrace. */
public class Main {

    public static void main(String[] args) {
        try {
            DatabaseInitializer.initialize();
        } catch (DataAccessException e) {
            System.out.println(e.getMessage());
            return;
        }

        Authentication auth = new Authentication();
        System.out.println("=== AyudaTrace: Relief Goods and Cash Aid Distribution Tracker ===");
        try {
            while (true) {
                System.out.println("\n1. Login");
                System.out.println("2. Register");
                System.out.println("0. Exit");
                int choice = ConsoleInput.readIntInRange("Choice: ", 0, 2);
                try {
                    switch (choice) {
                        case 1 -> {
                            User user = auth.login();
                            if (user != null) openMenu(user, auth);
                        }
                        case 2 -> auth.register();
                        default -> {
                            System.out.println("Goodbye!");
                            return;
                        }
                    }
                } catch (DataAccessException e) {
                    System.out.println("Error: " + e.getMessage());
                }
            }
        } catch (NoSuchElementException e) {
            System.out.println("\nInput closed. Goodbye!");
        }
    }

    private static void openMenu(User user, Authentication auth) {
        switch (user.role()) {
            case User.ROLE_ADMIN -> new AdminMenu(user).run();
            case User.ROLE_STAFF -> new StaffMenu(user).run();
            default -> new HouseholdMenu(user).run();
        }
        auth.logout(user);
    }
}
