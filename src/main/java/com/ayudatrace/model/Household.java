package com.ayudatrace.model;

public record Household(int householdId, String headName, String purokAddress, String contactNo,
                        int memberCount, String category, Integer userId, String status) {
}
