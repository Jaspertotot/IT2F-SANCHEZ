package com.ayudatrace.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/** Salted SHA-256 hashing. Stored format: saltHex$hashHex */
public final class PasswordUtil {
    private static final SecureRandom RNG = new SecureRandom();

    private PasswordUtil() {}

    public static String hash(String password) {
        byte[] salt = new byte[16];
        RNG.nextBytes(salt);
        return HexFormat.of().formatHex(salt) + "$" + sha256(salt, password);
    }

    public static boolean verify(String password, String stored) {
        int i = stored.indexOf('$');
        if (i < 0) return false;
        byte[] salt = HexFormat.of().parseHex(stored.substring(0, i));
        String expected = stored.substring(i + 1);
        return MessageDigest.isEqual(
                sha256(salt, password).getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8));
    }

    private static String sha256(byte[] salt, String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            return HexFormat.of().formatHex(md.digest(password.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
