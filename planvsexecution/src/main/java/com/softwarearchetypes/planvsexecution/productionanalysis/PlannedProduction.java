package com.softwarearchetypes.planvsexecution.productionanalysis;

import java.util.UUID;

/**
 * Represents planned production for a product - the PLAN entity. Simple: product ID and target
 * quantity. No dates, no complexity.
 */
public record PlannedProduction(UUID id, String productId, int targetQuantity) {

    public PlannedProduction(String productId, int targetQuantity) {
        this(UUID.randomUUID(), productId, targetQuantity);
    }

    public static PlannedProduction of(String productId, int targetQuantity) {
        return new PlannedProduction(productId, targetQuantity);
    }

    @Override
    public String toString() {
        return String.format("Planned[%s: %d units]", productId, targetQuantity);
    }
}
