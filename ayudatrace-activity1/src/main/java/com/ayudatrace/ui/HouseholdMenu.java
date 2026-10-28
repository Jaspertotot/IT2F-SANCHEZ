package com.ayudatrace.ui;

import com.ayudatrace.dao.UserDao;
import com.ayudatrace.model.User;

import java.util.Scanner;

/** Placeholder for the Household Representative menu (built in a later deliverable). */
public class HouseholdMenu {

    private final UserDao userDao;
    private final Scanner scanner;

    public HouseholdMenu(UserDao userDao, Scanner scanner) {
        this.userDao = userDao;
        this.scanner = scanner;
    }

    public void show(User user) {
        System.out.println("\n===== HOUSEHOLD REPRESENTATIVE MENU =====");
        System.out.println("Logged in as " + user.username() + ". Features coming in a later deliverable.");
        System.out.print("Press Enter to log out...");
        scanner.nextLine();
    }
}
