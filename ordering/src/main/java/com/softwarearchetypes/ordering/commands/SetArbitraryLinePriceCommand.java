package com.softwarearchetypes.ordering.commands;

import com.softwarearchetypes.ordering.OrderId;
import com.softwarearchetypes.ordering.OrderLineId;
import java.math.BigDecimal;

public record SetArbitraryLinePriceCommand(
        OrderId orderId,
        OrderLineId lineId,
        BigDecimal unitPrice,
        BigDecimal totalPrice,
        String currency,
        String reason) {}
