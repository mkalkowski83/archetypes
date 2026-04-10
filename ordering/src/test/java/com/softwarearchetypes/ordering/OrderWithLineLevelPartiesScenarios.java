package com.softwarearchetypes.ordering;

import static org.junit.jupiter.api.Assertions.*;

import com.softwarearchetypes.ordering.commands.CreateOrderCommand;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Multi-location delivery scenario: Company orders equipment to be delivered to different branches.
 * Line-level parties override order-level receiver per line.
 */
class OrderWithLineLevelPartiesScenarios {

    private final OrderingConfiguration configuration = OrderingConfiguration.inMemory();
    private final OrderingFacade facade = configuration.orderingFacade();

    @Test
    void orderWithDifferentReceiversPerLine() {
        // when
        OrderView order =
                facade.handle(
                                new CreateOrderCommand(
                                        List.of(
                                                new CreateOrderCommand.OrderPartyData(
                                                        "company-abc",
                                                        "ABC Corp",
                                                        "abc@corp.com",
                                                        Set.of("ORDERER", "PAYER", "RECEIVER")),
                                                new CreateOrderCommand.OrderPartyData(
                                                        "it-supplier",
                                                        "IT Supplies",
                                                        "it@supplies.com",
                                                        Set.of("EXECUTOR"))),
                                        List.of(
                                                new CreateOrderCommand.OrderLineData(
                                                        "LAPTOP-DELL-5540",
                                                        5,
                                                        "pieces",
                                                        Map.of("ram", "16GB"),
                                                        List.of(
                                                                new CreateOrderCommand
                                                                        .OrderPartyData(
                                                                        "branch-warsaw",
                                                                        "Warsaw Branch",
                                                                        "warsaw@abc.com",
                                                                        Set.of("RECEIVER")))),
                                                new CreateOrderCommand.OrderLineData(
                                                        "MONITOR-LG-27UK850",
                                                        5,
                                                        "pieces",
                                                        Map.of("resolution", "4K"),
                                                        List.of(
                                                                new CreateOrderCommand
                                                                        .OrderPartyData(
                                                                        "branch-cracow",
                                                                        "Cracow Branch",
                                                                        "cracow@abc.com",
                                                                        Set.of("RECEIVER")))))))
                        .getSuccess();

        // then
        assertEquals(2, order.lines().size());

        OrderLineView line1 = order.lines().get(0);
        assertEquals(1, line1.parties().size());
        assertEquals("branch-warsaw", line1.parties().get(0).partyId());
        assertTrue(line1.parties().get(0).roles().contains("RECEIVER"));

        OrderLineView line2 = order.lines().get(1);
        assertEquals(1, line2.parties().size());
        assertEquals("branch-cracow", line2.parties().get(0).partyId());
        assertTrue(line2.parties().get(0).roles().contains("RECEIVER"));

        assertEquals(2, order.parties().size());
    }
}
