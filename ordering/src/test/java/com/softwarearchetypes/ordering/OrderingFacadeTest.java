package com.softwarearchetypes.ordering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softwarearchetypes.common.Result;
import com.softwarearchetypes.ordering.commands.AddOrderLineCommand;
import com.softwarearchetypes.ordering.commands.CancelOrderCommand;
import com.softwarearchetypes.ordering.commands.ChangeOrderLineQuantityCommand;
import com.softwarearchetypes.ordering.commands.ConfirmOrderCommand;
import com.softwarearchetypes.ordering.commands.CreateOrderCommand;
import com.softwarearchetypes.ordering.commands.RemoveOrderLineCommand;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class OrderingFacadeTest {

    private final OrderingConfiguration configuration = OrderingConfiguration.inMemory();
    private final OrderingFacade facade = configuration.orderingFacade();
    private final OrderingQueries queries = configuration.orderingQueries();

    @Test
    void shouldCreateSimpleOrder() {
        // when
        Result<String, OrderView> result =
                facade.handle(simpleOrderCommand("APPLE-IPHONE-15-PRO", 1, "pieces"));

        // then
        assertTrue(result.success());
        OrderView view = result.getSuccess();
        assertEquals("DRAFT", view.status());
        assertEquals(1, view.lines().size());
        assertEquals("APPLE-IPHONE-15-PRO", view.lines().get(0).productId());
    }

    @Test
    void shouldCreateOrderWithMultipleLines() {
        // given
        CreateOrderCommand command =
                new CreateOrderCommand(
                        defaultParties(),
                        List.of(
                                new CreateOrderCommand.OrderLineData(
                                        "LAPTOP-DELL-5540",
                                        1,
                                        "pieces",
                                        Map.of("color", "black"),
                                        null),
                                new CreateOrderCommand.OrderLineData(
                                        "MOUSE-LOGITECH-MX3", 2, "pieces", Map.of(), null)));

        // when
        Result<String, OrderView> result = facade.handle(command);

        // then
        assertTrue(result.success());
        assertEquals(2, result.getSuccess().lines().size());
    }

    @Test
    void shouldCreateOrderWithSpecification() {
        // given
        CreateOrderCommand command =
                new CreateOrderCommand(
                        defaultParties(),
                        List.of(
                                new CreateOrderCommand.OrderLineData(
                                        "BMW-X5-2024",
                                        1,
                                        "pieces",
                                        Map.of("vin", "WBA12345678901234", "color", "black"),
                                        null)));

        // when
        Result<String, OrderView> result = facade.handle(command);

        // then
        assertTrue(result.success());
        OrderLineView line = result.getSuccess().lines().get(0);
        assertEquals("WBA12345678901234", line.specification().get("vin"));
        assertEquals("black", line.specification().get("color"));
    }

    @Test
    void shouldCreateOrderWithCorporateParties() {
        // given
        CreateOrderCommand command =
                new CreateOrderCommand(
                        List.of(
                                new CreateOrderCommand.OrderPartyData(
                                        "company-abc",
                                        "ABC Corp",
                                        "abc@corp.com",
                                        Set.of("ORDERER", "PAYER")),
                                new CreateOrderCommand.OrderPartyData(
                                        "vendor-xyz",
                                        "XYZ Vendor",
                                        "vendor@xyz.com",
                                        Set.of("EXECUTOR")),
                                new CreateOrderCommand.OrderPartyData(
                                        "branch-warsaw",
                                        "Warsaw Branch",
                                        "warsaw@abc.com",
                                        Set.of("RECEIVER"))),
                        List.of(
                                new CreateOrderCommand.OrderLineData(
                                        "LAPTOP-DELL-5540", 5, "pieces", Map.of(), null)));

        // when
        Result<String, OrderView> result = facade.handle(command);

        // then
        assertTrue(result.success());
        OrderView view = result.getSuccess();
        assertEquals(3, view.parties().size());
        assertTrue(
                view.parties().stream()
                        .anyMatch(
                                p ->
                                        p.partyId().equals("company-abc")
                                                && p.roles()
                                                        .containsAll(Set.of("ORDERER", "PAYER"))));
        assertTrue(
                view.parties().stream()
                        .anyMatch(
                                p ->
                                        p.partyId().equals("vendor-xyz")
                                                && p.roles().contains("EXECUTOR")));
        assertTrue(
                view.parties().stream()
                        .anyMatch(
                                p ->
                                        p.partyId().equals("branch-warsaw")
                                                && p.roles().contains("RECEIVER")));
    }

    @Test
    void shouldCreateOrderWithLineLevelParties() {
        // given
        CreateOrderCommand command =
                new CreateOrderCommand(
                        defaultParties(),
                        List.of(
                                new CreateOrderCommand.OrderLineData(
                                        "LAPTOP-DELL-5540",
                                        1,
                                        "pieces",
                                        Map.of(),
                                        List.of(
                                                new CreateOrderCommand.OrderPartyData(
                                                        "branch-cracow",
                                                        "Cracow Branch",
                                                        "cracow@shop.com",
                                                        Set.of("RECEIVER"))))));

        // when
        Result<String, OrderView> result = facade.handle(command);

        // then
        assertTrue(result.success());
        OrderLineView line = result.getSuccess().lines().get(0);
        assertEquals(1, line.parties().size());
        assertEquals("branch-cracow", line.parties().get(0).partyId());
        assertTrue(line.parties().get(0).roles().contains("RECEIVER"));
    }

    @Test
    void shouldAddLineToExistingDraftOrder() {
        // given
        OrderView created =
                facade.handle(simpleOrderCommand("LAPTOP-DELL-5540", 1, "pieces")).getSuccess();

        // when
        Result<String, OrderView> result =
                facade.handle(
                        new AddOrderLineCommand(
                                created.id(), "MOUSE-LOGITECH-MX3", 2, "pieces", Map.of()));

        // then
        assertTrue(result.success());
        assertEquals(2, result.getSuccess().lines().size());
    }

    @Test
    void shouldRemoveLineFromOrder() {
        // given
        CreateOrderCommand command =
                new CreateOrderCommand(
                        defaultParties(),
                        List.of(
                                new CreateOrderCommand.OrderLineData(
                                        "LAPTOP-DELL-5540", 1, "pieces", Map.of(), null),
                                new CreateOrderCommand.OrderLineData(
                                        "MOUSE-LOGITECH-MX3", 2, "pieces", Map.of(), null)));
        OrderView created = facade.handle(command).getSuccess();
        OrderLineId lineToRemove = created.lines().get(1).id();

        // when
        Result<String, OrderView> result =
                facade.handle(new RemoveOrderLineCommand(created.id(), lineToRemove));

        // then
        assertTrue(result.success());
        assertEquals(1, result.getSuccess().lines().size());
        assertEquals("LAPTOP-DELL-5540", result.getSuccess().lines().get(0).productId());
    }

    @Test
    void shouldChangeLineQuantity() {
        // given
        OrderView created =
                facade.handle(simpleOrderCommand("SCREW-M6-50MM", 100, "pieces")).getSuccess();
        OrderLineId lineId = created.lines().get(0).id();

        // when
        Result<String, OrderView> result =
                facade.handle(
                        new ChangeOrderLineQuantityCommand(created.id(), lineId, 500, "pieces"));

        // then
        assertTrue(result.success());
        assertTrue(result.getSuccess().lines().get(0).quantity().contains("500"));
    }

    @Test
    void shouldConfirmDraftOrder() {
        // given
        OrderView created =
                facade.handle(simpleOrderCommand("APPLE-IPHONE-15-PRO", 1, "pieces")).getSuccess();

        // when
        Result<String, OrderView> result = facade.handle(new ConfirmOrderCommand(created.id()));

        // then
        assertTrue(result.success());
        assertEquals("CONFIRMED", result.getSuccess().status());
    }

    @Test
    void shouldCancelDraftOrder() {
        // given
        OrderView created =
                facade.handle(simpleOrderCommand("APPLE-IPHONE-15-PRO", 1, "pieces")).getSuccess();

        // when
        Result<String, OrderView> result =
                facade.handle(new CancelOrderCommand(created.id(), "Changed my mind"));

        // then
        assertTrue(result.success());
        assertEquals("CANCELLED", result.getSuccess().status());
    }

    @Test
    void shouldCancelConfirmedOrder() {
        // given
        OrderView created =
                facade.handle(simpleOrderCommand("APPLE-IPHONE-15-PRO", 1, "pieces")).getSuccess();
        facade.handle(new ConfirmOrderCommand(created.id()));

        // when
        Result<String, OrderView> result =
                facade.handle(new CancelOrderCommand(created.id(), "Customer request"));

        // then
        assertTrue(result.success());
        assertEquals("CANCELLED", result.getSuccess().status());
    }

    @Test
    void shouldFailToAddLineToConfirmedOrder() {
        // given
        OrderView created =
                facade.handle(simpleOrderCommand("APPLE-IPHONE-15-PRO", 1, "pieces")).getSuccess();
        facade.handle(new ConfirmOrderCommand(created.id()));

        // when
        Result<String, OrderView> result =
                facade.handle(
                        new AddOrderLineCommand(
                                created.id(), "MOUSE-LOGITECH-MX3", 1, "pieces", Map.of()));

        // then
        assertTrue(result.failure());
    }

    @Test
    void shouldFailToConfirmAlreadyConfirmedOrder() {
        // given
        OrderView created =
                facade.handle(simpleOrderCommand("APPLE-IPHONE-15-PRO", 1, "pieces")).getSuccess();
        facade.handle(new ConfirmOrderCommand(created.id()));

        // when
        Result<String, OrderView> result = facade.handle(new ConfirmOrderCommand(created.id()));

        // then
        assertTrue(result.failure());
    }

    @Test
    void shouldFailToCreateOrderWithoutLines() {
        // given
        CreateOrderCommand command = new CreateOrderCommand(defaultParties(), List.of());

        // when
        Result<String, OrderView> result = facade.handle(command);

        // then
        assertTrue(result.failure());
    }

    @Test
    void shouldFailToCreateOrderWithInvalidRoles() {
        // given - missing EXECUTOR
        CreateOrderCommand command =
                new CreateOrderCommand(
                        List.of(
                                new CreateOrderCommand.OrderPartyData(
                                        "customer-123",
                                        "John Doe",
                                        "john@example.com",
                                        Set.of("ORDERER", "PAYER", "RECEIVER"))),
                        List.of(
                                new CreateOrderCommand.OrderLineData(
                                        "LAPTOP", 1, "pieces", Map.of(), null)));

        // when
        Result<String, OrderView> result = facade.handle(command);

        // then
        assertTrue(result.failure());
    }

    @Test
    void shouldFailToRemoveLastLine() {
        // given
        OrderView created =
                facade.handle(simpleOrderCommand("LAPTOP-DELL-5540", 1, "pieces")).getSuccess();
        OrderLineId lineId = created.lines().get(0).id();

        // when
        Result<String, OrderView> result =
                facade.handle(new RemoveOrderLineCommand(created.id(), lineId));

        // then
        assertTrue(result.failure());
    }

    @Test
    void shouldFindOrderById() {
        // given
        OrderView created =
                facade.handle(simpleOrderCommand("LAPTOP-DELL-5540", 1, "pieces")).getSuccess();

        // when
        OrderView found = queries.findById(created.id()).orElseThrow();

        // then
        assertEquals(created.id(), found.id());
        assertEquals("DRAFT", found.status());
    }

    @Test
    void shouldFindAllOrders() {
        // given
        facade.handle(simpleOrderCommand("LAPTOP-DELL-5540", 1, "pieces"));
        facade.handle(simpleOrderCommand("MOUSE-LOGITECH-MX3", 2, "pieces"));

        // when
        List<OrderView> all = queries.findAll();

        // then
        assertEquals(2, all.size());
    }

    // --- Behavioral facade tests with service integrations ---

    @Test
    void shouldConfirmOrderWithInventoryAndPayment() {
        // given - default fixable services succeed
        OrderView created =
                facade.handle(simpleOrderCommand("LAPTOP-DELL-5540", 1, "pieces")).getSuccess();

        // when
        Result<String, OrderView> result = facade.handle(new ConfirmOrderCommand(created.id()));

        // then
        assertTrue(result.success());
        assertEquals("CONFIRMED", result.getSuccess().status());
        assertEquals(1, configuration.inventoryService().allocateRequests().size());
        assertEquals(1, configuration.paymentService().authorizeRequests().size());
        assertEquals(1, configuration.fulfillmentService().startedOrders().size());
    }

    @Test
    void shouldCancelConfirmedOrderWithCompensation() {
        // given
        OrderView created =
                facade.handle(simpleOrderCommand("LAPTOP-DELL-5540", 1, "pieces")).getSuccess();
        facade.handle(new ConfirmOrderCommand(created.id()));

        // when
        Result<String, OrderView> result =
                facade.handle(new CancelOrderCommand(created.id(), "Customer changed mind"));

        // then
        assertTrue(result.success());
        assertEquals("CANCELLED", result.getSuccess().status());
        assertEquals(1, configuration.fulfillmentService().cancelledOrders().size());
    }

    @Test
    void shouldHandleFulfillmentUpdatedEvent() {
        // given
        OrderView created =
                facade.handle(simpleOrderCommand("LAPTOP-DELL-5540", 1, "pieces")).getSuccess();
        facade.handle(new ConfirmOrderCommand(created.id()));

        // when
        Result<String, OrderView> result =
                facade.handle(
                        new FulfillmentUpdated(
                                created.id(),
                                FulfillmentStatus.IN_PROGRESS,
                                "Picking started",
                                LocalDateTime.now()));

        // then
        assertTrue(result.success());
        assertEquals("PROCESSING", result.getSuccess().status());
    }

    @Test
    void shouldHandleFullOrderLifecycle() {
        // given
        OrderView created =
                facade.handle(simpleOrderCommand("LAPTOP-DELL-5540", 1, "pieces")).getSuccess();
        assertEquals("DRAFT", created.status());

        // when - confirm
        facade.handle(new ConfirmOrderCommand(created.id()));
        OrderView confirmed = queries.findById(created.id()).orElseThrow();
        assertEquals("CONFIRMED", confirmed.status());

        // when - fulfillment starts
        facade.handle(
                new FulfillmentUpdated(
                        created.id(),
                        FulfillmentStatus.IN_PROGRESS,
                        "Picking",
                        LocalDateTime.now()));
        OrderView processing = queries.findById(created.id()).orElseThrow();
        assertEquals("PROCESSING", processing.status());

        // when - fulfillment completes
        facade.handle(
                new FulfillmentUpdated(
                        created.id(),
                        FulfillmentStatus.COMPLETED,
                        "Delivered",
                        LocalDateTime.now()));
        OrderView fulfilled = queries.findById(created.id()).orElseThrow();
        assertEquals("FULFILLED", fulfilled.status());
    }

    @Test
    void shouldFailToConfirmWhenInventoryUnavailable() {
        // given
        configuration.inventoryService().willFailOnAllocate();
        OrderView created =
                facade.handle(simpleOrderCommand("LAPTOP-DELL-5540", 1, "pieces")).getSuccess();

        // when
        Result<String, OrderView> result = facade.handle(new ConfirmOrderCommand(created.id()));

        // then
        assertTrue(result.failure());

        // and order stays DRAFT
        OrderView order = queries.findById(created.id()).orElseThrow();
        assertEquals("DRAFT", order.status());
    }

    @Test
    void shouldFailToConfirmWhenPaymentFails() {
        // given
        configuration.paymentService().willFailOnPayment("Insufficient funds");
        OrderView created =
                facade.handle(simpleOrderCommand("LAPTOP-DELL-5540", 1, "pieces")).getSuccess();

        // when
        Result<String, OrderView> result = facade.handle(new ConfirmOrderCommand(created.id()));

        // then
        assertTrue(result.failure());

        // and order stays DRAFT
        OrderView order = queries.findById(created.id()).orElseThrow();
        assertEquals("DRAFT", order.status());
    }

    @Test
    void shouldFailToHandleFulfillmentOnDraftOrder() {
        // given
        OrderView created =
                facade.handle(simpleOrderCommand("LAPTOP-DELL-5540", 1, "pieces")).getSuccess();

        // when
        Result<String, OrderView> result =
                facade.handle(
                        new FulfillmentUpdated(
                                created.id(),
                                FulfillmentStatus.IN_PROGRESS,
                                "Picking",
                                LocalDateTime.now()));

        // then
        assertTrue(result.failure());
    }

    // --- Helper methods ---

    private CreateOrderCommand simpleOrderCommand(String productId, double quantity, String unit) {
        return new CreateOrderCommand(
                defaultParties(),
                List.of(
                        new CreateOrderCommand.OrderLineData(
                                productId, quantity, unit, Map.of(), null)));
    }

    private List<CreateOrderCommand.OrderPartyData> defaultParties() {
        return List.of(
                new CreateOrderCommand.OrderPartyData(
                        "customer-123",
                        "John Doe",
                        "john@example.com",
                        Set.of("ORDERER", "PAYER", "RECEIVER")),
                new CreateOrderCommand.OrderPartyData(
                        "shop-warsaw", "Warsaw Shop", "warsaw@shop.com", Set.of("EXECUTOR")));
    }
}
