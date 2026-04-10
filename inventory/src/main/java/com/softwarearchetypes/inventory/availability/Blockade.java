package com.softwarearchetypes.inventory.availability;

import java.time.Instant;

/**
 * Blockade represents a lock placed on a resource. It contains information about who owns the lock,
 * when it was created, and its duration.
 */
sealed interface Blockade permits IndividualBlockade, PoolBlockade, TemporalBlockade {

    BlockadeId id();

    OwnerId owner();

    Instant blockedAt();

    LockDuration duration();

    default boolean isActive(Instant now) {
        return duration().isActive(now, blockedAt());
    }

    default boolean isExpired(Instant now) {
        return duration().isExpired(now, blockedAt());
    }

    default boolean isOwnedBy(OwnerId requester) {
        return owner().equals(requester);
    }
}
