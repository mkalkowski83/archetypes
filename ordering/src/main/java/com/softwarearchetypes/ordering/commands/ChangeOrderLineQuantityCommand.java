package com.softwarearchetypes.ordering.commands;

import com.softwarearchetypes.ordering.OrderId;
import com.softwarearchetypes.ordering.OrderLineId;

public record ChangeOrderLineQuantityCommand(
        OrderId orderId, OrderLineId lineId, double newQuantity, String unit) {}
