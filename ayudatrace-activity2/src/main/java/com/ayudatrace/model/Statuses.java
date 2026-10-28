package com.ayudatrace.model;

import java.util.List;

public final class Statuses {
    public static final String PENDING = "PENDING";
    public static final String APPROVED = "APPROVED";

    /** Statuses an admin may assign from the Update User screen. */
    public static final List<String> ASSIGNABLE = List.of(APPROVED, PENDING);

    private Statuses() {
    }
}
