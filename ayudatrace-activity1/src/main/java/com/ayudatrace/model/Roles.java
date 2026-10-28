package com.ayudatrace.model;

import java.util.List;

public final class Roles {
    public static final String ADMIN = "ADMIN";
    public static final String STAFF = "STAFF";
    public static final String HOUSEHOLD_REP = "HOUSEHOLD_REP";

    public static final List<String> ALL = List.of(ADMIN, STAFF, HOUSEHOLD_REP);

    private Roles() {
    }
}
