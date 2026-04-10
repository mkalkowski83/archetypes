package com.softwarearchetypes.ordering;

import java.time.LocalDateTime;

public record FulfillmentUpdated(
        OrderId orderId, FulfillmentStatus status, String details, LocalDateTime occurredAt) {}
