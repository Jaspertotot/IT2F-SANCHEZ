package com.ayudatrace.ui;

import com.ayudatrace.dao.AidProgramDao;
import com.ayudatrace.dao.DataAccessException;
import com.ayudatrace.dao.DistributionDao;
import com.ayudatrace.dao.HouseholdDao;
import com.ayudatrace.model.Household;
import com.ayudatrace.model.User;

import java.util.List;

/** Household Representative: read-only view of own household, available aid and claim history. */
public class HouseholdMenu {
    private final User user;
    private final HouseholdDao householdDao = new HouseholdDao();
    private final AidProgramDao programDao = new AidProgramDao();
    private final DistributionDao distributionDao = new DistributionDao();

    public HouseholdMenu(User user) {
        this.user = user;
    }

    public void run() {
        while (true) {
            System.out.println("\n===== HOUSEHOLD MENU (" + user.fullName() + ") =====");
            System.out.println("1. View My Household");
            System.out.println("2. View Available Aid Programs");
            System.out.println("3. My Claim History");
            System.out.println("0. Logout");
            int choice = ConsoleInput.readIntInRange("Choice: ", 0, 3);
            try {
                switch (choice) {
                    case 1 -> withHousehold(h -> ConsolePrinter.households(List.of(h)));
                    case 2 -> ConsolePrinter.programs(programDao.getOngoing());
                    case 3 -> withHousehold(h -> {
                        var claims = distributionDao.getClaimsByHousehold(h.householdId());
                        if (claims.isEmpty()) System.out.println("No claims yet.");
                        else ConsolePrinter.claims(claims);
                    });
                    default -> { return; }
                }
            } catch (DataAccessException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private void withHousehold(java.util.function.Consumer<Household> action) {
        Household h = householdDao.findByUserId(user.userId());
        if (h == null) {
            System.out.println("No household is linked to your account yet. Please ask barangay staff to link it.");
            return;
        }
        action.accept(h);
    }
}
