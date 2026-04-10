package com.softwarearchetypes.inventory.availability;

import com.softwarearchetypes.quantity.Quantity;
import java.util.Objects;

public record PoolLockRequest(
        ResourceId resourceId, Quantity quantity, OwnerId owner, LockDuration duration)
        implements LockRequest {

    public PoolLockRequest {
        Objects.requireNonNull(resourceId, "ResourceId cannot be null");
        Objects.requireNonNull(quantity, "Quantity cannot be null");
        Objects.requireNonNull(owner, "OwnerId cannot be null");
        Objects.requireNonNull(duration, "LockDuration cannot be null");
    }

    public static PoolLockRequest of(
            ResourceId resourceId, Quantity quantity, OwnerId owner, LockDuration duration) {
        return new PoolLockRequest(resourceId, quantity, owner, duration);
    }

    public static PoolLockRequest indefinite(
            ResourceId resourceId, Quantity quantity, OwnerId owner) {
        return new PoolLockRequest(resourceId, quantity, owner, LockDuration.indefinite());
    }
}
