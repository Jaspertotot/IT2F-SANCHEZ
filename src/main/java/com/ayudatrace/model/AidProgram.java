package com.ayudatrace.model;

import java.util.List;

public record AidProgram(int programId, String programName, String aidType, String itemsOrAmount,
                         String targetPurok, String programDate, int createdBy, String status) {

    public static final List<String> AID_TYPES = List.of("RELIEF_GOODS", "CASH");
    public static final List<String> STATUSES = List.of("PLANNED", "ONGOING", "COMPLETED", "CANCELLED");
}
