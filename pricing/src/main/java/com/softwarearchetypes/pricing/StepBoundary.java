package com.softwarearchetypes.pricing;

/**
 * Defines how step boundaries are calculated in step function calculators.
 *
 * <p>Determines whether step boundaries include or exclude the upper limit: - EXCLUSIVE (default):
 * step size N creates ranges [0, N), [N, 2N), etc. - INCLUSIVE: step size N creates ranges [0, N],
 * [N+1, 2N+1], etc.
 *
 * <p>Example with stepSize=5: - EXCLUSIVE: quantities 0-4 → step 0, 5-9 → step 1, 10-14 → step 2 -
 * INCLUSIVE: quantities 0-5 → step 0, 6-10 → step 1, 11-15 → step 2
 */
public enum StepBoundary {

    /**
     * Exclusive upper boundary (default).
     *
     * <p>Step calculation: floor(quantity / stepSize)
     *
     * <p>Example with stepSize=5: - quantities 0, 1, 2, 3, 4 → step 0 - quantities 5, 6, 7, 8, 9 →
     * step 1 - quantities 10, 11, 12, 13, 14 → step 2
     *
     * <p>Range notation: [0, 5), [5, 10), [10, 15), ...
     */
    EXCLUSIVE,

    /**
     * Inclusive upper boundary.
     *
     * <p>Step calculation: floor((quantity - 1) / stepSize) for quantity > 0
     *
     * <p>Example with stepSize=5: - quantities 0, 1, 2, 3, 4, 5 → step 0 - quantities 6, 7, 8, 9,
     * 10 → step 1 - quantities 11, 12, 13, 14, 15 → step 2
     *
     * <p>Range notation: [0, 5], [6, 10], [11, 15], ...
     *
     * <p>Useful when step size represents "up to N units" rather than "every N units".
     */
    INCLUSIVE;

    /** Human-readable description of this boundary type. */
    public String describe() {
        return switch (this) {
            case EXCLUSIVE -> "Exclusive upper boundary: [0, N), [N, 2N), ...";
            case INCLUSIVE -> "Inclusive upper boundary: [0, N], [N+1, 2N+1], ...";
        };
    }
}
