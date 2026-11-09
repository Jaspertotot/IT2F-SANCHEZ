package com.ayudatrace.model;

/** One row of the activity_logs table. */
public record ActivityLog(int id, int userId, String actionType, String description, String timestamp) {
}
