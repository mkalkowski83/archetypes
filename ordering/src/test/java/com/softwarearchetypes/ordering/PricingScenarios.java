package com.softwarearchetypes.ordering;

import static org.junit.jupiter.api.Assertions.*;

import com.softwarearchetypes.common.Result;
import com.softwarearchetypes.ordering.commands.*;
import com.softwarearchetypes.quantity.money.Money;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PricingScenarios {

    private final OrderingConfiguration config = OrderingConfiguration.inMemory();
    private final OrderingFacade facade = config.orderingFacade();
    private final FixablePricingService pricingService = config.pricingService();
    private final FixableInventoryService inventoryService = config.inventoryService();
    private final FixablePaymentService paymentService = config.paymentService();

    @Test
    void shouldNotHavePriceWhenOrderLineIsCreated() {
        // given
        OrderView order = createSingleLineOrder("PHONE-1", 2);

        // then
        assertEquals("NotPricedYet", order.lines().get(0).pricingType());
        assertNull(order.lines().get(0).unitPrice());
        assertNull(order.lines().get(0).totalPrice());
        assertTrue(order.lines().get(0).breakdown().isEmpty());
        assertNull(order.totalPrice());
    }

    @Test
    void shouldCalculatePricingForAllLinesWhenPriceOrderIsHandled() {
        // given
        OrderView order = createTwoLineOrder();
        pricingService.willCalculate(Money.pln(100), Money.pln(200));

        // when
        Result<String, OrderView> result = facade.handle(new PriceOrderCommand(order.id()));

        // then
        assertTrue(result.success());
        OrderView priced = result.getSuccess();
        assertEquals("CalculatedPricing", priced.lines().get(0).pricingType());
        assertEquals("PLN 100", priced.lines().get(0).unitPrice());
        assertEquals("PLN 200", priced.lines().get(0).totalPrice());
        assertEquals("CalculatedPricing", priced.lines().get(1).pricingType());
        assertEquals("PLN 100", priced.lines().get(1).unitPrice());
        assertEquals("PLN 200", priced.lines().get(1).totalPrice());
        assertEquals("PLN 400", priced.totalPrice());
    }

    @Test
    void shouldSendProductIdAndQuantityInPricingContext() {
        // given
        OrderView order = createSingleLineOrder("LAPTOP-1", 3);
        pricingService.willCalculate(Money.pln(3000), Money.pln(9000));

        // when
        facade.handle(new PriceOrderCommand(order.id()));

        // then
        assertEquals(1, pricingService.calculateRequests().size());
        PricingContext ctx = pricingService.calculateRequests().get(0);
        assertEquals(ProductIdentifier.of("LAPTOP-1"), ctx.productId());
        assertNotNull(ctx.quantity());
        assertNotNull(ctx.parties());
        assertNotNull(ctx.specification());
        assertNotNull(ctx.pricingTime());
    }

    @Test
    void shouldApplyDifferentPricesPerLine() {
        // given
        OrderView order = createTwoLineOrder();
        pricingService.willAnswer(
                ctx -> {
                    if (ctx.productId().equals(ProductIdentifier.of("PHONE-1"))) {
                        return new CalculatedPricing(Money.pln(1000), Money.pln(2000));
                    } else {
                        return new CalculatedPricing(Money.pln(50), Money.pln(150));
                    }
                });

        // when
        Result<String, OrderView> result = facade.handle(new PriceOrderCommand(order.id()));

        // then
        assertTrue(result.success());
        OrderView priced = result.getSuccess();
        assertEquals("PLN 1000", priced.lines().get(0).unitPrice());
        assertEquals("PLN 2000", priced.lines().get(0).totalPrice());
        assertEquals("PLN 50", priced.lines().get(1).unitPrice());
        assertEquals("PLN 150", priced.lines().get(1).totalPrice());
        assertEquals("PLN 2150", priced.totalPrice());
    }

    @Test
    void shouldSetArbitraryPriceOnOrderLine() {
        // given
        OrderView order = createSingleLineOrder("CUSTOM-1", 1);
        OrderLineId lineId = order.lines().get(0).id();

        // when
        Result<String, OrderView> result =
                facade.handle(
                        new SetArbitraryLinePriceCommand(
                                order.id(),
                                lineId,
                                new BigDecimal("99.99"),
                                new BigDecimal("99.99"),
                                "PLN",
                                "Manager override"));

        // then
        assertTrue(result.success());
        OrderView updated = result.getSuccess();
        assertEquals("ArbitraryPricing", updated.lines().get(0).pricingType());
        assertEquals("PLN 99.99", updated.lines().get(0).unitPrice());
        assertEquals("PLN 99.99", updated.lines().get(0).totalPrice());
        assertTrue(updated.lines().get(0).breakdown().isEmpty());
    }

    @Test
    void shouldNotBeDefinitiveWhenEstimatedPricingIsApplied() {
        // given
        OrderView order = createSingleLineOrder("SERVICE-1", 1);
        pricingService.willEstimate(Money.pln(500), Money.pln(500));

        // when
        Result<String, OrderView> result = facade.handle(new PriceOrderCommand(order.id()));

        // then
        assertTrue(result.success());
        assertEquals("EstimatedPricing", result.getSuccess().lines().get(0).pricingType());
        assertEquals("PLN 500", result.getSuccess().lines().get(0).unitPrice());
        assertEquals("PLN 500", result.getSuccess().lines().get(0).totalPrice());
    }

    @Test
    void shouldResetPricingToNotPricedYetWhenQuantityChanges() {
        // given
        OrderView order = createSingleLineOrder("PHONE-1", 2);
        OrderLineId lineId = order.lines().get(0).id();
        pricingService.willCalculate(Money.pln(1000), Money.pln(2000));
        facade.handle(new PriceOrderCommand(order.id()));

        // when
        Result<String, OrderView> result =
                facade.handle(new ChangeOrderLineQuantityCommand(order.id(), lineId, 5, "pcs"));

        // then
        assertTrue(result.success());
        assertEquals("NotPricedYet", result.getSuccess().lines().get(0).pricingType());
        assertNull(result.getSuccess().lines().get(0).unitPrice());
        assertNull(result.getSuccess().totalPrice());
    }

    @Test
    void shouldConfirmPricedOrder() {
        // given
        OrderView order = createSingleLineOrder("PHONE-1", 2);
        pricingService.willCalculate(Money.pln(1000), Money.pln(2000));
        facade.handle(new PriceOrderCommand(order.id()));
        inventoryService.willReturnOnAllocate(AllocationStatus.ALLOCATED);

        // when
        Result<String, OrderView> confirmResult =
                facade.handle(new ConfirmOrderCommand(order.id()));

        // then
        assertTrue(confirmResult.success());
        assertEquals("CONFIRMED", confirmResult.getSuccess().status());
        assertEquals("PLN 2000", confirmResult.getSuccess().totalPrice());
    }

    @Test
    void shouldPreserveBreakdownComponentsAfterPricing() {
        // given
        OrderView order = createSingleLineOrder("BUNDLE-1", 1);
        pricingService.willCalculateWithBreakdown(
                Money.pln(300),
                Money.pln(300),
                List.of(
                        new PriceBreakdown("base", Money.pln(250)),
                        new PriceBreakdown("warranty", Money.pln(50))));

        // when
        Result<String, OrderView> result = facade.handle(new PriceOrderCommand(order.id()));

        // then
        assertTrue(result.success());
        OrderLineView line = result.getSuccess().lines().get(0);
        assertEquals("CalculatedPricing", line.pricingType());
        assertEquals("PLN 300", line.totalPrice());
        assertEquals(2, line.breakdown().size());
        assertEquals("base", line.breakdown().get(0).componentName());
        assertEquals("PLN 250", line.breakdown().get(0).amount());
        assertEquals("warranty", line.breakdown().get(1).componentName());
        assertEquals("PLN 50", line.breakdown().get(1).amount());
    }

    @Test
    void shouldCallPricingServiceOncePerLine() {
        // given
        OrderView order = createTwoLineOrder();
        pricingService.willCalculate(Money.pln(100), Money.pln(200));

        // when
        facade.handle(new PriceOrderCommand(order.id()));

        // then
        assertEquals(2, pricingService.calculateRequests().size());
        assertEquals(
                ProductIdentifier.of("PHONE-1"),
                pricingService.calculateRequests().get(0).productId());
        assertEquals(
                ProductIdentifier.of("MOUSE-1"),
                pricingService.calculateRequests().get(1).productId());
    }

    private OrderView createSingleLineOrder(String productId, int quantity) {
        Result<String, OrderView> result =
                facade.handle(
                        new CreateOrderCommand(
                                List.of(
                                        new CreateOrderCommand.OrderPartyData(
                                                "customer-1",
                                                "Customer",
                                                "c@test.com",
                                                Set.of("ORDERER", "PAYER", "RECEIVER")),
                                        new CreateOrderCommand.OrderPartyData(
                                                "executor-1", "Executor", "", Set.of("EXECUTOR"))),
                                List.of(
                                        new CreateOrderCommand.OrderLineData(
                                                productId, quantity, "pcs", Map.of(), List.of()))));
        assertTrue(result.success());
        return result.getSuccess();
    }

    private OrderView createTwoLineOrder() {
        Result<String, OrderView> result =
                facade.handle(
                        new CreateOrderCommand(
                                List.of(
                                        new CreateOrderCommand.OrderPartyData(
                                                "customer-1",
                                                "Customer",
                                                "c@test.com",
                                                Set.of("ORDERER", "PAYER", "RECEIVER")),
                                        new CreateOrderCommand.OrderPartyData(
                                                "executor-1", "Executor", "", Set.of("EXECUTOR"))),
                                List.of(
                                        new CreateOrderCommand.OrderLineData(
                                                "PHONE-1", 2, "pcs", Map.of(), List.of()),
                                        new CreateOrderCommand.OrderLineData(
                                                "MOUSE-1", 3, "pcs", Map.of(), List.of()))));
        assertTrue(result.success());
        return result.getSuccess();
    }
}
