package com.softwarearchetypes.ordering;

import static org.junit.jupiter.api.Assertions.*;

import com.softwarearchetypes.common.Result;
import com.softwarearchetypes.ordering.commands.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Failure scenarios: What happens when inventory is unavailable, payment fails, or operations are
 * attempted in wrong order status.
 */
class OrderFailureScenarios {

    private final OrderingConfiguration configuration = OrderingConfiguration.inMemory();
    private final OrderingFacade facade = configuration.orderingFacade();
    private final OrderingQueries queries = configuration.orderingQueries();
    private final FixableInventoryService inventoryService = configuration.inventoryService();

    @Test
    void confirmFailsWhenInventoryUnavailable_orderStaysDraft() {
        // given
        inventoryService.willFailOnAllocate();
        OrderView order = createSimpleOrder();

        // when
        Result<String, OrderView> result = facade.handle(new ConfirmOrderCommand(order.id()));

        // then
        assertTrue(result.failure());
        assertEquals("DRAFT", queries.findById(order.id()).orElseThrow().status());
        assertTrue(configuration.paymentService().authorizeRequests().isEmpty());
        assertTrue(configuration.fulfillmentService().startedOrders().isEmpty());
    }

    @Test
    void confirmFailsWhenPaymentFails_orderStaysDraft() {
        // given
        configuration.paymentService().willFailOnPayment("Insufficient funds");
        OrderView order = createSimpleOrder();

        // when
        Result<String, OrderView> result = facade.handle(new ConfirmOrderCommand(order.id()));

        // then
        assertTrue(result.failure());
        assertEquals("DRAFT", queries.findById(order.id()).orElseThrow().status());
        assertFalse(inventoryService.allocateRequests().isEmpty());
        assertTrue(configuration.fulfillmentService().startedOrders().isEmpty());
    }

    @Test
    void cannotAddLineToConfirmedOrder() {
        // given
        OrderView order = createSimpleOrder();
        facade.handle(new ConfirmOrderCommand(order.id()));

        // when
        Result<String, OrderView> result =
                facade.handle(new AddOrderLineCommand(order.id(), "MOUSE", 1, "pieces", Map.of()));

        // then
        assertTrue(result.failure());
    }

    @Test
    void cannotCancelAlreadyCancelledOrder() {
        // given
        OrderView order = createSimpleOrder();
        facade.handle(new CancelOrderCommand(order.id(), "Changed mind"));

        // when
        Result<String, OrderView> result =
                facade.handle(new CancelOrderCommand(order.id(), "Double cancel"));

        // then
        assertTrue(result.failure());
    }

    @Test
    void confirmFailsWhenInventoryWaitlisted() {
        // given
        inventoryService.willReturnOnAllocate(AllocationStatus.WAITLISTED);
        OrderView order = createSimpleOrder();

        // when
        Result<String, OrderView> result = facade.handle(new ConfirmOrderCommand(order.id()));

        // then
        assertTrue(result.failure());
        assertEquals("DRAFT", queries.findById(order.id()).orElseThrow().status());
    }

    @Test
    void confirmFailsWhenInventoryPartiallyAllocated() {
        // given
        inventoryService.willReturnOnAllocate(AllocationStatus.PARTIAL);
        OrderView order = createSimpleOrder();

        // when
        Result<String, OrderView> result = facade.handle(new ConfirmOrderCommand(order.id()));

        // then
        assertTrue(result.failure());
        assertEquals("DRAFT", queries.findById(order.id()).orElseThrow().status());
    }

    private OrderView createSimpleOrder() {
        return facade.handle(
                        new CreateOrderCommand(
                                List.of(
                                        new CreateOrderCommand.OrderPartyData(
                                                "customer-1",
                                                "Customer",
                                                "c@test.com",
                                                Set.of("ORDERER", "PAYER", "RECEIVER")),
                                        new CreateOrderCommand.OrderPartyData(
                                                "shop-1",
                                                "Shop",
                                                "s@test.com",
                                                Set.of("EXECUTOR"))),
                                List.of(
                                        new CreateOrderCommand.OrderLineData(
                                                "PRODUCT-1", 1, "pieces", Map.of(), null))))
                .getSuccess();
    }
}
