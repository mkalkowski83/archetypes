package com.softwarearchetypes.party;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Phone address details with validation. Uses per-country patterns for known countries, falls back
 * to a general pattern for others.
 */
record PhoneAddressDetails(String phoneNumber) implements AddressDetails {

    // Patterns match against normalized input (whitespace, dashes, dots, parens stripped)
    private static final Pattern PL = Pattern.compile("^\\+48\\d{9}$");
    private static final Pattern US = Pattern.compile("^\\+1\\d{10}$");
    private static final Pattern UK = Pattern.compile("^\\+44\\d{7,10}$");
    private static final Pattern FR = Pattern.compile("^\\+33\\d{9}$");
    private static final Pattern DE = Pattern.compile("^\\+49\\d{5,12}$");
    private static final Pattern LOCAL = Pattern.compile("^\\d{7,10}$");

    private static final List<Pattern> PATTERNS = List.of(PL, US, UK, FR, DE, LOCAL);

    private static final int MIN_DIGITS = 7;
    private static final int MAX_DIGITS = 15;

    public PhoneAddressDetails {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new IllegalArgumentException("Phone number cannot be null or empty");
        }

        String stripped = phoneNumber.replaceAll("[\\s.()-]", "");

        if (!stripped.matches("^\\+?\\d+$")) {
            throw new IllegalArgumentException("Invalid phone number format: " + phoneNumber);
        }

        String digitsOnly = stripped.replaceAll("[^0-9]", "");

        if (digitsOnly.length() < MIN_DIGITS || digitsOnly.length() > MAX_DIGITS) {
            throw new IllegalArgumentException(
                    "Phone number must contain between "
                            + MIN_DIGITS
                            + " and "
                            + MAX_DIGITS
                            + " digits: "
                            + phoneNumber);
        }

        if (PATTERNS.stream().noneMatch(pattern -> pattern.matcher(stripped).matches())) {
            throw new IllegalArgumentException("Invalid phone number format: " + phoneNumber);
        }
    }

    public static PhoneAddressDetails of(String phoneNumber) {
        return new PhoneAddressDetails(phoneNumber);
    }

    /** Returns normalized phone number (digits only, with optional + prefix). */
    public String normalized() {
        String result = phoneNumber.replaceAll("[\\s.()-]", "");
        if (phoneNumber.startsWith("+") && !result.startsWith("+")) {
            result = "+" + result;
        }
        return result;
    }
}
