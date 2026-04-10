package com.softwarearchetypes.ordering;

import static org.junit.jupiter.api.Assertions.*;

import com.softwarearchetypes.quantity.Quantity;
import com.softwarearchetypes.quantity.Unit;
import org.junit.jupiter.api.Test;

class OrderTest {

    private final FixableInventoryService inventoryService = new FixableInventoryService();
    private final FixablePaymentService paymentService = new FixablePaymentService();
    private final FixableFulfillmentService fulfillmentService = new FixableFulfillmentService();
    private final FixablePricingService pricingService = new FixablePricingService();
    private final OrderServices services =
            new OrderServices(pricingService, inventoryService, paymentService, fulfillmentService);

    @Test
    void shouldCreateOrderWithSingleLine() {
        // when
        Order order =
                Order.builder(
                                OrderId.generate(),
                                OrderParties.singleParty(
                                        PartySnapshot.of(
                                                PartyId.of("customer-123"),
                                                "John Doe",
                                                "john@example.com"),
                                        PartySnapshot.of(
                                                PartyId.of("shop-warsaw"),
                                                "Warsaw Shop",
                                                "warsaw@shop.com")),
                                services)
                        .addLine(
                                line ->
                                        line.productId(ProductIdentifier.of("APPLE-IPHONE-15-PRO"))
                                                .quantity(Quantity.of(1, Unit.pieces()))
                                                .specification(
                                                        spec ->
                                                                spec.add("color", "titanium-blue")
                                                                        .add("storage", "256GB")))
                        .build();

        // then
        assertEquals(OrderStatus.DRAFT, order.status());
        assertEquals(1, order.lines().size());

        OrderLine line = order.lines().get(0);
        assertEquals(ProductIdentifier.of("APPLE-IPHONE-15-PRO"), line.productId());
        assertEquals(Quantity.of(1, Unit.pieces()), line.quantity());
        assertEquals("titanium-blue", line.specification().get("color").orElseThrow());
        assertEquals("256GB", line.specification().get("storage").orElseThrow());
    }

    @Test
    void shouldCreateOrderWithMultipleLines() {
        // when
        Order order =
                Order.builder(
                                OrderId.generate(),
                                OrderParties.singleParty(
                                        PartySnapshot.of(
                                                PartyId.of("customer-456"),
                                                "Jane Smith",
                                                "jane@example.com"),
                                        PartySnapshot.of(
                                                PartyId.of("shop-warsaw"),
                                                "Warsaw Shop",
                                                "warsaw@shop.com")),
                                services)
                        .addLine(
                                line ->
                                        line.productId(ProductIdentifier.of("LAPTOP-DELL-5540"))
                                                .quantity(Quantity.of(1, Unit.pieces()))
                                                .specification(
                                                        spec ->
                                                                spec.add("color", "black")
                                                                        .add("ram", "16GB")))
                        .addLine(
                                line ->
                                        line.productId(ProductIdentifier.of("MOUSE-LOGITECH-MX3"))
                                                .quantity(Quantity.of(1, Unit.pieces())))
                        .build();

        // then
        assertEquals(2, order.lines().size());
    }

    @Test
    void shouldCreateOrderWithPackageComponents() {
        // when
        Order order =
                Order.builder(
                                OrderId.generate(),
                                OrderParties.singleParty(
                                        PartySnapshot.of(
                                                PartyId.of("customer-789"),
                                                "Tech Corp",
                                                "tech@corp.com"),
                                        PartySnapshot.of(
                                                PartyId.of("it-supplier"),
                                                "IT Supplier Inc",
                                                "supplier@it.com")),
                                services)
                        .addLine(
                                line ->
                                        line.productId(ProductIdentifier.of("PACKAGE-HOME-OFFICE"))
                                                .quantity(Quantity.of(1, Unit.packages()))
                                                .specification(
                                                        spec ->
                                                                spec.component(
                                                                                "laptop",
                                                                                "Dell-5540")
                                                                        .component(
                                                                                "mouse",
                                                                                "Logitech-MX3")
                                                                        .component(
                                                                                "bag", "Targus-15")
                                                                        .componentFeature(
                                                                                "laptop", "color",
                                                                                "black")
                                                                        .componentFeature(
                                                                                "laptop", "ram",
                                                                                "16GB")))
                        .build();

        // then
        OrderLine line = order.lines().get(0);
        assertEquals("Dell-5540", line.specification().components().get("laptop"));
        assertEquals("Logitech-MX3", line.specification().components().get("mouse"));
        assertEquals("Targus-15", line.specification().components().get("bag"));
    }

    @Test
    void shouldCreateOrderWithPreferences() {
        // when
        Order order =
                Order.builder(
                                OrderId.generate(),
                                OrderParties.singleParty(
                                        PartySnapshot.of(
                                                PartyId.of("customer-123"),
                                                "John Doe",
                                                "john@example.com"),
                                        PartySnapshot.of(
                                                PartyId.of("shop-warsaw"),
                                                "Warsaw Shop",
                                                "warsaw@shop.com")),
                                services)
                        .addLine(
                                line ->
                                        line.productId(ProductIdentifier.of("APPLE-IPHONE-15-PRO"))
                                                .quantity(Quantity.of(1, Unit.pieces()))
                                                .specification(
                                                        spec ->
                                                                spec.add("color", "blue")
                                                                        .preference(
                                                                                "warehouse",
                                                                                "warsaw-central")
                                                                        .preference(
                                                                                "deliveryDate",
                                                                                "2025-01-16")
                                                                        .preference(
                                                                                "giftWrap",
                                                                                "true")))
                        .build();

        // then
        OrderLine line = order.lines().get(0);
        assertEquals("warsaw-central", line.specification().preferences().get("warehouse"));
        assertEquals("2025-01-16", line.specification().preferences().get("deliveryDate"));
        assertEquals("true", line.specification().preferences().get("giftWrap"));
    }

    @Test
    void shouldCreateOrderWithConcreteInstanceReference() {
        // when
        Order order =
                Order.builder(
                                OrderId.generate(),
                                OrderParties.singleParty(
                                        PartySnapshot.of(
                                                PartyId.of("customer-123"),
                                                "BMW Buyer",
                                                "buyer@example.com"),
                                        PartySnapshot.of(
                                                PartyId.of("car-dealer"),
                                                "Premium Cars Dealer",
                                                "dealer@cars.com")),
                                services)
                        .addLine(
                                line ->
                                        line.productId(ProductIdentifier.of("BMW-X5-2024"))
                                                .quantity(Quantity.of(1, Unit.pieces()))
                                                .specification(
                                                        spec ->
                                                                spec.add(
                                                                        "vin",
                                                                        "WBA12345678901234")))
                        .build();

        // then
        OrderLine line = order.lines().get(0);
        assertEquals("WBA12345678901234", line.specification().get("vin").orElseThrow());
    }

    @Test
    void shouldCreateOrderWithTemporalResource() {
        // when
        Order order =
                Order.builder(
                                OrderId.generate(),
                                OrderParties.singleParty(
                                        PartySnapshot.of(
                                                PartyId.of("patient-789"),
                                                "Patient Mike",
                                                "mike@example.com"),
                                        PartySnapshot.of(
                                                PartyId.of("clinic-orthopedic"),
                                                "Orthopedic Clinic",
                                                "clinic@ortho.com")),
                                services)
                        .addLine(
                                line ->
                                        line.productId(
                                                        ProductIdentifier.of(
                                                                "SERVICE-ORTHO-CONSULTATION"))
                                                .quantity(Quantity.of(1, Unit.pieces()))
                                                .specification(
                                                        spec ->
                                                                spec.add("resourceId", "dr-smith")
                                                                        .add(
                                                                                "timeSlot",
                                                                                "2025-01-15T10:00/PT30M")
                                                                        .add(
                                                                                "location",
                                                                                "clinic-room-3")))
                        .build();

        // then
        OrderLine line = order.lines().get(0);
        assertEquals("dr-smith", line.specification().get("resourceId").orElseThrow());
        assertEquals("2025-01-15T10:00/PT30M", line.specification().get("timeSlot").orElseThrow());
    }

    @Test
    void shouldCreateOrderForOnDemandProduct() {
        // when
        Order order =
                Order.builder(
                                OrderId.generate(),
                                OrderParties.singleParty(
                                        PartySnapshot.of(
                                                PartyId.of("customer-123"),
                                                "Account Holder",
                                                "holder@example.com"),
                                        PartySnapshot.of(
                                                PartyId.of("bank-abc"),
                                                "ABC Bank",
                                                "bank@abc.com")),
                                services)
                        .addLine(
                                line ->
                                        line.productId(
                                                        ProductIdentifier.of(
                                                                "ACCOUNT-PERSONAL-STANDARD"))
                                                .quantity(Quantity.of(1, Unit.accounts()))
                                                .specification(
                                                        spec ->
                                                                spec.add("currency", "PLN")
                                                                        .add("package", "standard")
                                                                        .add(
                                                                                "initialDeposit",
                                                                                "500.00")
                                                                        .add(
                                                                                "branch",
                                                                                "warsaw-center")))
                        .build();

        // then
        OrderLine line = order.lines().get(0);
        assertEquals("PLN", line.specification().features().get("currency"));
        assertEquals("standard", line.specification().features().get("package"));
    }

    @Test
    void shouldUseShorthandForSimpleOrder() {
        // when
        Order order =
                Order.builder(
                                OrderId.generate(),
                                OrderParties.singleParty(
                                        PartySnapshot.of(
                                                PartyId.of("workshop-123"),
                                                "Workshop ABC",
                                                "workshop@abc.com"),
                                        PartySnapshot.of(
                                                PartyId.of("hardware-supplier"),
                                                "Hardware Supplier Co",
                                                "supplier@hardware.com")),
                                services)
                        .addLine(
                                ProductIdentifier.of("SCREW-M6-50MM"),
                                Quantity.of(5000, Unit.pieces()))
                        .build();

        // then
        assertEquals(1, order.lines().size());
        OrderLine line = order.lines().get(0);
        assertTrue(line.specification().attributes().isEmpty());
    }

    @Test
    void shouldThrowWhenOrderHasNoLines() {
        // when/then
        assertThrows(
                IllegalStateException.class,
                () ->
                        Order.builder(
                                        OrderId.generate(),
                                        OrderParties.singleParty(
                                                PartySnapshot.of(
                                                        PartyId.of("customer-123"),
                                                        "John Doe",
                                                        "john@example.com"),
                                                PartySnapshot.of(
                                                        PartyId.of("shop-warsaw"),
                                                        "Warsaw Shop",
                                                        "warsaw@shop.com")),
                                        services)
                                .build());
    }

    @Test
    void shouldCreateOrderWithLineLevelParties() {
        // given
        PartySnapshot customer =
                PartySnapshot.of(PartyId.of("customer-123"), "John Doe", "john@example.com");
        PartySnapshot shop =
                PartySnapshot.of(PartyId.of("shop-warsaw"), "Warsaw Shop", "warsaw@shop.com");
        PartySnapshot branch =
                PartySnapshot.of(PartyId.of("branch-cracow"), "Cracow Branch", "cracow@shop.com");
        PartySnapshot courier =
                PartySnapshot.of(PartyId.of("courier-1"), "Courier Bob", "bob@courier.com");

        // when
        Order order =
                Order.builder(
                                OrderId.generate(),
                                OrderParties.singleParty(customer, shop),
                                services)
                        .addLine(
                                line ->
                                        line.productId(ProductIdentifier.of("LAPTOP-DELL-5540"))
                                                .quantity(Quantity.of(1, Unit.pieces()))
                                                .specification(spec -> spec.add("color", "black"))
                                                .parties(
                                                        parties ->
                                                                parties.receiver(branch)
                                                                        .deliveryContact(courier)))
                        .build();

        // then
        assertEquals(1, order.lines().size());
        OrderLine line = order.lines().get(0);
        assertTrue(line.hasLineLevelParties());

        OrderParties effectiveParties = order.getEffectivePartiesFor(line);
        assertEquals(
                customer.partyId(), effectiveParties.partyWithRole(RoleInOrder.ORDERER).partyId());
        assertEquals(
                customer.partyId(), effectiveParties.partyWithRole(RoleInOrder.PAYER).partyId());
        assertEquals(
                shop.partyId(), effectiveParties.partyWithRole(RoleInOrder.EXECUTOR).partyId());
        assertEquals(
                branch.partyId(), effectiveParties.partyWithRole(RoleInOrder.RECEIVER).partyId());
        assertEquals(
                courier.partyId(),
                effectiveParties.partyWithRole(RoleInOrder.DELIVERY_CONTACT).partyId());
    }

    // --- Behavioral tests for Order aggregate ---

    @Test
    void shouldAddLineToOrder() {
        // given
        Order order = draftOrderWithOneLine();

        // when
        order.addLine(
                new OrderLine(
                        OrderLineId.generate(),
                        ProductIdentifier.of("MOUSE-LOGITECH-MX3"),
                        Quantity.of(2, Unit.pieces()),
                        OrderLineSpecification.empty(),
                        null));

        // then
        assertEquals(2, order.lines().size());
    }

    @Test
    void shouldFailToAddLineToConfirmedOrder() {
        // given
        Order order = draftOrderWithOneLine();
        configureServicesForConfirm();
        order.confirm();

        // when/then
        assertThrows(
                IllegalStateException.class,
                () ->
                        order.addLine(
                                new OrderLine(
                                        OrderLineId.generate(),
                                        ProductIdentifier.of("MOUSE"),
                                        Quantity.of(1, Unit.pieces()),
                                        OrderLineSpecification.empty(),
                                        null)));
    }

    @Test
    void shouldRemoveLineFromOrder() {
        // given
        Order order = draftOrderWithTwoLines();
        OrderLineId secondLineId = order.lines().get(1).id();

        // when
        order.removeLine(secondLineId);

        // then
        assertEquals(1, order.lines().size());
    }

    @Test
    void shouldFailToRemoveLastLine() {
        // given
        Order order = draftOrderWithOneLine();
        OrderLineId lineId = order.lines().get(0).id();

        // when/then
        assertThrows(IllegalStateException.class, () -> order.removeLine(lineId));
    }

    @Test
    void shouldRemoveLineFromConfirmedOrder() {
        // given
        Order order = draftOrderWithTwoLines();
        configureServicesForConfirm();
        order.confirm();
        OrderLineId lineId = order.lines().get(0).id();

        // when
        order.removeLine(lineId);

        // then
        assertEquals(1, order.lines().size());
    }

    @Test
    void shouldChangeLineQuantity() {
        // given
        Order order = draftOrderWithOneLine();
        OrderLineId lineId = order.lines().get(0).id();

        // when
        order.changeLineQuantity(lineId, Quantity.of(500, Unit.pieces()));

        // then
        assertEquals(Quantity.of(500, Unit.pieces()), order.lines().get(0).quantity());
    }

    @Test
    void shouldFailToChangeQuantityOnNonExistentLine() {
        // given
        Order order = draftOrderWithOneLine();

        // when/then
        assertThrows(
                IllegalStateException.class,
                () ->
                        order.changeLineQuantity(
                                OrderLineId.generate(), Quantity.of(10, Unit.pieces())));
    }

    @Test
    void shouldConfirmDraftOrder() {
        // given
        Order order = draftOrderWithOneLine();
        configureServicesForConfirm();

        // when
        order.confirm();

        // then
        assertEquals(OrderStatus.CONFIRMED, order.status());
    }

    @Test
    void shouldFailToConfirmNonDraftOrder() {
        // given
        Order order = draftOrderWithOneLine();
        configureServicesForConfirm();
        order.confirm();

        // when/then
        assertThrows(IllegalStateException.class, order::confirm);
    }

    @Test
    void shouldCancelDraftOrder() {
        // given
        Order order = draftOrderWithOneLine();

        // when
        order.cancel();

        // then
        assertEquals(OrderStatus.CANCELLED, order.status());
    }

    @Test
    void shouldCancelConfirmedOrder() {
        // given
        Order order = draftOrderWithOneLine();
        configureServicesForConfirm();
        order.confirm();

        // when
        order.cancel();

        // then
        assertEquals(OrderStatus.CANCELLED, order.status());
    }

    @Test
    void shouldFailToCancelAlreadyCancelledOrder() {
        // given
        Order order = draftOrderWithOneLine();
        order.cancel();

        // when/then
        assertThrows(IllegalStateException.class, order::cancel);
    }

    @Test
    void shouldUpdateFulfillmentStatusToProcessing() {
        // given
        Order order = draftOrderWithOneLine();
        configureServicesForConfirm();
        order.confirm();

        // when
        order.updateFulfillmentStatus(FulfillmentStatus.IN_PROGRESS);

        // then
        assertEquals(OrderStatus.PROCESSING, order.status());
    }

    @Test
    void shouldUpdateFulfillmentStatusToFulfilled() {
        // given
        Order order = draftOrderWithOneLine();
        configureServicesForConfirm();
        order.confirm();

        // when
        order.updateFulfillmentStatus(FulfillmentStatus.COMPLETED);

        // then
        assertEquals(OrderStatus.FULFILLED, order.status());
    }

    @Test
    void shouldFailToUpdateFulfillmentOnDraftOrder() {
        // given
        Order order = draftOrderWithOneLine();

        // when/then
        assertThrows(
                IllegalStateException.class,
                () -> order.updateFulfillmentStatus(FulfillmentStatus.IN_PROGRESS));
    }

    // --- Helper methods ---

    private void configureServicesForConfirm() {
        inventoryService.willReturnOnAllocate(AllocationStatus.ALLOCATED);
    }

    private Order draftOrderWithOneLine() {
        return Order.builder(
                        OrderId.generate(),
                        OrderParties.singleParty(
                                PartySnapshot.of(
                                        PartyId.of("customer-123"), "John Doe", "john@example.com"),
                                PartySnapshot.of(
                                        PartyId.of("shop-warsaw"),
                                        "Warsaw Shop",
                                        "warsaw@shop.com")),
                        services)
                .addLine(ProductIdentifier.of("LAPTOP-DELL-5540"), Quantity.of(1, Unit.pieces()))
                .build();
    }

    private Order draftOrderWithTwoLines() {
        return Order.builder(
                        OrderId.generate(),
                        OrderParties.singleParty(
                                PartySnapshot.of(
                                        PartyId.of("customer-123"), "John Doe", "john@example.com"),
                                PartySnapshot.of(
                                        PartyId.of("shop-warsaw"),
                                        "Warsaw Shop",
                                        "warsaw@shop.com")),
                        services)
                .addLine(ProductIdentifier.of("LAPTOP-DELL-5540"), Quantity.of(1, Unit.pieces()))
                .addLine(ProductIdentifier.of("MOUSE-LOGITECH-MX3"), Quantity.of(2, Unit.pieces()))
                .build();
    }
}
