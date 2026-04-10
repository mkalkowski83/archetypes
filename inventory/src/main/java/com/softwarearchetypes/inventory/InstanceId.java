package com.softwarearchetypes.inventory;

import java.util.Objects;
import java.util.UUID;

/** Unique identifier for an Instance (ProductInstance or PackageInstance). */
public record InstanceId(UUID value) {

    public InstanceId {
        Objects.requireNonNull(value, "InstanceId value cannot be null");
    }

    public static InstanceId random() {
        return new InstanceId(UUID.randomUUID());
    }

    public static InstanceId of(UUID value) {
        return new InstanceId(value);
    }

    public static InstanceId of(String value) {
        return new InstanceId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
