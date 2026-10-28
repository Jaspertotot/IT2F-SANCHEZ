package com.ayudatrace.ui;

import com.ayudatrace.dao.UserDao;
import com.ayudatrace.model.User;

import java.util.Scanner;

/** Placeholder admin menu (full admin features are built in the next activities). */
public class AdminMenu {

    private final UserDao userDao;
    private final Scanner scanner;

    public AdminMenu(UserDao userDao, Scanner scanner) {
        this.userDao = userDao;
        this.scanner = scanner;
    }

    public void show(User currentAdmin) {
        System.out.println("\n===== ADMIN MENU =====");
        System.out.println("Logged in as " + currentAdmin.username() + ". Admin features coming in the next activity.");
        System.out.print("Press Enter to log out...");
        scanner.nextLine();
    }
}
