package com.ayudatrace.model;

/** Immutable user account. The password hash is never exposed here. */
public record User(int id,
                   String username,
                   String fullName,
                   String contactNo,
                   String email,
                   String role,
                   String status) {
}
