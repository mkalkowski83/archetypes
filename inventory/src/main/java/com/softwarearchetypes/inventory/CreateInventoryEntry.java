package com.softwarearchetypes.inventory;

import java.util.Objects;

/** Command for creating a new InventoryEntry for a product. */
public record CreateInventoryEntry(InventoryProduct product) {

    public CreateInventoryEntry {
        Objects.requireNonNull(product, "product cannot be null");
    }

    public static CreateInventoryEntry forProduct(InventoryProduct product) {
        return new CreateInventoryEntry(product);
    }
}
