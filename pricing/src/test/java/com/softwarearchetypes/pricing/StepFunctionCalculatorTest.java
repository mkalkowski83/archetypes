package com.softwarearchetypes.pricing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softwarearchetypes.quantity.money.Money;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StepFunctionCalculatorTest {

    @Test
    void shouldCalculateBasePriceForQuantityWithinFirstStep() {
        // given
        StepFunctionCalculator calculator =
                new StepFunctionCalculator(
                        "Volume Pricing",
                        Money.pln(100), // base price
                        new BigDecimal("10"), // step size
                        new BigDecimal("5") // step increment
                        );

        Parameters params = new Parameters(Map.of("quantity", new BigDecimal("5")));

        // when
        Money result = calculator.calculate(params);

        // then
        assertEquals(0, new BigDecimal("100.00").compareTo(result.value()));
    }

    @Test
    void shouldCalculateIncreasedPriceForSecondStep() {
        // given
        StepFunctionCalculator calculator =
                new StepFunctionCalculator(
                        "Volume Pricing",
                        Money.pln(100), // base price
                        new BigDecimal("10"), // step size
                        new BigDecimal("5") // step increment
                        );

        Parameters params = new Parameters(Map.of("quantity", new BigDecimal("15")));

        // when
        Money result = calculator.calculate(params);

        // then - base price + 1 step increment = 100 + 5 = 105
        assertEquals(0, new BigDecimal("105.00").compareTo(result.value()));
    }

    @Test
    void shouldCalculateIncreasedPriceForMultipleSteps() {
        // given
        StepFunctionCalculator calculator =
                new StepFunctionCalculator(
                        "Volume Pricing",
                        Money.pln(100), // base price
                        new BigDecimal("10"), // step size
                        new BigDecimal("5") // step increment
                        );

        Parameters params = new Parameters(Map.of("quantity", new BigDecimal("35")));

        // when
        Money result = calculator.calculate(params);

        // then - base price + 3 step increments = 100 + 15 = 115
        assertEquals(0, new BigDecimal("115.00").compareTo(result.value()));
    }

    @Test
    void shouldCalculatePriceAtExactStepBoundary() {
        // given
        StepFunctionCalculator calculator =
                new StepFunctionCalculator(
                        "Volume Pricing",
                        Money.pln(100), // base price
                        new BigDecimal("10"), // step size
                        new BigDecimal("5") // step increment
                        );

        Parameters params = new Parameters(Map.of("quantity", new BigDecimal("20")));

        // when
        Money result = calculator.calculate(params);

        // then - base price + 2 step increments = 100 + 10 = 110
        assertEquals(0, new BigDecimal("110.00").compareTo(result.value()));
    }

    @Test
    void shouldThrowExceptionWhenQuantityParameterMissing() {
        // given
        StepFunctionCalculator calculator =
                new StepFunctionCalculator(
                        "Volume Pricing",
                        Money.pln(100),
                        new BigDecimal("10"),
                        new BigDecimal("5"));

        Parameters params = Parameters.empty();

        // when & then
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(params));
    }

    @Test
    void shouldReturnCorrectType() {
        // given
        StepFunctionCalculator calculator =
                new StepFunctionCalculator(
                        "Volume Pricing",
                        Money.pln(100),
                        new BigDecimal("10"),
                        new BigDecimal("5"));

        // when & then
        assertEquals(CalculatorType.STEP_FUNCTION, calculator.getType());
    }

    @Test
    void shouldProvideDescription() {
        // given
        StepFunctionCalculator calculator =
                new StepFunctionCalculator(
                        "Volume Pricing",
                        Money.pln(100),
                        new BigDecimal("10"),
                        new BigDecimal("5"));

        // when
        String description = calculator.describe();

        // then
        assertNotNull(description);
        assertTrue(description.contains("100"));
        assertTrue(description.contains("10"));
    }
}
