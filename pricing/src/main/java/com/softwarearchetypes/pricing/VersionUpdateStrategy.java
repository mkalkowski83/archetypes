package com.softwarearchetypes.pricing;

import java.util.List;

/**
 * Strategy for validating version updates when adding new component versions. Different strategies
 * allow different levels of strictness for validity period overlaps.
 */
public enum VersionUpdateStrategy {

    /**
     * Reject if identical validity period exists. Allows overlapping periods - resolution uses
     * youngest validFrom (latest wins).
     *
     * <p>Example: OK to have [2024-01-01, ∞) and [2024-02-01, 2024-03-01) NOT OK to have two
     * versions with [2024-01-01, ∞)
     *
     * <p>This is the recommended default strategy for temporal pricing: - Base tariff valid from
     * January forever: [2024-01-01, ∞) - Temporary discount in February: [2024-02-01, 2024-03-01) -
     * After March 1st, automatically reverts to base tariff
     */
    REJECT_IDENTICAL {
        @Override
        void validate(List<? extends ComponentVersion> existingVersions, Validity newValidity) {
            for (ComponentVersion version : existingVersions) {
                if (version.validity().equals(newValidity)) {
                    throw new IllegalArgumentException(
                            "Version with identical validity period already exists: %s. "
                                    + "Use different validFrom/validTo to create temporal overlaps."
                                            .formatted(newValidity));
                }
            }
        }
    },

    /**
     * Reject if any overlap with existing periods. Requires clean, non-overlapping time windows.
     *
     * <p>Example: OK to have [2024-01-01, 2024-02-01) and [2024-02-01, ∞) NOT OK to have
     * [2024-01-01, ∞) and [2024-02-01, 2024-03-01)
     *
     * <p>Use when you need strict temporal partitioning with no ambiguity. Less flexible but
     * provides deterministic version selection.
     */
    REJECT_OVERLAPPING {
        @Override
        void validate(List<? extends ComponentVersion> existingVersions, Validity newValidity) {
            for (ComponentVersion version : existingVersions) {
                if (version.validity().overlaps(newValidity)) {
                    throw new IllegalArgumentException(
                            "New validity period %s overlaps with existing period %s. "
                                    + "Use REJECT_IDENTICAL strategy if overlaps are intentional."
                                            .formatted(newValidity, version.validity()));
                }
            }
        }
    },

    /**
     * No validation - allows duplicates and overlaps.
     *
     * <p>Use with caution - when multiple versions match a point in time, resolution uses youngest
     * validFrom (latest added wins).
     *
     * <p>Useful for advanced scenarios like: - A/B testing with multiple concurrent pricing
     * strategies - Gradual rollouts where latest version takes precedence - Import/migration
     * scenarios where cleanup happens later
     */
    ALLOW_ALL {
        @Override
        void validate(List<? extends ComponentVersion> existingVersions, Validity newValidity) {
            // No validation - anything goes
        }
    };

    /**
     * Validate that adding a new version with given validity is allowed.
     *
     * @param existingVersions current versions in the component
     * @param newValidity validity period of version being added
     * @throws IllegalArgumentException if validation fails according to this strategy
     */
    abstract void validate(List<? extends ComponentVersion> existingVersions, Validity newValidity);
}
