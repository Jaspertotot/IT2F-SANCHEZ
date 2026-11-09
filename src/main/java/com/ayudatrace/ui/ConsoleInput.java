package com.ayudatrace.ui;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

/** Safe console input: bad input re-prompts instead of crashing. */
public final class ConsoleInput {
    private static final Scanner SC = new Scanner(System.in);

    private ConsoleInput() {}

    public static String readLine(String prompt) {
        System.out.print(prompt);
        if (!SC.hasNextLine()) throw new NoSuchElementException("Input closed");
        return SC.nextLine().trim();
    }

    public static String readRequired(String prompt) {
        while (true) {
            String s = readLine(prompt);
            if (!s.isEmpty()) return s;
            System.out.println("  This field cannot be empty.");
        }
    }

    public static int readInt(String prompt) {
        while (true) {
            String s = readLine(prompt);
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                System.out.println("  Invalid input. Please enter a whole number.");
            }
        }
    }

    public static int readIntInRange(String prompt, int min, int max) {
        while (true) {
            int n = readInt(prompt);
            if (n >= min && n <= max) return n;
            System.out.println("  Please enter a number from " + min + " to " + max + ".");
        }
    }

    public static int readPositiveInt(String prompt) {
        while (true) {
            int n = readInt(prompt);
            if (n > 0) return n;
            System.out.println("  Please enter a number greater than 0.");
        }
    }

    public static String readDate(String prompt) {
        while (true) {
            String s = readLine(prompt);
            try {
                return LocalDate.parse(s).toString();
            } catch (DateTimeParseException e) {
                System.out.println("  Invalid date. Use the format YYYY-MM-DD (e.g. 2026-11-15).");
            }
        }
    }

    public static boolean confirm(String prompt) {
        while (true) {
            String s = readLine(prompt + " (y/n): ").toLowerCase();
            if (s.equals("y") || s.equals("yes")) return true;
            if (s.equals("n") || s.equals("no")) return false;
            System.out.println("  Please answer y or n.");
        }
    }

    /** Shows a numbered list and returns the chosen item. */
    public static String choose(String title, List<String> options) {
        System.out.println(title);
        for (int i = 0; i < options.size(); i++) {
            System.out.println("  " + (i + 1) + ". " + options.get(i));
        }
        return options.get(readIntInRange("Choice: ", 1, options.size()) - 1);
    }
}
