package com.greenfields.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/** Salted PBKDF2 password storage for user accounts. */
public final class PasswordHasher {

    private static final String PREFIX = "pbkdf2-sha256";
    private static final int ITERATIONS = 310_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() { }

    public static String hash(char[] password) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] derived = derive(password, salt, ITERATIONS);
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        return PREFIX + "$" + ITERATIONS + "$" + encoder.encodeToString(salt)
                + "$" + encoder.encodeToString(derived);
    }

    public static boolean matches(String candidate, String stored) {
        if (candidate == null || stored == null) return false;
        if (!stored.startsWith(PREFIX + "$")) {
            return MessageDigest.isEqual(candidate.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    stored.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }

        try {
            String[] parts = stored.split("\\$", -1);
            if (parts.length != 4 || !PREFIX.equals(parts[0])) return false;
            int iterations = Integer.parseInt(parts[1]);
            if (iterations < 100_000 || iterations > 2_000_000) return false;
            Base64.Decoder decoder = Base64.getUrlDecoder();
            byte[] salt = decoder.decode(parts[2]);
            byte[] expected = decoder.decode(parts[3]);
            if (salt.length != SALT_BYTES || expected.length != KEY_BITS / 8) return false;
            byte[] actual = derive(candidate.toCharArray(), salt, iterations);
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static boolean isHashed(String stored) {
        return stored != null && stored.startsWith(PREFIX + "$");
    }

    private static byte[] derive(char[] password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Password hashing is unavailable", e);
        } finally {
            spec.clearPassword();
        }
    }
}
