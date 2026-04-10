package com.softwarearchetypes.pricing;

import java.time.LocalDateTime;

/**
 * Represents a validity period for pricing components. validFrom is inclusive, validTo is exclusive
 * [validFrom, validTo)
 *
 * <p>Examples: - Permanent price: Validity.from(2024-01-01) → valid from Jan 1 forever - Temporary
 * discount: Validity.between(2024-02-01, 2024-03-01) → valid only in February - When periods
 * overlap, the version with the latest validFrom takes precedence
 */
public record Validity(LocalDateTime validFrom, LocalDateTime validTo) {

    public static final Validity ALWAYS = new Validity(LocalDateTime.MIN, LocalDateTime.MAX);

    public static Validity until(LocalDateTime validTo) {
        return new Validity(LocalDateTime.MIN, validTo);
    }

    public static Validity from(LocalDateTime validFrom) {
        return new Validity(validFrom, LocalDateTime.MAX);
    }

    public static Validity between(LocalDateTime validFrom, LocalDateTime validTo) {
        if (validFrom == null && validTo == null) {
            return ALWAYS;
        }
        if (validFrom == null) {
            return until(validTo);
        }
        if (validTo == null) {
            return from(validFrom);
        }
        if (!validFrom.isBefore(validTo)) {
            throw new IllegalArgumentException(
                    "validFrom must be before validTo: [%s, %s)".formatted(validFrom, validTo));
        }
        return new Validity(validFrom, validTo);
    }

    public static Validity always() {
        return ALWAYS;
    }

    public boolean isValidAt(LocalDateTime pointInTime) {
        return !pointInTime.isBefore(validFrom) && pointInTime.isBefore(validTo);
    }

    public boolean hasExpired(LocalDateTime pointInTime) {
        return !pointInTime.isBefore(validTo);
    }

    public boolean hasNotStartedYet(LocalDateTime pointInTime) {
        return pointInTime.isBefore(validFrom);
    }

    public boolean overlaps(Validity other) {
        // Two periods overlap if one starts before the other ends
        return this.validFrom.isBefore(other.validTo) && other.validFrom.isBefore(this.validTo);
    }
}
