package com.softwarearchetypes.pricing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softwarearchetypes.quantity.money.Money;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DiscretePointsCalculatorTest {

    @Test
    void shouldReturnPriceForDefinedQuantity() {
        // given
        Map<BigDecimal, Money> points = new HashMap<>();
        points.put(new BigDecimal("5"), Money.pln(100));
        points.put(new BigDecimal("10"), Money.pln(180));
        points.put(new BigDecimal("20"), Money.pln(350));

        DiscretePointsCalculator calculator =
                new DiscretePointsCalculator("Volume Discount", points);
        Parameters params = new Parameters(Map.of("quantity", new BigDecimal("10")));

        // when
        Money result = calculator.calculate(params);

        // then
        assertEquals(0, new BigDecimal("180.00").compareTo(result.value()));
    }

    @Test
    void shouldReturnCorrectPriceForAllDefinedPoints() {
        // given
        Map<BigDecimal, Money> points = new HashMap<>();
        points.put(new BigDecimal("5"), Money.pln(100));
        points.put(new BigDecimal("10"), Money.pln(180));
        points.put(new BigDecimal("20"), Money.pln(350));

        DiscretePointsCalculator calculator =
                new DiscretePointsCalculator("Volume Discount", points);

        // when & then - check all points
        Money result5 =
                calculator.calculate(new Parameters(Map.of("quantity", new BigDecimal("5"))));
        assertEquals(0, new BigDecimal("100.00").compareTo(result5.value()));

        Money result10 =
                calculator.calculate(new Parameters(Map.of("quantity", new BigDecimal("10"))));
        assertEquals(0, new BigDecimal("180.00").compareTo(result10.value()));

        Money result20 =
                calculator.calculate(new Parameters(Map.of("quantity", new BigDecimal("20"))));
        assertEquals(0, new BigDecimal("350.00").compareTo(result20.value()));
    }

    @Test
    void shouldThrowExceptionForUndefinedQuantity() {
        // given
        Map<BigDecimal, Money> points = new HashMap<>();
        points.put(new BigDecimal("5"), Money.pln(100));
        points.put(new BigDecimal("10"), Money.pln(180));
        points.put(new BigDecimal("20"), Money.pln(350));

        DiscretePointsCalculator calculator =
                new DiscretePointsCalculator("Volume Discount", points);
        Parameters params = new Parameters(Map.of("quantity", new BigDecimal("7")));

        // when & then
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> calculator.calculate(params));

        assertTrue(exception.getMessage().contains("7"));
        assertTrue(exception.getMessage().contains("not defined"));
    }

    @Test
    void shouldThrowExceptionForQuantityOutsideRange() {
        // given
        Map<BigDecimal, Money> points = new HashMap<>();
        points.put(new BigDecimal("5"), Money.pln(100));
        points.put(new BigDecimal("10"), Money.pln(180));

        DiscretePointsCalculator calculator =
                new DiscretePointsCalculator("Volume Discount", points);
        Parameters params = new Parameters(Map.of("quantity", new BigDecimal("100")));

        // when & then
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(params));
    }

    @Test
    void shouldThrowExceptionWhenQuantityParameterMissing() {
        // given
        Map<BigDecimal, Money> points = new HashMap<>();
        points.put(new BigDecimal("5"), Money.pln(100));

        DiscretePointsCalculator calculator =
                new DiscretePointsCalculator("Volume Discount", points);
        Parameters params = Parameters.empty();

        // when & then
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(params));
    }

    @Test
    void shouldReturnCorrectType() {
        // given
        Map<BigDecimal, Money> points = new HashMap<>();
        points.put(new BigDecimal("5"), Money.pln(100));

        DiscretePointsCalculator calculator =
                new DiscretePointsCalculator("Volume Discount", points);

        // when & then
        assertEquals(CalculatorType.DISCRETE_POINTS, calculator.getType());
    }

    @Test
    void shouldProvideDescription() {
        // given
        Map<BigDecimal, Money> points = new HashMap<>();
        points.put(new BigDecimal("5"), Money.pln(100));
        points.put(new BigDecimal("10"), Money.pln(180));

        DiscretePointsCalculator calculator =
                new DiscretePointsCalculator("Volume Discount", points);

        // when
        String description = calculator.describe();

        // then
        assertNotNull(description);
        assertTrue(description.contains("2")); // number of points
        assertTrue(description.toLowerCase().contains("discrete"));
    }

    @Test
    void shouldHandleSinglePoint() {
        // given
        Map<BigDecimal, Money> points = new HashMap<>();
        points.put(new BigDecimal("1"), Money.pln(50));

        DiscretePointsCalculator calculator = new DiscretePointsCalculator("Single Price", points);
        Parameters params = new Parameters(Map.of("quantity", new BigDecimal("1")));

        // when
        Money result = calculator.calculate(params);

        // then
        assertEquals(0, new BigDecimal("50.00").compareTo(result.value()));
    }
}
