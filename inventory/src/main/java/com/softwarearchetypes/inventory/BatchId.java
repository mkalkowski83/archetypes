package com.softwarearchetypes.inventory;

import java.util.Objects;
import java.util.UUID;

public record BatchId(UUID value) {

    public BatchId {
        Objects.requireNonNull(value, "BatchId value cannot be null");
    }

    public static BatchId random() {
        return new BatchId(UUID.randomUUID());
    }

    public static BatchId of(UUID value) {
        return new BatchId(value);
    }

    public static BatchId of(String value) {
        return new BatchId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
