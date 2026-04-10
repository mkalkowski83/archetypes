package com.softwarearchetypes.pricing;

import java.util.List;
import java.util.Optional;

/**
 * Aggregate of CalculatorRange objects with validation. Knows which parameter to check and ensures
 * all ranges are compatible (same type) and non-overlapping.
 */
record Ranges(String rangeSelector, List<CalculatorRange> ranges) {

    public Ranges {
        if (rangeSelector == null || rangeSelector.isBlank()) {
            throw new IllegalArgumentException("Range selector cannot be null or blank");
        }
        ranges = List.copyOf(ranges); // defensive copy - immutable
        validate(ranges);
    }

    /**
     * Factory method for convenient creation of Ranges.
     *
     * @param rangeSelector the parameter name to check
     * @param ranges the calculator ranges (varargs)
     * @return new Ranges instance
     */
    public static Ranges of(String rangeSelector, CalculatorRange... ranges) {
        return new Ranges(rangeSelector, List.of(ranges));
    }

    private void validate(List<CalculatorRange> ranges) {
        if (ranges.isEmpty()) {
            throw new IllegalArgumentException("Ranges cannot be empty");
        }

        validateCompatibility(ranges);
        validateNoOverlaps(ranges);
    }

    /** Validates that all ranges are compatible (same type). */
    private void validateCompatibility(List<CalculatorRange> ranges) {
        if (ranges.size() < 2) {
            return; // single range - always compatible with itself
        }

        CalculatorRange first = ranges.get(0);

        for (int i = 1; i < ranges.size(); i++) {
            CalculatorRange current = ranges.get(i);
            if (!first.isCompatibleWith(current)) {
                throw new IllegalArgumentException(
                        "All ranges must be of the same type. Found incompatible types: %s and %s"
                                .formatted(
                                        first.getClass().getSimpleName(),
                                        current.getClass().getSimpleName()));
            }
        }
    }

    /** Validates that no two ranges overlap. */
    private void validateNoOverlaps(List<CalculatorRange> ranges) {
        for (int i = 0; i < ranges.size(); i++) {
            for (int j = i + 1; j < ranges.size(); j++) {
                CalculatorRange rangeI = ranges.get(i);
                CalculatorRange rangeJ = ranges.get(j);

                if (rangeI.overlaps(rangeJ)) {
                    throw new IllegalArgumentException(
                            "Ranges cannot overlap: %s overlaps with %s".formatted(rangeI, rangeJ));
                }
            }
        }
    }

    /**
     * Finds the first range that contains the value from the specified parameter.
     *
     * @param parameters the parameters containing the value to check
     * @return Optional containing the matching range, or empty if no match
     */
    public Optional<CalculatorRange> findMatching(Parameters parameters) {
        Object value = parameters.get(rangeSelector);

        if (value == null) {
            throw new IllegalArgumentException(
                    "Parameter '%s' is required but not found in parameters"
                            .formatted(rangeSelector));
        }

        return ranges.stream().filter(range -> range.contains(value)).findFirst();
    }

    /** Returns the number of ranges. */
    public int size() {
        return ranges.size();
    }

    /** Returns all ranges as a list. */
    public List<CalculatorRange> toList() {
        return ranges; // already immutable from defensive copy
    }

    @Override
    public String toString() {
        return "Ranges[selector='%s', ranges=%s]".formatted(rangeSelector, ranges);
    }
}
