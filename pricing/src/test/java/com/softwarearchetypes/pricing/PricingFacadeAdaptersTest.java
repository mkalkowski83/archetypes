package com.softwarearchetypes.pricing;

import static java.time.Clock.fixed;
import static org.junit.jupiter.api.Assertions.*;

import com.softwarearchetypes.quantity.money.Money;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class PricingFacadeAdaptersTest {

    static final Instant NOW =
            LocalDateTime.of(2025, 1, 15, 12, 50).atZone(ZoneId.systemDefault()).toInstant();
    static final Clock clock = fixed(NOW, ZoneId.systemDefault());
    private final PricingFacade facade = PricingConfiguration.inMemory(clock).pricingFacade();

    @Test
    void calculateTotal_shouldReturnDirectlyIfCalculatorReturnsTotal() {
        // Given: calculator that already returns TOTAL
        facade.addCalculator(
                "total-calc", CalculatorType.SIMPLE_FIXED, Parameters.of("amount", Money.pln(150)));

        // When: calculate total
        Money total = facade.calculateTotal("total-calc", Parameters.empty());

        // Then: returns same value (no conversion needed)
        assertEquals(Money.pln(150), total);
    }

    @Test
    void calculateTotal_shouldWrapUnitPriceCalculator() {
        // Given: calculator that returns UNIT
        facade.addCalculator(
                "unit-calc",
                CalculatorType.SIMPLE_FIXED,
                Parameters.of("amount", Money.pln(10), "interpretation", Interpretation.UNIT));

        // When: calculate total
        Money total =
                facade.calculateTotal("unit-calc", Parameters.of("quantity", new BigDecimal("15")));

        // Then: auto-wrapped to total (10 × 15 = 150)
        assertEquals(Money.pln(150), total);
    }

    @Test
    void calculateTotal_shouldWrapMarginalPriceCalculator() {
        // Given: calculator that returns MARGINAL
        facade.addCalculator(
                "marginal-calc",
                CalculatorType.SIMPLE_FIXED,
                Parameters.of("amount", Money.pln(10), "interpretation", Interpretation.MARGINAL));

        // When: calculate total
        Money total =
                facade.calculateTotal(
                        "marginal-calc", Parameters.of("quantity", new BigDecimal("5")));

        // Then: auto-wrapped to total (sum of 5 marginals = 50)
        assertEquals(Money.pln(50), total);
    }

    @Test
    void calculateUnitPrice_shouldReturnDirectlyIfCalculatorReturnsUnitPrice() {
        // Given: calculator that already returns UNIT
        facade.addCalculator(
                "unit-calc",
                CalculatorType.SIMPLE_FIXED,
                Parameters.of("amount", Money.pln(10), "interpretation", Interpretation.UNIT));

        // When: calculate unit price
        Money unit = facade.calculateUnitPrice("unit-calc", Parameters.empty());

        // Then: returns same value (no conversion needed)
        assertEquals(Money.pln(10), unit);
    }

    @Test
    void calculateUnitPrice_shouldWrapTotalPriceCalculator() {
        // Given: step function calculator returning TOTAL
        facade.addCalculator(
                "step-calc",
                CalculatorType.STEP_FUNCTION,
                Parameters.of(
                        "basePrice", Money.pln(100),
                        "stepSize", new BigDecimal("10"),
                        "stepIncrement", new BigDecimal("5")));

        // When: calculate unit price for 15 units
        // Total(15) = 100 + floor(15/10) × 5 = 105
        // Unit = 105 / 15 = 7
        Money unit =
                facade.calculateUnitPrice(
                        "step-calc", Parameters.of("quantity", new BigDecimal("15")));

        // Then: auto-wrapped to unit price
        assertEquals(Money.pln(7), unit);
    }

    @Test
    void calculateUnitPrice_shouldWrapMarginalPriceCalculator() {
        // Given: calculator that returns MARGINAL
        facade.addCalculator(
                "marginal-calc",
                CalculatorType.SIMPLE_FIXED,
                Parameters.of("amount", Money.pln(10), "interpretation", Interpretation.MARGINAL));

        // When: calculate unit price
        // Sum of 5 marginals = 50, unit = 50/5 = 10
        Money unit =
                facade.calculateUnitPrice(
                        "marginal-calc", Parameters.of("quantity", new BigDecimal("5")));

        // Then: auto-wrapped to unit price
        assertEquals(Money.pln(10), unit);
    }

    @Test
    void calculateMarginal_shouldReturnDirectlyIfCalculatorReturnsMarginal() {
        // Given: calculator that already returns MARGINAL
        facade.addCalculator(
                "marginal-calc",
                CalculatorType.SIMPLE_FIXED,
                Parameters.of("amount", Money.pln(10), "interpretation", Interpretation.MARGINAL));

        // When: calculate marginal
        Money marginal = facade.calculateMarginal("marginal-calc", Parameters.empty());

        // Then: returns same value (no conversion needed)
        assertEquals(Money.pln(10), marginal);
    }

    @Test
    void calculateMarginal_shouldWrapUnitPriceCalculator() {
        // Given: calculator that returns UNIT
        facade.addCalculator(
                "unit-calc",
                CalculatorType.SIMPLE_FIXED,
                Parameters.of("amount", Money.pln(10), "interpretation", Interpretation.UNIT));

        // When: calculate marginal
        Money marginal =
                facade.calculateMarginal(
                        "unit-calc", Parameters.of("quantity", new BigDecimal("5")));

        // Then: auto-wrapped to marginal (for constant unit price, marginal = unit)
        assertEquals(Money.pln(10), marginal);
    }

    @Test
    void calculateMarginal_shouldWrapTotalPriceCalculator() {
        // Given: step function calculator returning TOTAL
        facade.addCalculator(
                "step-calc",
                CalculatorType.STEP_FUNCTION,
                Parameters.of(
                        "basePrice", Money.pln(100),
                        "stepSize", new BigDecimal("1"),
                        "stepIncrement", new BigDecimal("5")));

        // When: calculate marginal for 11th unit
        // Total(11) = 155, Total(10) = 150
        // Marginal(11) = 155 - 150 = 5
        Money marginal =
                facade.calculateMarginal(
                        "step-calc", Parameters.of("quantity", new BigDecimal("11")));

        // Then: auto-wrapped to marginal price
        assertEquals(Money.pln(5), marginal);
    }

    @Test
    void facade_shouldNotAllowCreatingAdaptersDirectly() {
        // When/Then: attempting to create adapter directly should fail
        IllegalArgumentException ex =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                facade.addCalculator(
                                        "adapter",
                                        CalculatorType.UNIT_TO_TOTAL_ADAPTER,
                                        Parameters.empty()));
        assertTrue(ex.getMessage().contains("cannot be created directly"));
    }

    @Test
    void autoWrapping_shouldWorkWithComplexScenario() {
        // Given: step function with quantity discounts
        facade.addCalculator(
                "bulk",
                CalculatorType.STEP_FUNCTION,
                Parameters.of(
                        "basePrice", Money.pln(100),
                        "stepSize", new BigDecimal("10"),
                        "stepIncrement", new BigDecimal("5")));

        Parameters params = Parameters.of("quantity", new BigDecimal("25"));

        // When/Then: calculate different interpretations
        // Total(25) = 100 + floor(25/10) × 5 = 110
        Money total = facade.calculateTotal("bulk", params);
        assertEquals(Money.pln(110), total);

        // Unit(25) = 110 / 25 = 4.40
        Money unit = facade.calculateUnitPrice("bulk", params);
        assertEquals(Money.of(new BigDecimal("4.40"), "PLN"), unit);

        // Marginal(25) = Total(25) - Total(24) = 110 - 110 = 0
        Money marginal = facade.calculateMarginal("bulk", params);
        assertEquals(Money.pln(0), marginal);
    }
}
