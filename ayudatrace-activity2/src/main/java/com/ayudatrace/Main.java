package com.ayudatrace;

import com.ayudatrace.dao.DatabaseInitializer;
import com.ayudatrace.dao.UserDao;
import com.ayudatrace.ui.Authentication;

import java.util.Scanner;

/**
 * Single entry point of AyudaTrace.
 * Flow: Initialize Database -> Instantiate DAOs & Utilities -> Authentication.start()
 */
public class Main {
    public static void main(String[] args) {
        try {
            DatabaseInitializer.initialize();
        } catch (IllegalStateException e) {
            System.err.println("Startup failed: " + e.getMessage());
            return;
        }

        UserDao userDao = new UserDao();

        try (Scanner scanner = new Scanner(System.in)) {
            new Authentication(userDao, scanner).start();
        }
    }
}
