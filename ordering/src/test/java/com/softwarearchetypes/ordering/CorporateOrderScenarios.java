package com.softwarearchetypes.ordering;

import static org.junit.jupiter.api.Assertions.*;

import com.softwarearchetypes.common.Result;
import com.softwarearchetypes.ordering.commands.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Corporate (B2B) order scenario: Company orders equipment for a branch office. Orderer and payer
 * is the company, receiver is the branch. After confirmation, the order is cancelled - compensation
 * fires.
 */
class CorporateOrderScenarios {

    private final OrderingConfiguration configuration = OrderingConfiguration.inMemory();
    private final OrderingFacade facade = configuration.orderingFacade();

    @Test
    void corporateOrderWithCancellation() {
        // given
        OrderView order =
                facade.handle(
                                new CreateOrderCommand(
                                        List.of(
                                                new CreateOrderCommand.OrderPartyData(
                                                        "company-abc",
                                                        "ABC Corporation",
                                                        "orders@abc.com",
                                                        Set.of("ORDERER", "PAYER")),
                                                new CreateOrderCommand.OrderPartyData(
                                                        "it-supplier",
                                                        "IT Supplies Ltd",
                                                        "sales@it-supplies.com",
                                                        Set.of("EXECUTOR")),
                                                new CreateOrderCommand.OrderPartyData(
                                                        "branch-warsaw",
                                                        "ABC Warsaw Branch",
                                                        "warsaw@abc.com",
                                                        Set.of("RECEIVER"))),
                                        List.of(
                                                new CreateOrderCommand.OrderLineData(
                                                        "LAPTOP-DELL-5540",
                                                        10,
                                                        "pieces",
                                                        Map.of("ram", "32GB", "ssd", "1TB"),
                                                        null),
                                                new CreateOrderCommand.OrderLineData(
                                                        "MONITOR-LG-27UK850",
                                                        10,
                                                        "pieces",
                                                        Map.of(
                                                                "size",
                                                                "27inch",
                                                                "resolution",
                                                                "4K"),
                                                        null),
                                                new CreateOrderCommand.OrderLineData(
                                                        "DOCKING-STATION-TB3",
                                                        10,
                                                        "pieces",
                                                        Map.of(),
                                                        null))))
                        .getSuccess();

        assertEquals("DRAFT", order.status());
        assertEquals(3, order.lines().size());
        assertEquals(3, order.parties().size());
        assertTrue(
                order.parties().stream()
                        .anyMatch(
                                p ->
                                        p.partyId().equals("company-abc")
                                                && p.roles()
                                                        .containsAll(Set.of("ORDERER", "PAYER"))));
        assertTrue(
                order.parties().stream()
                        .anyMatch(
                                p ->
                                        p.partyId().equals("branch-warsaw")
                                                && p.roles().contains("RECEIVER")));

        order = facade.handle(new ConfirmOrderCommand(order.id())).getSuccess();
        assertEquals("CONFIRMED", order.status());

        // when
        Result<String, OrderView> cancelResult =
                facade.handle(new CancelOrderCommand(order.id(), "Budget reallocation"));

        // then
        assertTrue(cancelResult.success());
        assertEquals("CANCELLED", cancelResult.getSuccess().status());
        assertEquals(1, configuration.fulfillmentService().cancelledOrders().size());
        assertEquals(order.id(), configuration.fulfillmentService().cancelledOrders().get(0));
    }
}
