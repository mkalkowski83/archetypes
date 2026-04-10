package com.softwarearchetypes.inventory;

import java.util.Objects;
import java.util.UUID;

public record InventoryEntryId(UUID id) {

    public InventoryEntryId {
        Objects.requireNonNull(id, "InventoryEntryId cannot be null");
    }

    public static InventoryEntryId random() {
        return new InventoryEntryId(UUID.randomUUID());
    }

    public static InventoryEntryId of(UUID id) {
        return new InventoryEntryId(id);
    }

    public static InventoryEntryId of(String id) {
        return new InventoryEntryId(UUID.fromString(id));
    }
}
