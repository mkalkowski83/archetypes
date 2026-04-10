package com.softwarearchetypes.inventory.availability;

import java.util.Objects;

/**
 * LockCommand bundles a LockRequest with the target ResourceAvailabilityId. Used when working
 * directly with availability layer (without going through inventory).
 */
public record LockCommand(ResourceAvailabilityId availabilityId, LockRequest request) {
    public LockCommand {
        Objects.requireNonNull(availabilityId, "availabilityId cannot be null");
        Objects.requireNonNull(request, "request cannot be null");
    }

    public static LockCommand of(ResourceAvailabilityId availabilityId, LockRequest request) {
        return new LockCommand(availabilityId, request);
    }
}
