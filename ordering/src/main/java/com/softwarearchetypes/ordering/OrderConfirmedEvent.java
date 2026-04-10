package com.softwarearchetypes.ordering;

import com.softwarearchetypes.quantity.money.Money;
import java.time.LocalDateTime;
import java.util.List;

record OrderConfirmedEvent(
        OrderId orderId,
        Money totalPrice,
        PartyId payerId,
        List<OrderLine> lines,
        LocalDateTime occurredAt) {}
