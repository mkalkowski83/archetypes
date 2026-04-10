package com.softwarearchetypes.pricing;

import static java.time.Clock.fixed;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.softwarearchetypes.quantity.money.Money;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Tests for the simulate() method across different calculator types. Simulations test how
 * calculators behave over a range of input values.
 */
class SimulationTest {

    @Test
    void shouldSimulateStepFunctionCalculatorOverQuantityRange() {
        // given - step function: base 100 PLN + 5 PLN per 10 units
        StepFunctionCalculator calculator =
                new StepFunctionCalculator(
                        "volume-discount",
                        Money.pln(100),
                        new BigDecimal("10"),
                        new BigDecimal("5"));

        // Create simulation points: quantities 0, 5, 10, 15, 20, 25, 30
        List<Parameters> points = new ArrayList<>();
        for (int qty = 0; qty <= 30; qty += 5) {
            points.add(Parameters.of("quantity", new BigDecimal(qty)));
        }

        // when
        Map<Parameters, Money> results = calculator.simulate(points);

        // then - verify results
        assertNotNull(results);
        assertEquals(7, results.size());

        // 0-9 units: 100 PLN (0 complete steps)
        assertEquals(
                0,
                new BigDecimal("100.00")
                        .compareTo(
                                results.get(Parameters.of("quantity", BigDecimal.ZERO)).value()));
        assertEquals(
                0,
                new BigDecimal("100.00")
                        .compareTo(
                                results.get(Parameters.of("quantity", new BigDecimal("5")))
                                        .value()));

        // 10-19 units: 105 PLN (1 complete step)
        assertEquals(
                0,
                new BigDecimal("105.00")
                        .compareTo(
                                results.get(Parameters.of("quantity", new BigDecimal("10")))
                                        .value()));
        assertEquals(
                0,
                new BigDecimal("105.00")
                        .compareTo(
                                results.get(Parameters.of("quantity", new BigDecimal("15")))
                                        .value()));

        // 20-29 units: 110 PLN (2 complete steps)
        assertEquals(
                0,
                new BigDecimal("110.00")
                        .compareTo(
                                results.get(Parameters.of("quantity", new BigDecimal("20")))
                                        .value()));
        assertEquals(
                0,
                new BigDecimal("110.00")
                        .compareTo(
                                results.get(Parameters.of("quantity", new BigDecimal("25")))
                                        .value()));

        // 30-39 units: 115 PLN (3 complete steps)
        assertEquals(
                0,
                new BigDecimal("115.00")
                        .compareTo(
                                results.get(Parameters.of("quantity", new BigDecimal("30")))
                                        .value()));
    }

    @Test
    void shouldSimulateDailyIncrementCalculatorOverDateRange() {
        // given - presale pricing: 1999 PLN on June 1, +100 PLN per day
        LocalDate startDate = LocalDate.of(2024, 6, 1);
        DailyIncrementCalculator calculator =
                new DailyIncrementCalculator(
                        "presale-pricing", startDate, Money.pln(1999), Money.pln(100));

        // Create simulation points: every 2 days for 14 days
        List<Parameters> points = new ArrayList<>();
        for (int day = 0; day <= 14; day += 2) {
            points.add(Parameters.of("date", startDate.plusDays(day)));
        }

        // when
        Map<Parameters, Money> results = calculator.simulate(points);

        // then - verify price increases by 200 PLN every 2 days
        assertNotNull(results);
        assertEquals(8, results.size());

        assertEquals(
                0,
                new BigDecimal("1999.00")
                        .compareTo(results.get(Parameters.of("date", startDate)).value()));
        assertEquals(
                0,
                new BigDecimal("2199.00")
                        .compareTo(
                                results.get(Parameters.of("date", startDate.plusDays(2))).value()));
        assertEquals(
                0,
                new BigDecimal("2399.00")
                        .compareTo(
                                results.get(Parameters.of("date", startDate.plusDays(4))).value()));
        assertEquals(
                0,
                new BigDecimal("2599.00")
                        .compareTo(
                                results.get(Parameters.of("date", startDate.plusDays(6))).value()));
        assertEquals(
                0,
                new BigDecimal("2799.00")
                        .compareTo(
                                results.get(Parameters.of("date", startDate.plusDays(8))).value()));
        assertEquals(
                0,
                new BigDecimal("2999.00")
                        .compareTo(
                                results.get(Parameters.of("date", startDate.plusDays(10)))
                                        .value()));
        assertEquals(
                0,
                new BigDecimal("3199.00")
                        .compareTo(
                                results.get(Parameters.of("date", startDate.plusDays(12)))
                                        .value()));
        assertEquals(
                0,
                new BigDecimal("3399.00")
                        .compareTo(
                                results.get(Parameters.of("date", startDate.plusDays(14)))
                                        .value()));
    }

    @Test
    void shouldSimulateContinuousLinearTimeCalculator() {
        // given - auction: 1999 PLN to 3399 PLN over 14 days
        LocalDateTime startTime = LocalDateTime.of(2024, 6, 1, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(2024, 6, 15, 0, 0);

        ContinuousLinearTimeCalculator calculator =
                new ContinuousLinearTimeCalculator(
                        "auction-pricing", startTime, Money.pln(1999), endTime, Money.pln(3399));

        // Create simulation points: start, 25%, 50%, 75%, end
        List<Parameters> points =
                List.of(
                        Parameters.of("time", startTime), // 0%
                        Parameters.of("time", startTime.plusDays(3).plusHours(12)), // 25%
                        Parameters.of("time", startTime.plusDays(7)), // 50%
                        Parameters.of("time", startTime.plusDays(10).plusHours(12)), // 75%
                        Parameters.of("time", endTime) // 100%
                        );

        // when
        Map<Parameters, Money> results = calculator.simulate(points);

        // then - verify linear interpolation
        assertNotNull(results);
        assertEquals(5, results.size());

        // At start: 1999 PLN
        assertEquals(
                0,
                new BigDecimal("1999.00")
                        .compareTo(results.get(Parameters.of("time", startTime)).value()));

        // At 50%: 2699 PLN (midpoint)
        assertEquals(
                0,
                new BigDecimal("2699.00")
                        .compareTo(
                                results.get(Parameters.of("time", startTime.plusDays(7))).value()));

        // At end: 3399 PLN
        assertEquals(
                0,
                new BigDecimal("3399.00")
                        .compareTo(results.get(Parameters.of("time", endTime)).value()));

        // At 25%: approximately 2349 PLN
        Money at25Percent = results.get(Parameters.of("time", startTime.plusDays(3).plusHours(12)));
        assertEquals(
                0,
                new BigDecimal("2349")
                        .compareTo(
                                at25Percent.value().setScale(0, java.math.RoundingMode.HALF_UP)));

        // At 75%: approximately 3049 PLN
        Money at75Percent =
                results.get(Parameters.of("time", startTime.plusDays(10).plusHours(12)));
        assertEquals(
                0,
                new BigDecimal("3049")
                        .compareTo(
                                at75Percent.value().setScale(0, java.math.RoundingMode.HALF_UP)));
    }

    @Test
    void shouldSimulateSimpleFixedCalculator() {
        // given - fixed price
        SimpleFixedCalculator calculator = new SimpleFixedCalculator("flat-fee", Money.pln(50));

        // Create simulation points with different parameters (but all return same price)
        List<Parameters> points =
                List.of(
                        Parameters.empty(),
                        Parameters.of("quantity", new BigDecimal("1")),
                        Parameters.of("quantity", new BigDecimal("100")),
                        Parameters.of("anything", "value"));

        // when
        Map<Parameters, Money> results = calculator.simulate(points);

        // then - all results should be 50 PLN
        assertNotNull(results);
        assertEquals(4, results.size());

        results.values()
                .forEach(
                        price -> assertEquals(0, new BigDecimal("50.00").compareTo(price.value())));
    }

    @Test
    void shouldSimulateDiscretePointsCalculator() {
        // given - discrete pricing at specific quantities
        Map<BigDecimal, Money> pricePoints =
                Map.of(
                        new BigDecimal("5"), Money.pln(100),
                        new BigDecimal("10"), Money.pln(180),
                        new BigDecimal("20"), Money.pln(350));

        DiscretePointsCalculator calculator =
                new DiscretePointsCalculator("bulk-pricing", pricePoints);

        // Create simulation points for defined quantities only
        List<Parameters> points =
                List.of(
                        Parameters.of("quantity", new BigDecimal("5")),
                        Parameters.of("quantity", new BigDecimal("10")),
                        Parameters.of("quantity", new BigDecimal("20")));

        // when
        Map<Parameters, Money> results = calculator.simulate(points);

        // then
        assertNotNull(results);
        assertEquals(3, results.size());

        assertEquals(
                0,
                new BigDecimal("100.00")
                        .compareTo(
                                results.get(Parameters.of("quantity", new BigDecimal("5")))
                                        .value()));
        assertEquals(
                0,
                new BigDecimal("180.00")
                        .compareTo(
                                results.get(Parameters.of("quantity", new BigDecimal("10")))
                                        .value()));
        assertEquals(
                0,
                new BigDecimal("350.00")
                        .compareTo(
                                results.get(Parameters.of("quantity", new BigDecimal("20")))
                                        .value()));
    }

    @Test
    void shouldSimulateCompositeCalculator() {
        // given - composite calculator with income-based tiers
        Instant NOW =
                LocalDateTime.of(2025, 1, 15, 12, 50).atZone(ZoneId.systemDefault()).toInstant();
        Clock clock = fixed(NOW, ZoneId.systemDefault());
        PricingFacade facade = PricingConfiguration.inMemory(clock).pricingFacade();

        Calculator lowTier =
                facade.addCalculator(
                        "low-tier",
                        CalculatorType.SIMPLE_FIXED,
                        Parameters.of("amount", Money.pln(20)));

        Calculator mediumTier =
                facade.addCalculator(
                        "medium-tier",
                        CalculatorType.SIMPLE_FIXED,
                        Parameters.of("amount", Money.pln(10)));

        Calculator highTier =
                facade.addCalculator(
                        "high-tier",
                        CalculatorType.SIMPLE_FIXED,
                        Parameters.of("amount", Money.pln(0)));

        List<CalculatorRange> ranges =
                List.of(
                        new NumericRange(BigDecimal.ZERO, new BigDecimal("1000"), lowTier.getId()),
                        new NumericRange(
                                new BigDecimal("1000"), new BigDecimal("4000"), mediumTier.getId()),
                        new NumericRange(
                                new BigDecimal("4000"),
                                new BigDecimal(Integer.MAX_VALUE),
                                highTier.getId()));

        CompositeFunctionCalculator composite =
                (CompositeFunctionCalculator)
                        facade.addCalculator(
                                "account-fee",
                                CalculatorType.COMPOSITE,
                                Parameters.of("rangeSelector", "monthlyIncome", "ranges", ranges));

        // Create simulation points across all tiers
        List<Parameters> points =
                List.of(
                        Parameters.of("monthlyIncome", BigDecimal.ZERO),
                        Parameters.of("monthlyIncome", new BigDecimal("500")),
                        Parameters.of("monthlyIncome", new BigDecimal("1000")),
                        Parameters.of("monthlyIncome", new BigDecimal("2500")),
                        Parameters.of("monthlyIncome", new BigDecimal("4000")),
                        Parameters.of("monthlyIncome", new BigDecimal("10000")));

        // when
        Map<Parameters, Money> results = composite.simulate(points);

        // then
        assertNotNull(results);
        assertEquals(6, results.size());

        // Low tier: 20 PLN
        assertEquals(
                0,
                new BigDecimal("20.00")
                        .compareTo(
                                results.get(Parameters.of("monthlyIncome", BigDecimal.ZERO))
                                        .value()));
        assertEquals(
                0,
                new BigDecimal("20.00")
                        .compareTo(
                                results.get(Parameters.of("monthlyIncome", new BigDecimal("500")))
                                        .value()));

        // Medium tier: 10 PLN
        assertEquals(
                0,
                new BigDecimal("10.00")
                        .compareTo(
                                results.get(Parameters.of("monthlyIncome", new BigDecimal("1000")))
                                        .value()));
        assertEquals(
                0,
                new BigDecimal("10.00")
                        .compareTo(
                                results.get(Parameters.of("monthlyIncome", new BigDecimal("2500")))
                                        .value()));

        // High tier: 0 PLN
        assertEquals(
                0,
                BigDecimal.ZERO.compareTo(
                        results.get(Parameters.of("monthlyIncome", new BigDecimal("4000")))
                                .value()));
        assertEquals(
                0,
                BigDecimal.ZERO.compareTo(
                        results.get(Parameters.of("monthlyIncome", new BigDecimal("10000")))
                                .value()));
    }

    @Test
    void shouldPreserveOrderInSimulationResults() {
        // given
        StepFunctionCalculator calculator =
                new StepFunctionCalculator(
                        "test", Money.pln(100), new BigDecimal("10"), new BigDecimal("5"));

        // Create points in specific order
        List<Parameters> points =
                List.of(
                        Parameters.of("quantity", new BigDecimal("30")),
                        Parameters.of("quantity", new BigDecimal("10")),
                        Parameters.of("quantity", new BigDecimal("20")),
                        Parameters.of("quantity", BigDecimal.ZERO));

        // when
        Map<Parameters, Money> results = calculator.simulate(points);

        // then - verify order is preserved (LinkedHashMap)
        List<Parameters> resultKeys = new ArrayList<>(results.keySet());
        assertEquals(points.get(0), resultKeys.get(0));
        assertEquals(points.get(1), resultKeys.get(1));
        assertEquals(points.get(2), resultKeys.get(2));
        assertEquals(points.get(3), resultKeys.get(3));
    }

    @Test
    void shouldSimulateEmptyListOfPoints() {
        // given
        SimpleFixedCalculator calculator = new SimpleFixedCalculator("test", Money.pln(100));

        // when
        Map<Parameters, Money> results = calculator.simulate(List.of());

        // then
        assertNotNull(results);
        assertEquals(0, results.size());
    }
}
