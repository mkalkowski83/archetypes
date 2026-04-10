package com.softwarearchetypes.ordering;

import static org.junit.jupiter.api.Assertions.*;

import com.softwarearchetypes.common.Result;
import com.softwarearchetypes.ordering.commands.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * E-commerce order scenario: Customer orders an iPhone, adds a mouse, changes mouse quantity,
 * confirms (inventory allocated, payment captured), fulfillment progresses and completes.
 */
class ECommerceOrderScenarios {

    private final OrderingConfiguration configuration = OrderingConfiguration.inMemory();
    private final OrderingFacade facade = configuration.orderingFacade();
    private final OrderingQueries queries = configuration.orderingQueries();

    @Test
    void fullECommerceOrderLifecycle() {
        // given
        OrderView order =
                facade.handle(
                                new CreateOrderCommand(
                                        List.of(
                                                new CreateOrderCommand.OrderPartyData(
                                                        "customer-jan",
                                                        "Jan Kowalski",
                                                        "jan@example.com",
                                                        Set.of("ORDERER", "PAYER", "RECEIVER")),
                                                new CreateOrderCommand.OrderPartyData(
                                                        "shop-online",
                                                        "TechShop Online",
                                                        "shop@tech.com",
                                                        Set.of("EXECUTOR"))),
                                        List.of(
                                                new CreateOrderCommand.OrderLineData(
                                                        "APPLE-IPHONE-15-PRO",
                                                        1,
                                                        "pieces",
                                                        Map.of(
                                                                "color",
                                                                "titanium-blue",
                                                                "storage",
                                                                "256GB"),
                                                        null))))
                        .getSuccess();

        assertEquals("DRAFT", order.status());
        assertEquals(1, order.lines().size());
        assertEquals("titanium-blue", order.lines().get(0).specification().get("color"));

        // when - customer adds a mouse
        order =
                facade.handle(
                                new AddOrderLineCommand(
                                        order.id(),
                                        "MOUSE-LOGITECH-MX3",
                                        1,
                                        "pieces",
                                        Map.of("color", "black")))
                        .getSuccess();

        // then
        assertEquals(2, order.lines().size());

        // when - customer changes mouse quantity to 2
        OrderLineId mouseLineId = order.lines().get(1).id();
        order =
                facade.handle(
                                new ChangeOrderLineQuantityCommand(
                                        order.id(), mouseLineId, 2, "pieces"))
                        .getSuccess();

        // then
        assertTrue(order.lines().get(1).quantity().contains("2"));

        // when - customer confirms the order
        Result<String, OrderView> confirmResult =
                facade.handle(new ConfirmOrderCommand(order.id()));

        // then - inventory allocated, payment captured, fulfillment started
        assertTrue(confirmResult.success());
        order = confirmResult.getSuccess();
        assertEquals("CONFIRMED", order.status());
        assertEquals(2, configuration.inventoryService().allocateRequests().size());
        assertEquals(1, configuration.paymentService().authorizeRequests().size());
        assertEquals(1, configuration.fulfillmentService().startedOrders().size());

        // when - fulfillment starts
        facade.handle(
                new FulfillmentUpdated(
                        order.id(),
                        FulfillmentStatus.IN_PROGRESS,
                        "Items being picked",
                        LocalDateTime.now()));

        // then
        order = queries.findById(order.id()).orElseThrow();
        assertEquals("PROCESSING", order.status());

        // when - fulfillment completes
        facade.handle(
                new FulfillmentUpdated(
                        order.id(),
                        FulfillmentStatus.COMPLETED,
                        "Delivered to customer",
                        LocalDateTime.now()));

        // then
        order = queries.findById(order.id()).orElseThrow();
        assertEquals("FULFILLED", order.status());
    }
}
