package com.softwarearchetypes.ordering;

import static org.junit.jupiter.api.Assertions.*;

import com.softwarearchetypes.quantity.money.Money;
import java.util.List;
import org.junit.jupiter.api.Test;

class OrderLinePricingTest {

    @Test
    void shouldBeDefinitiveForCalculatedPricing() {
        // given
        OrderLinePricing pricing = new CalculatedPricing(Money.pln(10), Money.pln(30));

        // then
        assertTrue(pricing.isDefinitive());
        assertEquals(Money.pln(10), pricing.unitPrice());
        assertEquals(Money.pln(30), pricing.totalPrice());
        assertTrue(pricing.breakdown().isEmpty());
    }

    @Test
    void shouldPreserveBreakdownComponentsInCalculatedPricing() {
        // given
        List<PriceBreakdown> breakdown =
                List.of(
                        new PriceBreakdown("base", Money.pln(20)),
                        new PriceBreakdown("tax", Money.pln(10)));

        // when
        OrderLinePricing pricing = new CalculatedPricing(Money.pln(10), Money.pln(30), breakdown);

        // then
        assertEquals(2, pricing.breakdown().size());
        assertEquals("base", pricing.breakdown().get(0).componentName());
        assertEquals(Money.pln(20), pricing.breakdown().get(0).amount());
        assertEquals("tax", pricing.breakdown().get(1).componentName());
        assertEquals(Money.pln(10), pricing.breakdown().get(1).amount());
    }

    @Test
    void shouldBeDefinitiveForArbitraryPricingWithReason() {
        // given
        ArbitraryPricing pricing =
                new ArbitraryPricing(Money.pln(50), Money.pln(150), "VIP discount");

        // then
        assertTrue(pricing.isDefinitive());
        assertEquals(Money.pln(50), pricing.unitPrice());
        assertEquals(Money.pln(150), pricing.totalPrice());
        assertTrue(pricing.breakdown().isEmpty());
        assertEquals("VIP discount", pricing.reason());
    }

    @Test
    void shouldNotBeDefinitiveForEstimatedPricing() {
        // given
        OrderLinePricing pricing = new EstimatedPricing(Money.pln(10), Money.pln(30));

        // then
        assertFalse(pricing.isDefinitive());
        assertEquals(Money.pln(10), pricing.unitPrice());
        assertEquals(Money.pln(30), pricing.totalPrice());
        assertTrue(pricing.breakdown().isEmpty());
    }

    @Test
    void shouldNotBeDefinitiveWhenNotPricedYet() {
        // given
        OrderLinePricing pricing = new NotPricedYet();

        // then
        assertFalse(pricing.isDefinitive());
        assertTrue(pricing.breakdown().isEmpty());
    }

    @Test
    void shouldThrowOnUnitPriceWhenNotPricedYet() {
        // given
        OrderLinePricing pricing = new NotPricedYet();

        // when/then
        assertThrows(UnsupportedOperationException.class, pricing::unitPrice);
    }

    @Test
    void shouldThrowOnTotalPriceWhenNotPricedYet() {
        // given
        OrderLinePricing pricing = new NotPricedYet();

        // when/then
        assertThrows(UnsupportedOperationException.class, pricing::totalPrice);
    }

    @Test
    void shouldPreserveChildrenInNestedBreakdown() {
        // given
        PriceBreakdown child1 = new PriceBreakdown("base", Money.pln(20));
        PriceBreakdown child2 = new PriceBreakdown("margin", Money.pln(10));

        // when
        PriceBreakdown parent = new PriceBreakdown("total", Money.pln(30), List.of(child1, child2));

        // then
        assertEquals(2, parent.children().size());
        assertEquals("base", parent.children().get(0).componentName());
        assertEquals(Money.pln(20), parent.children().get(0).amount());
        assertEquals("margin", parent.children().get(1).componentName());
        assertEquals(Money.pln(10), parent.children().get(1).amount());
        assertEquals(Money.pln(30), parent.amount());
    }

    @Test
    void shouldSupportPatternMatchingOnAllPricingStrategies() {
        // given
        OrderLinePricing pricing = new CalculatedPricing(Money.pln(10), Money.pln(30));

        // when
        String result =
                switch (pricing) {
                    case CalculatedPricing c -> "calculated: " + c.totalPrice();
                    case ArbitraryPricing a -> "arbitrary: " + a.reason();
                    case EstimatedPricing e -> "estimated: " + e.totalPrice();
                    case NotPricedYet n -> "not priced";
                };

        // then
        assertEquals("calculated: PLN 30", result);
    }

    @Test
    void shouldPreserveBreakdownInEstimatedPricing() {
        // given
        List<PriceBreakdown> breakdown =
                List.of(
                        new PriceBreakdown("labor", Money.pln(400)),
                        new PriceBreakdown("materials", Money.pln(100)));

        // when
        OrderLinePricing pricing = new EstimatedPricing(Money.pln(500), Money.pln(500), breakdown);

        // then
        assertFalse(pricing.isDefinitive());
        assertEquals(2, pricing.breakdown().size());
        assertEquals("labor", pricing.breakdown().get(0).componentName());
        assertEquals(Money.pln(400), pricing.breakdown().get(0).amount());
        assertEquals("materials", pricing.breakdown().get(1).componentName());
        assertEquals(Money.pln(100), pricing.breakdown().get(1).amount());
    }
}
