package com.ayudatrace.ui;

import com.ayudatrace.model.AidProgram;
import com.ayudatrace.model.Distribution;
import com.ayudatrace.model.Household;
import com.ayudatrace.model.User;

import java.util.List;

/** Formatted console tables (presentation only, no SQL). */
public final class ConsolePrinter {
    private ConsolePrinter() {}

    public static void users(List<User> users) {
        String f = "%-5s | %-14s | %-22s | %-26s | %-10s%n";
        System.out.printf(f, "ID", "Username", "Full Name", "Role", "Status");
        System.out.println("-".repeat(88));
        for (User u : users) System.out.printf(f, u.userId(), cut(u.username(), 14), cut(u.fullName(), 22), u.role(), u.status());
    }

    public static void households(List<Household> list) {
        String f = "%-5s | %-20s | %-14s | %-7s | %-18s | %-8s | %-8s%n";
        System.out.printf(f, "ID", "Head of Family", "Purok", "Members", "Category", "User ID", "Status");
        System.out.println("-".repeat(98));
        for (Household h : list) {
            System.out.printf(f, h.householdId(), cut(h.headName(), 20), cut(h.purokAddress(), 14),
                    h.memberCount(), cut(h.category(), 18), h.userId() == null ? "-" : h.userId(), h.status());
        }
    }

    public static void programs(List<AidProgram> list) {
        String f = "%-5s | %-24s | %-12s | %-20s | %-10s | %-10s%n";
        System.out.printf(f, "ID", "Program", "Aid Type", "Items / Amount", "Date", "Status");
        System.out.println("-".repeat(94));
        for (AidProgram p : list) {
            System.out.printf(f, p.programId(), cut(p.programName(), 24), p.aidType(),
                    cut(p.itemsOrAmount(), 20), p.programDate(), p.status());
        }
    }

    public static void claims(List<Distribution> list) {
        String f = "%-5s | %-20s | %-18s | %-18s | %-16s | %-8s%n";
        System.out.printf(f, "ID", "Program", "Household Head", "Claimant", "Received", "Status");
        System.out.println("-".repeat(98));
        for (Distribution d : list) {
            System.out.printf(f, d.distributionId(), cut(d.programName(), 20), cut(d.headName(), 18),
                    cut(d.claimantName(), 18), cut(d.itemsOrAmountReceived(), 16), d.status());
        }
    }

    private static String cut(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }
}
