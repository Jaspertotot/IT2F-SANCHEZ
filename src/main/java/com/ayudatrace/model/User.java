package com.ayudatrace.model;

import java.util.List;

public record User(int userId, String username, String fullName, String contactNo,
                   String email, String role, String status) {

    public static final String ROLE_ADMIN = "ADMINISTRATOR";
    public static final String ROLE_STAFF = "BARANGAY_STAFF";
    public static final String ROLE_HOUSEHOLD = "HOUSEHOLD_REPRESENTATIVE";
    public static final List<String> ROLES = List.of(ROLE_ADMIN, ROLE_STAFF, ROLE_HOUSEHOLD);

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_INACTIVE = "INACTIVE";
    public static final List<String> STATUSES = List.of(STATUS_ACTIVE, STATUS_INACTIVE, STATUS_PENDING);
}
