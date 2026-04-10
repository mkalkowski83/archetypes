package com.softwarearchetypes.ordering;

import java.util.List;

public record OrderView(
        OrderId id,
        String status,
        List<OrderLineView> lines,
        List<PartyInOrderView> parties,
        String totalPrice) {

    static OrderView from(Order order) {
        return new OrderView(
                order.id(),
                order.status().name(),
                order.lines().stream().map(OrderLineView::from).toList(),
                order.parties().parties().stream().map(PartyInOrderView::from).toList(),
                order.totalPrice().map(Object::toString).orElse(null));
    }
}
