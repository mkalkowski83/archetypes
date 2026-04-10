package com.softwarearchetypes.inventory;

import java.util.Objects;
import java.util.UUID;

/**
 * Local representation of product identifier in Inventory module. This is a read model projection -
 * synchronized from Product module via events.
 */
public record ProductIdentifier(String value) {

    public ProductIdentifier {
        Objects.requireNonNull(value, "ProductIdentifier value cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("ProductIdentifier value cannot be blank");
        }
    }

    public static ProductIdentifier of(String value) {
        return new ProductIdentifier(value);
    }

    public static ProductIdentifier of(UUID uuid) {
        return new ProductIdentifier(uuid.toString());
    }

    public static ProductIdentifier random() {
        return new ProductIdentifier(UUID.randomUUID().toString());
    }
}
