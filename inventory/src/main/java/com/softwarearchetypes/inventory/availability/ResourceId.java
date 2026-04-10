package com.softwarearchetypes.inventory.availability;

import java.util.UUID;

public record ResourceId(UUID id) {

    public static ResourceId none() {
        return new ResourceId(null);
    }

    public static ResourceId random() {
        return new ResourceId(UUID.randomUUID());
    }

    public static ResourceId of(UUID id) {
        return new ResourceId(id);
    }

    public static ResourceId of(String id) {
        if (id == null) {
            return none();
        }
        return new ResourceId(UUID.fromString(id));
    }

    public boolean isNone() {
        return id == null;
    }
}
