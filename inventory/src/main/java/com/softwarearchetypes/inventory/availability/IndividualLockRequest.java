package com.softwarearchetypes.inventory.availability;

import java.util.Objects;

public record IndividualLockRequest(ResourceId resourceId, OwnerId owner, LockDuration duration)
        implements LockRequest {

    public IndividualLockRequest {
        Objects.requireNonNull(resourceId, "ResourceId cannot be null");
        Objects.requireNonNull(owner, "OwnerId cannot be null");
        Objects.requireNonNull(duration, "LockDuration cannot be null");
    }

    public static IndividualLockRequest of(
            ResourceId resourceId, OwnerId owner, LockDuration duration) {
        return new IndividualLockRequest(resourceId, owner, duration);
    }

    public static IndividualLockRequest indefinite(ResourceId resourceId, OwnerId owner) {
        return new IndividualLockRequest(resourceId, owner, LockDuration.indefinite());
    }
}
