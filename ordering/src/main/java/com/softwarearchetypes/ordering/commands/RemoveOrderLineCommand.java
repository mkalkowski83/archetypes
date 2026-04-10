package com.softwarearchetypes.ordering.commands;

import com.softwarearchetypes.ordering.OrderId;
import com.softwarearchetypes.ordering.OrderLineId;

public record RemoveOrderLineCommand(OrderId orderId, OrderLineId lineId) {}
