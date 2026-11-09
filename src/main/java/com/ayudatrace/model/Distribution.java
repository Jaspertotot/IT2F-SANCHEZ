package com.ayudatrace.model;

/** An aid claim (joined with program and household names for display). */
public record Distribution(int distributionId, int programId, String programName, int householdId,
                           String headName, String claimantName, String itemsOrAmountReceived,
                           String claimDate, int releasedBy, String status) {
}
