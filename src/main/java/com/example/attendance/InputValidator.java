package com.example.attendance;

import java.util.regex.Pattern;

/**
 * Shared input rules (email, mobile number, password).
 * Keep these in sync with the checks in employees.html and login.html.
 */
public final class InputValidator {

    private InputValidator() {
    }

    private static final Pattern EMAIL = Pattern.compile(
            "^[A-Za-z0-9]+([._%+-][A-Za-z0-9]+)*@[A-Za-z0-9]+(-[A-Za-z0-9]+)*(\\.[A-Za-z0-9]+(-[A-Za-z0-9]+)*)*\\.[A-Za-z]{2,}$");

    /** Indian mobile number: exactly 10 digits, starting with 6, 7, 8 or 9. */
    private static final Pattern MOBILE = Pattern.compile("^[6-9][0-9]{9}$");

    public static final int PASSWORD_MIN_LENGTH = 8;

    public static final String PASSWORD_RULES =
            "Password must be at least 8 characters and include an uppercase letter, "
                    + "a lowercase letter, a number and a special character, "
                    + "with no spaces.";

    public static boolean isValidEmail(String email) {
        return email != null
                && email.length() <= 254
                && EMAIL.matcher(email.trim()).matches();
    }

    /** Removes spaces and dashes typed between digits. */
    public static String cleanMobile(String mobile) {
        return mobile == null ? "" : mobile.replaceAll("[\\s-]", "");
    }

    public static boolean isValidMobile(String mobile) {
        return MOBILE.matcher(cleanMobile(mobile)).matches();
    }

    /** Returns null when the password is strong enough, otherwise the reason. */
    public static String passwordError(String password, String email) {

        if (password == null || password.length() < PASSWORD_MIN_LENGTH) {
            return PASSWORD_RULES;
        }
        if (password.length() > 64) {
            return "Password must be 64 characters or fewer.";
        }
        if (password.chars().anyMatch(Character::isWhitespace)) {
            return PASSWORD_RULES;
        }
        boolean upper = password.chars().anyMatch(Character::isUpperCase);
        boolean lower = password.chars().anyMatch(Character::isLowerCase);
        boolean digit = password.chars().anyMatch(Character::isDigit);
        boolean special = password.chars()
                .anyMatch(c -> !Character.isLetterOrDigit(c) && !Character.isWhitespace(c));
        if (!(upper && lower && digit && special)) {
            return PASSWORD_RULES;
        }
        if (email != null && email.contains("@")) {
            String local = email.substring(0, email.indexOf('@')).toLowerCase();
            if (local.length() >= 4 && password.toLowerCase().contains(local)) {
                return "Password must not contain your email name.";
            }
        }
        return null;
    }
}
