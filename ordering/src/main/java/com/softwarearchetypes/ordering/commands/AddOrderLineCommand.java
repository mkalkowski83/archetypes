package com.softwarearchetypes.ordering.commands;

import com.softwarearchetypes.ordering.OrderId;
import java.util.Map;

public record AddOrderLineCommand(
        OrderId orderId,
        String productId,
        double quantity,
        String unit,
        Map<String, String> specification) {}
