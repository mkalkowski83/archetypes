package com.softwarearchetypes.inventory.availability;

import java.util.Objects;
import java.util.UUID;

public record ResourceAvailabilityId(UUID id) {

    public ResourceAvailabilityId {
        Objects.requireNonNull(id, "ResourceAvailabilityId cannot be null");
    }

    public static ResourceAvailabilityId random() {
        return new ResourceAvailabilityId(UUID.randomUUID());
    }

    public static ResourceAvailabilityId of(UUID id) {
        return new ResourceAvailabilityId(id);
    }

    public static ResourceAvailabilityId of(String id) {
        return new ResourceAvailabilityId(UUID.fromString(id));
    }
}
