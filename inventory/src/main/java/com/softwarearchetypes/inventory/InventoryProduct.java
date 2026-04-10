package com.softwarearchetypes.inventory;

import com.softwarearchetypes.quantity.Unit;
import java.util.Objects;

/**
 * InventoryProduct is a read model projection of product information needed by the inventory
 * module. This keeps the inventory module decoupled from the product module's internal model.
 *
 * <p>Contains only the fields that Inventory needs: - productId: unique identifier - name: for
 * display and logging - trackingStrategy: for validation rules - preferredUnit: for quantity
 * validation
 */
public record InventoryProduct(
        ProductIdentifier productId,
        String name,
        ProductTrackingStrategy trackingStrategy,
        Unit preferredUnit) {

    public InventoryProduct {
        Objects.requireNonNull(productId, "productId cannot be null");
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(trackingStrategy, "trackingStrategy cannot be null");
    }

    public static InventoryProduct of(
            ProductIdentifier productId,
            String name,
            ProductTrackingStrategy trackingStrategy,
            Unit preferredUnit) {
        return new InventoryProduct(productId, name, trackingStrategy, preferredUnit);
    }

    public static InventoryProduct unique(ProductIdentifier productId, String name) {
        return new InventoryProduct(productId, name, ProductTrackingStrategy.UNIQUE, Unit.pieces());
    }

    public static InventoryProduct individuallyTracked(ProductIdentifier productId, String name) {
        return new InventoryProduct(
                productId, name, ProductTrackingStrategy.INDIVIDUALLY_TRACKED, Unit.pieces());
    }

    public static InventoryProduct batchTracked(ProductIdentifier productId, String name) {
        return new InventoryProduct(
                productId, name, ProductTrackingStrategy.BATCH_TRACKED, Unit.pieces());
    }

    public static InventoryProduct identical(ProductIdentifier productId, String name) {
        return new InventoryProduct(
                productId, name, ProductTrackingStrategy.IDENTICAL, Unit.pieces());
    }

    boolean isUnique() {
        return trackingStrategy == ProductTrackingStrategy.UNIQUE;
    }

    boolean isIndividuallyTracked() {
        return trackingStrategy == ProductTrackingStrategy.INDIVIDUALLY_TRACKED
                || trackingStrategy == ProductTrackingStrategy.INDIVIDUALLY_AND_BATCH_TRACKED;
    }

    boolean isBatchTracked() {
        return trackingStrategy == ProductTrackingStrategy.BATCH_TRACKED
                || trackingStrategy == ProductTrackingStrategy.INDIVIDUALLY_AND_BATCH_TRACKED;
    }

    boolean isIdentical() {
        return trackingStrategy == ProductTrackingStrategy.IDENTICAL;
    }
}
