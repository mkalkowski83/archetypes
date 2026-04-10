package com.softwarearchetypes.ordering.commands;

import com.softwarearchetypes.ordering.OrderId;

public record CancelOrderCommand(OrderId orderId, String reason) {}
