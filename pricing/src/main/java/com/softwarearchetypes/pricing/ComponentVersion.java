package com.softwarearchetypes.pricing;

import java.time.LocalDateTime;

/**
 * Marker interface for component versions. Represents a snapshot of component configuration valid
 * during a specific time period.
 *
 * <p>This interface exists primarily to: 1. Enable polymorphic validation strategies
 * (VersionUpdateStrategy) 2. Provide common access to validity period 3. Track when version was
 * defined (for tiebreaking)
 *
 * <p>Implementations: - SimpleComponentVersion: calculator + parameter mappings -
 * CompositeComponentVersion: children + dependencies
 */
sealed interface ComponentVersion permits SimpleComponentVersion, CompositeComponentVersion {

    /**
     * Returns the validity period for this version. When multiple versions overlap, the one with
     * youngest validFrom takes precedence. If validFrom is identical, definedAt is used as
     * tiebreaker.
     */
    Validity validity();

    /**
     * Returns when this version was defined/created. Used as tiebreaker when multiple versions have
     * identical validFrom.
     */
    LocalDateTime definedAt();
}
