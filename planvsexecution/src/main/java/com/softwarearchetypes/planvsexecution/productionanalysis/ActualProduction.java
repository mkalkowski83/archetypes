package com.softwarearchetypes.planvsexecution.productionanalysis;

import java.util.UUID;

/**
 * Represents actual production output - the EXECUTION entity. What was really produced: product ID
 * and actual quantity.
 */
public record ActualProduction(UUID id, String productId, int producedQuantity) {

    public ActualProduction(String productId, int producedQuantity) {
        this(UUID.randomUUID(), productId, producedQuantity);
    }

    public static ActualProduction of(String productId, int producedQuantity) {
        return new ActualProduction(productId, producedQuantity);
    }

    @Override
    public String toString() {
        return String.format("Produced[%s: %d units]", productId, producedQuantity);
    }
}
