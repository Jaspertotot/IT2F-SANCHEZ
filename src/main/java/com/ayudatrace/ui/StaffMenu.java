package com.ayudatrace.ui;

import com.ayudatrace.dao.AidProgramDao;
import com.ayudatrace.dao.DataAccessException;
import com.ayudatrace.dao.DistributionDao;
import com.ayudatrace.dao.DistributionDao.ClaimResult;
import com.ayudatrace.dao.HouseholdDao;
import com.ayudatrace.dao.UserDao;
import com.ayudatrace.model.AidProgram;
import com.ayudatrace.model.Household;
import com.ayudatrace.model.User;

import java.util.List;

/** Barangay Staff / Distribution Officer tools (also opened from the Administrator menu). */
public class StaffMenu {
    private static final List<String> CATEGORIES =
            List.of("4Ps", "Senior", "PWD", "Calamity-Affected", "Regular");

    private final User actor;
    private final UserDao userDao = new UserDao();
    private final HouseholdDao householdDao = new HouseholdDao();
    private final AidProgramDao programDao = new AidProgramDao();
    private final DistributionDao distributionDao = new DistributionDao();

    public StaffMenu(User actor) {
        this.actor = actor;
    }

    public void run() {
        while (true) {
            System.out.println("\n===== AID OPERATIONS (" + actor.fullName() + ") =====");
            System.out.println("1. Register Household");
            System.out.println("2. Search Households");
            System.out.println("3. Create Aid Program");
            System.out.println("4. Update Aid Program Status");
            System.out.println("5. List Aid Programs");
            System.out.println("6. Record Aid Claim");
            System.out.println("7. Void Aid Claim");
            System.out.println("8. View All Claims");
            System.out.println("9. List Unserved Households for a Program");
            System.out.println("0. " + (User.ROLE_ADMIN.equals(actor.role()) ? "Back" : "Logout"));
            int choice = ConsoleInput.readIntInRange("Choice: ", 0, 9);
            try {
                switch (choice) {
                    case 1 -> registerHousehold();
                    case 2 -> searchHouseholds();
                    case 3 -> createProgram();
                    case 4 -> updateProgramStatus();
                    case 5 -> ConsolePrinter.programs(programDao.getAll());
                    case 6 -> recordClaim();
                    case 7 -> voidClaim();
                    case 8 -> ConsolePrinter.claims(distributionDao.getAllClaims());
                    case 9 -> unserved();
                    default -> { return; }
                }
            } catch (DataAccessException | IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private void registerHousehold() {
        System.out.println("\n--- Register Household ---");
        String head = ConsoleInput.readRequired("Head of family: ");
        String purok = ConsoleInput.readRequired("Purok / address: ");
        String contact = ConsoleInput.readLine("Contact no. (optional): ");
        int members = ConsoleInput.readPositiveInt("Number of members: ");
        String category = ConsoleInput.choose("Category:", CATEGORIES);

        Integer linkedUser = null;
        int uid = ConsoleInput.readInt("Linked Household Representative User ID (0 = none): ");
        if (uid != 0) {
            User u = userDao.findById(uid);
            if (u == null || !User.ROLE_HOUSEHOLD.equals(u.role())) {
                System.out.println("  User " + uid + " is not a Household Representative. Household saved without a link.");
            } else {
                linkedUser = uid;
            }
        }
        int id = householdDao.createHousehold(actor.userId(), head, purok, contact, members, category, linkedUser);
        System.out.println("Household #" + id + " registered.");
    }

    private void searchHouseholds() {
        String keyword = ConsoleInput.readLine("Search by name, purok or category (blank = all): ");
        List<Household> list = keyword.isEmpty() ? householdDao.getAll() : householdDao.search(keyword);
        if (list.isEmpty()) System.out.println("No households found.");
        else ConsolePrinter.households(list);
    }

    private void createProgram() {
        System.out.println("\n--- Create Aid Program ---");
        String name = ConsoleInput.readRequired("Program name: ");
        String type = ConsoleInput.choose("Aid type:", AidProgram.AID_TYPES);
        String items = ConsoleInput.readRequired("Items or amount (e.g. 'Relief pack' or 'P1500'): ");
        String purok = ConsoleInput.readLine("Target purok (blank = all): ");
        String date = ConsoleInput.readDate("Program date (YYYY-MM-DD): ");
        int id = programDao.createProgram(actor.userId(), name, type, items, purok.isEmpty() ? "ALL" : purok, date);
        System.out.println("Program #" + id + " created with status PLANNED.");
    }

    private void updateProgramStatus() {
        List<AidProgram> programs = programDao.getAll();
        if (programs.isEmpty()) {
            System.out.println("No programs yet.");
            return;
        }
        ConsolePrinter.programs(programs);
        int id = ConsoleInput.readInt("Program ID (0 to cancel): ");
        if (id == 0) return;
        AidProgram p = programDao.findById(id);
        if (p == null) {
            System.out.println("Program #" + id + " not found.");
            return;
        }
        String newStatus = ConsoleInput.choose("Current status: " + p.status() + ". New status:", AidProgram.STATUSES);
        System.out.println(programDao.updateStatus(actor.userId(), id, newStatus)
                ? "Program #" + id + " is now " + newStatus + "."
                : "Status is already " + newStatus + ". No changes made.");
    }

    private void recordClaim() {
        List<AidProgram> ongoing = programDao.getOngoing();
        if (ongoing.isEmpty()) {
            System.out.println("No ONGOING programs. Set a program to ONGOING first (option 4).");
            return;
        }
        ConsolePrinter.programs(ongoing);
        int programId = ConsoleInput.readInt("Program ID: ");
        ConsolePrinter.households(householdDao.getAll());
        int householdId = ConsoleInput.readInt("Household ID: ");
        Household h = householdDao.findById(householdId);
        if (h == null) {
            System.out.println("Household #" + householdId + " not found.");
            return;
        }
        String claimant = ConsoleInput.readLine("Claimant name (blank = " + h.headName() + "): ");
        if (claimant.isEmpty()) claimant = h.headName();
        String received = ConsoleInput.readRequired("Items or amount received: ");

        ClaimResult result = distributionDao.recordClaim(actor.userId(), programId, householdId, claimant, received);
        System.out.println(result.message());
    }

    private void voidClaim() {
        ConsolePrinter.claims(distributionDao.getAllClaims());
        int id = ConsoleInput.readInt("Claim ID to void (0 to cancel): ");
        if (id == 0) return;
        String reason = ConsoleInput.readRequired("Reason for voiding: ");
        if (!ConsoleInput.confirm("Void Claim #" + id + "?")) {
            System.out.println("Cancelled.");
            return;
        }
        System.out.println(distributionDao.voidClaim(actor.userId(), id, reason).message());
    }

    private void unserved() {
        ConsolePrinter.programs(programDao.getAll());
        int id = ConsoleInput.readInt("Program ID: ");
        if (programDao.findById(id) == null) {
            System.out.println("Program #" + id + " not found.");
            return;
        }
        List<Household> list = distributionDao.getUnservedHouseholds(id);
        if (list.isEmpty()) System.out.println("All active households have been served.");
        else ConsolePrinter.households(list);
    }
}
