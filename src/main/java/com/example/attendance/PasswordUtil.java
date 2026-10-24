package com.example.attendance;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * One-way password storage (PBKDF2 with a random salt). A stored value looks like
 * pbkdf2$iterations$salt$hash, so nobody can read the real password from the database.
 *
 * Old passwords that are still plain text keep working: matches() accepts them,
 * and the login code replaces them with a hash the first time that person signs in.
 */
public final class PasswordUtil {

    private PasswordUtil() {
    }

    /**
     * SWITCH: true  = passwords are stored scrambled (hash)  - safe, recommended
     *         false = passwords are stored exactly as typed  - anyone who opens the database can read them
     * Currently OFF because the owner asked to see the real password in the database.
     * Change to true any time; old plain values keep working and become hashes on the next login.
     */
    public static final boolean HASH_PASSWORDS = false;

    private static final String PREFIX = "pbkdf2";
    private static final int ITERATIONS = 120_000;
    private static final int KEY_BITS = 256;
    private static final int SALT_BYTES = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String hash(String rawPassword) {

        if (!HASH_PASSWORDS) {
            return rawPassword;
        }

        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);

        byte[] hash = derive(rawPassword, salt, ITERATIONS);

        return PREFIX + "$" + ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    public static boolean isHashed(String stored) {
        return stored != null && stored.startsWith(PREFIX + "$");
    }

    public static boolean matches(String rawPassword, String stored) {

        if (rawPassword == null || stored == null) {
            return false;
        }

        // Old plain-text value
        if (!isHashed(stored)) {
            return MessageDigest.isEqual(
                    rawPassword.getBytes(StandardCharsets.UTF_8),
                    stored.getBytes(StandardCharsets.UTF_8));
        }

        try {
            String[] parts = stored.split("\\$");
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);

            return MessageDigest.isEqual(derive(rawPassword, salt, iterations), expected);

        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] derive(String rawPassword, byte[] salt, int iterations) {

        try {
            PBEKeySpec spec = new PBEKeySpec(rawPassword.toCharArray(), salt, iterations, KEY_BITS);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec)
                    .getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("Password hashing is not available", e);
        }
    }
}
