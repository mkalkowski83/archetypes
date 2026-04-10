package com.softwarearchetypes.inventory.availability;

import java.util.Objects;

public record TemporalLockRequest(
        ResourceId resourceId, TimeSlot slot, OwnerId owner, LockDuration duration)
        implements LockRequest {

    public TemporalLockRequest {
        Objects.requireNonNull(resourceId, "ResourceId cannot be null");
        Objects.requireNonNull(slot, "TimeSlot cannot be null");
        Objects.requireNonNull(owner, "OwnerId cannot be null");
        Objects.requireNonNull(duration, "LockDuration cannot be null");
    }

    public static TemporalLockRequest of(
            ResourceId resourceId, TimeSlot slot, OwnerId owner, LockDuration duration) {
        return new TemporalLockRequest(resourceId, slot, owner, duration);
    }

    public static TemporalLockRequest indefinite(
            ResourceId resourceId, TimeSlot slot, OwnerId owner) {
        return new TemporalLockRequest(resourceId, slot, owner, LockDuration.indefinite());
    }
}
