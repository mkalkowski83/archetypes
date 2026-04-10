package com.softwarearchetypes.ordering;

import java.util.ArrayList;
import java.util.List;

/**
 * Service responsible for fulfillment orchestration. Delegates actual fulfillment to specialized
 * services based on product type.
 */
interface FulfillmentService {

    /** Start fulfillment for a confirmed order. */
    void startFulfillment(OrderId orderId);

    /** Cancel ongoing fulfillment (if possible). */
    void cancelFulfillment(OrderId orderId);
}

class FixableFulfillmentService implements FulfillmentService {

    private boolean shouldThrowOnStart = false;
    private final List<OrderId> startedOrders = new ArrayList<>();
    private final List<OrderId> cancelledOrders = new ArrayList<>();

    public void willFailOnStart() {
        this.shouldThrowOnStart = true;
    }

    public void reset() {
        this.shouldThrowOnStart = false;
        this.startedOrders.clear();
        this.cancelledOrders.clear();
    }

    public List<OrderId> startedOrders() {
        return List.copyOf(startedOrders);
    }

    public List<OrderId> cancelledOrders() {
        return List.copyOf(cancelledOrders);
    }

    @Override
    public void startFulfillment(OrderId orderId) {
        if (shouldThrowOnStart) {
            throw new RuntimeException("Fulfillment service unavailable");
        }
        startedOrders.add(orderId);
    }

    @Override
    public void cancelFulfillment(OrderId orderId) {
        cancelledOrders.add(orderId);
    }
}
