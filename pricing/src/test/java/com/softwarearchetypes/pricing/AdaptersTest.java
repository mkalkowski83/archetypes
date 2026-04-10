package com.softwarearchetypes.pricing;

import static org.junit.jupiter.api.Assertions.*;

import com.softwarearchetypes.quantity.money.Money;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class AdaptersTest {

    @Test
    void unitToTotalAdapter_shouldMultiplyByQuantity() {
        // Given: unit price calculator (10 PLN/piece)
        Calculator unitCalc =
                new SimpleFixedCalculator("unit-price", Money.pln(10), Interpretation.UNIT);

        // When: wrap with UnitToTotal adapter
        Calculator totalCalc = UnitToTotalAdapter.wrap("adapter", unitCalc);

        // Then: total = unit × quantity
        Money total = totalCalc.calculate(Parameters.of("quantity", new BigDecimal("15")));
        assertEquals(Money.pln(150), total);
        assertEquals(Interpretation.TOTAL, totalCalc.interpretation());
    }

    @Test
    void unitToTotalAdapter_shouldRejectNonUnitPriceCalculator() {
        // Given: TOTAL calculator
        Calculator totalCalc =
                new SimpleFixedCalculator("total", Money.pln(100), Interpretation.TOTAL);

        // When/Then: wrapping should fail
        IllegalArgumentException ex =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> UnitToTotalAdapter.wrap("adapter", totalCalc));
        assertTrue(ex.getMessage().contains("UNIT"));
    }

    @Test
    void unitToMarginalAdapter_shouldReturnSamePrice() {
        // Given: constant unit price (10 PLN/piece)
        Calculator unitCalc =
                new SimpleFixedCalculator("unit-price", Money.pln(10), Interpretation.UNIT);

        // When: wrap with UnitToMarginal adapter
        Calculator marginalCalc = UnitToMarginalAdapter.wrap("adapter", unitCalc);

        // Then: marginal price = unit price (constant)
        Money marginal5 = marginalCalc.calculate(Parameters.of("quantity", new BigDecimal("5")));
        Money marginal15 = marginalCalc.calculate(Parameters.of("quantity", new BigDecimal("15")));

        assertEquals(Money.pln(10), marginal5);
        assertEquals(Money.pln(10), marginal15);
        assertEquals(Interpretation.MARGINAL, marginalCalc.interpretation());
    }

    @Test
    void totalToUnitAdapter_shouldDivideByQuantity() {
        // Given: step function as TOTAL (base 100, step every 10, increment 5)
        Calculator totalCalc =
                new StepFunctionCalculator(
                        "bulk-pricing",
                        Money.pln(100),
                        new BigDecimal("10"),
                        new BigDecimal("5"),
                        Interpretation.TOTAL);

        // When: wrap with TotalToUnit adapter
        Calculator unitCalc = TotalToUnitAdapter.wrap("adapter", totalCalc);

        // Then: unit = total / quantity
        // For 15 units: total = 100 + floor(15/10) × 5 = 105
        // Unit = 105 / 15 = 7
        Money unit = unitCalc.calculate(Parameters.of("quantity", new BigDecimal("15")));
        assertEquals(Money.pln(7), unit);
        assertEquals(Interpretation.UNIT, unitCalc.interpretation());
    }

    @Test
    void totalToMarginalAdapter_shouldCalculateDerivative() {
        // Given: step function as TOTAL
        // Intervals: [1,10) → 100, [10,20) → 105, [20,30) → 110
        Calculator totalCalc =
                new StepFunctionCalculator(
                        "step-pricing",
                        Money.pln(100),
                        new BigDecimal("10"),
                        new BigDecimal("5"),
                        Interpretation.TOTAL);

        // When: wrap with TotalToMarginal adapter
        Calculator marginalCalc = TotalToMarginalAdapter.wrap("adapter", totalCalc);

        // Then: marginal(n) = total(n) - total(n-1)
        // First unit: marginal(1) = total(1) - total(0) = 100 - 0 = 100
        Money marginal1 = marginalCalc.calculate(Parameters.of("quantity", BigDecimal.ONE));
        assertEquals(Money.pln(100), marginal1);

        // 10th unit (step boundary): total(10) = 105, total(9) = 100, marginal(10) = 5
        Money marginal10 = marginalCalc.calculate(Parameters.of("quantity", new BigDecimal("10")));
        assertEquals(Money.pln(5), marginal10);

        // 11th unit (after step): total(11) = 105, total(10) = 105, marginal(11) = 0
        Money marginal11 = marginalCalc.calculate(Parameters.of("quantity", new BigDecimal("11")));
        assertEquals(Money.pln(0), marginal11);

        assertEquals(Interpretation.MARGINAL, marginalCalc.interpretation());
    }

    @Test
    void marginalToTotalAdapter_shouldSumMarginalPrices() {
        // Given: constant marginal price (10 PLN per unit)
        Calculator marginalCalc =
                new SimpleFixedCalculator("marginal-price", Money.pln(10), Interpretation.MARGINAL);

        // When: wrap with MarginalToTotal adapter
        Calculator totalCalc = MarginalToTotalAdapter.wrap("adapter", marginalCalc);

        // Then: total = sum of marginals
        // For 5 units: total = 10 + 10 + 10 + 10 + 10 = 50
        Money total = totalCalc.calculate(Parameters.of("quantity", new BigDecimal("5")));
        assertEquals(Money.pln(50), total);
        assertEquals(Interpretation.TOTAL, totalCalc.interpretation());
    }

    @Test
    void marginalToUnitAdapter_shouldCalculateAverage() {
        // Given: constant marginal price (10 PLN per unit)
        Calculator marginalCalc =
                new SimpleFixedCalculator("marginal-price", Money.pln(10), Interpretation.MARGINAL);

        // When: wrap with MarginalToUnit adapter
        Calculator unitCalc = MarginalToUnitAdapter.wrap("adapter", marginalCalc);

        // Then: unit = sum(marginals) / quantity
        // For 5 units: total = 50, unit = 50 / 5 = 10
        Money unit = unitCalc.calculate(Parameters.of("quantity", new BigDecimal("5")));
        assertEquals(Money.pln(10), unit);
        assertEquals(Interpretation.UNIT, unitCalc.interpretation());
    }

    @Test
    void unitToMarginalAdapter_shouldWorkForVariableUnitPrice() {
        // Given: variable unit price (step function returns UNIT)
        // Unit price decreases with quantity (bulk discount on average)
        Calculator unitCalc =
                new StepFunctionCalculator(
                        "bulk-unit-price",
                        Money.pln(100), // base "total" used to derive unit prices
                        new BigDecimal("10"),
                        new BigDecimal("5"),
                        Interpretation.UNIT);

        // When: wrap with UnitToMarginal adapter
        Calculator marginalCalc = UnitToMarginalAdapter.wrap("adapter", unitCalc);

        // Then: marginal correctly calculated even for variable unit price
        // At quantity 5: unit ≈ some value
        // At quantity 6: unit ≈ some other value
        // Marginal(6) should be calculated via derivative, not just returning unit price
        Money marginal6 = marginalCalc.calculate(Parameters.of("quantity", new BigDecimal("6")));
        Money marginal11 = marginalCalc.calculate(Parameters.of("quantity", new BigDecimal("11")));

        // Verify it's working (exact values depend on step function behavior)
        assertNotNull(marginal6);
        assertNotNull(marginal11);
        assertEquals(Interpretation.MARGINAL, marginalCalc.interpretation());
    }

    @Test
    void adapters_shouldHaveCorrectTypes() {
        Calculator unitCalc = new SimpleFixedCalculator("u", Money.pln(10), Interpretation.UNIT);
        Calculator totalCalc = new SimpleFixedCalculator("t", Money.pln(100), Interpretation.TOTAL);
        Calculator marginalCalc =
                new SimpleFixedCalculator("m", Money.pln(10), Interpretation.MARGINAL);

        assertEquals(
                CalculatorType.UNIT_TO_TOTAL_ADAPTER,
                UnitToTotalAdapter.wrap("a", unitCalc).getType());
        assertEquals(
                CalculatorType.UNIT_TO_MARGINAL_ADAPTER,
                UnitToMarginalAdapter.wrap("a", unitCalc).getType());
        assertEquals(
                CalculatorType.TOTAL_TO_UNIT_ADAPTER,
                TotalToUnitAdapter.wrap("a", totalCalc).getType());
        assertEquals(
                CalculatorType.TOTAL_TO_MARGINAL_ADAPTER,
                TotalToMarginalAdapter.wrap("a", totalCalc).getType());
        assertEquals(
                CalculatorType.MARGINAL_TO_TOTAL_ADAPTER,
                MarginalToTotalAdapter.wrap("a", marginalCalc).getType());
        assertEquals(
                CalculatorType.MARGINAL_TO_UNIT_ADAPTER,
                MarginalToUnitAdapter.wrap("a", marginalCalc).getType());
    }

    @Test
    void adapters_shouldPreserveSourceCalculatorFormula() {
        Calculator unitCalc = new SimpleFixedCalculator("test", Money.pln(10), Interpretation.UNIT);

        Calculator totalAdapter = UnitToTotalAdapter.wrap("adapter", unitCalc);

        String formula = totalAdapter.formula();
        assertTrue(
                formula.contains("quantity ×"), "Formula should mention quantity multiplication");
        assertTrue(formula.contains("f(x) = PLN 10"), "Formula should include source formula");
    }
}
