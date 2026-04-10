package com.softwarearchetypes.inventory.availability;

import java.util.UUID;

public record OwnerId(UUID id) {

    public static OwnerId none() {
        return new OwnerId(null);
    }

    public static OwnerId random() {
        return new OwnerId(UUID.randomUUID());
    }

    public static OwnerId of(UUID id) {
        return new OwnerId(id);
    }

    public static OwnerId of(String id) {
        if (id == null) {
            return none();
        }
        return new OwnerId(UUID.fromString(id));
    }

    public boolean isNone() {
        return id == null;
    }

    public boolean isPresent() {
        return id != null;
    }
}
