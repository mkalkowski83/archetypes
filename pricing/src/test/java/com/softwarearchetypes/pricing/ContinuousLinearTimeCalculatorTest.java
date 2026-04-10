package com.softwarearchetypes.pricing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softwarearchetypes.quantity.money.Money;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ContinuousLinearTimeCalculatorTest {

    @Test
    void shouldReturnStartPriceAtStartTime() {
        // given - auction from 1999 PLN to 3399 PLN over 14 days
        LocalDateTime startTime = LocalDateTime.of(2024, 6, 1, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(2024, 6, 15, 0, 0);

        ContinuousLinearTimeCalculator calculator =
                new ContinuousLinearTimeCalculator(
                        "auction-pricing", startTime, Money.pln(1999), endTime, Money.pln(3399));

        Parameters params = new Parameters(Map.of("time", startTime));

        // when
        Money result = calculator.calculate(params);

        // then
        assertEquals(0, new BigDecimal("1999.00").compareTo(result.value()));
    }

    @Test
    void shouldReturnEndPriceAtEndTime() {
        // given
        LocalDateTime startTime = LocalDateTime.of(2024, 6, 1, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(2024, 6, 15, 0, 0);

        ContinuousLinearTimeCalculator calculator =
                new ContinuousLinearTimeCalculator(
                        "auction-pricing", startTime, Money.pln(1999), endTime, Money.pln(3399));

        Parameters params = new Parameters(Map.of("time", endTime));

        // when
        Money result = calculator.calculate(params);

        // then
        assertEquals(0, new BigDecimal("3399.00").compareTo(result.value()));
    }

    @Test
    void shouldThrowExceptionForTimeBeforeStart() {
        // given
        LocalDateTime startTime = LocalDateTime.of(2024, 6, 1, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(2024, 6, 15, 0, 0);

        ContinuousLinearTimeCalculator calculator =
                new ContinuousLinearTimeCalculator(
                        "auction-pricing", startTime, Money.pln(1999), endTime, Money.pln(3399));

        // Query time before start
        LocalDateTime queryTime = LocalDateTime.of(2024, 5, 31, 12, 0);
        Parameters params = new Parameters(Map.of("time", queryTime));

        // when & then - should throw exception
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(params));
    }

    @Test
    void shouldThrowExceptionForTimeAfterEnd() {
        // given
        LocalDateTime startTime = LocalDateTime.of(2024, 6, 1, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(2024, 6, 15, 0, 0);

        ContinuousLinearTimeCalculator calculator =
                new ContinuousLinearTimeCalculator(
                        "auction-pricing", startTime, Money.pln(1999), endTime, Money.pln(3399));

        // Query time after end
        LocalDateTime queryTime = LocalDateTime.of(2024, 6, 16, 12, 0);
        Parameters params = new Parameters(Map.of("time", queryTime));

        // when & then - should throw exception
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(params));
    }

    @Test
    void shouldInterpolateAtMidpoint() {
        // given - from 1999 to 3399 over 14 days
        // Price range = 3399 - 1999 = 1400
        // Midpoint (50% progress) = 1999 + (1400 * 0.5) = 1999 + 700 = 2699
        LocalDateTime startTime = LocalDateTime.of(2024, 6, 1, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(2024, 6, 15, 0, 0);

        ContinuousLinearTimeCalculator calculator =
                new ContinuousLinearTimeCalculator(
                        "auction-pricing", startTime, Money.pln(1999), endTime, Money.pln(3399));

        // Exactly 7 days later (50% of 14 days)
        LocalDateTime midTime = LocalDateTime.of(2024, 6, 8, 0, 0);
        Parameters params = new Parameters(Map.of("time", midTime));

        // when
        Money result = calculator.calculate(params);

        // then - should be 2699 (midpoint)
        assertEquals(0, new BigDecimal("2699.00").compareTo(result.value()));
    }

    @Test
    void shouldInterpolateAtHalfDayWithPrecision() {
        // given - from 1999 to 3399 over 14 days
        // At 12:00 on day 0 (12 hours = 0.5 days out of 14 days)
        // Progress = 0.5 / 14 = 0.0357142857...
        // Price = 1999 + (1400 * 0.0357142857) = 1999 + 50 = 2049
        LocalDateTime startTime = LocalDateTime.of(2024, 6, 1, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(2024, 6, 15, 0, 0);

        ContinuousLinearTimeCalculator calculator =
                new ContinuousLinearTimeCalculator(
                        "auction-pricing", startTime, Money.pln(1999), endTime, Money.pln(3399));

        // 12 hours after start
        LocalDateTime queryTime = LocalDateTime.of(2024, 6, 1, 12, 0);
        Parameters params = new Parameters(Map.of("time", queryTime));

        // when
        Money result = calculator.calculate(params);

        // then - should be approximately 2049
        BigDecimal expected = new BigDecimal("2049");
        assertTrue(
                result.value().subtract(expected).abs().compareTo(new BigDecimal("1")) < 0,
                "Expected approximately 2049, got " + result.value());
    }

    @Test
    void shouldInterpolatePreciselyAtQuarterPoint() {
        // given - from 1999 to 3399 over 14 days
        // At 25% progress (3.5 days)
        // Price = 1999 + (1400 * 0.25) = 1999 + 350 = 2349
        LocalDateTime startTime = LocalDateTime.of(2024, 6, 1, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(2024, 6, 15, 0, 0);

        ContinuousLinearTimeCalculator calculator =
                new ContinuousLinearTimeCalculator(
                        "auction-pricing", startTime, Money.pln(1999), endTime, Money.pln(3399));

        // 3.5 days after start (84 hours)
        LocalDateTime queryTime = LocalDateTime.of(2024, 6, 4, 12, 0);
        Parameters params = new Parameters(Map.of("time", queryTime));

        // when
        Money result = calculator.calculate(params);

        // then - should be approximately 2349
        BigDecimal expected = new BigDecimal("2349");
        assertTrue(
                result.value().subtract(expected).abs().compareTo(new BigDecimal("1")) < 0,
                "Expected approximately 2349, got " + result.value());
    }

    @Test
    void shouldInterpolateAtThreeQuartersPoint() {
        // given - from 1999 to 3399 over 14 days
        // At 75% progress (10.5 days)
        // Price = 1999 + (1400 * 0.75) = 1999 + 1050 = 3049
        LocalDateTime startTime = LocalDateTime.of(2024, 6, 1, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(2024, 6, 15, 0, 0);

        ContinuousLinearTimeCalculator calculator =
                new ContinuousLinearTimeCalculator(
                        "auction-pricing", startTime, Money.pln(1999), endTime, Money.pln(3399));

        // 10.5 days after start
        LocalDateTime queryTime = LocalDateTime.of(2024, 6, 11, 12, 0);
        Parameters params = new Parameters(Map.of("time", queryTime));

        // when
        Money result = calculator.calculate(params);

        // then - should be approximately 3049
        BigDecimal expected = new BigDecimal("3049");
        assertTrue(
                result.value().subtract(expected).abs().compareTo(new BigDecimal("1")) < 0,
                "Expected approximately 3049, got " + result.value());
    }

    @Test
    void shouldInterpolateWithMinutePrecision() {
        // given - short time period to test minute-level precision
        // From 100 PLN to 200 PLN over 1 hour (60 minutes)
        LocalDateTime startTime = LocalDateTime.of(2024, 6, 1, 10, 0);
        LocalDateTime endTime = LocalDateTime.of(2024, 6, 1, 11, 0);

        ContinuousLinearTimeCalculator calculator =
                new ContinuousLinearTimeCalculator(
                        "minute-precision-test",
                        startTime,
                        Money.pln(100),
                        endTime,
                        Money.pln(200));

        // 30 minutes after start (50% progress)
        LocalDateTime queryTime = LocalDateTime.of(2024, 6, 1, 10, 30);
        Parameters params = new Parameters(Map.of("time", queryTime));

        // when
        Money result = calculator.calculate(params);

        // then - should be 150 (midpoint)
        assertEquals(0, new BigDecimal("150.00").compareTo(result.value()));
    }

    @Test
    void shouldWorkWithDifferentCurrencies() {
        // given - EUR auction
        LocalDateTime startTime = LocalDateTime.of(2024, 6, 1, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(2024, 6, 15, 0, 0);

        ContinuousLinearTimeCalculator calculator =
                new ContinuousLinearTimeCalculator(
                        "auction-pricing-eur", startTime, Money.eur(199), endTime, Money.eur(339));

        // At midpoint (7 days)
        LocalDateTime midTime = LocalDateTime.of(2024, 6, 8, 0, 0);
        Parameters params = new Parameters(Map.of("time", midTime));

        // when
        Money result = calculator.calculate(params);

        // then - should be 269 EUR (midpoint of 199 and 339)
        assertEquals(0, new BigDecimal("269.00").compareTo(result.value()));
        assertTrue(result.toString().contains("EUR"));
    }

    @Test
    void shouldHandleDecreasingPrice() {
        // given - price goes down over time
        LocalDateTime startTime = LocalDateTime.of(2024, 6, 1, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(2024, 6, 15, 0, 0);

        ContinuousLinearTimeCalculator calculator =
                new ContinuousLinearTimeCalculator(
                        "clearance-auction",
                        startTime,
                        Money.pln(5000), // starts high
                        endTime,
                        Money.pln(1000) // ends low
                        );

        // At midpoint (7 days)
        LocalDateTime midTime = LocalDateTime.of(2024, 6, 8, 0, 0);
        Parameters params = new Parameters(Map.of("time", midTime));

        // when
        Money result = calculator.calculate(params);

        // then - should be 3000 (midpoint of 5000 and 1000)
        assertEquals(0, new BigDecimal("3000.00").compareTo(result.value()));
    }

    @Test
    void shouldThrowExceptionWhenTimeParameterMissing() {
        // given
        LocalDateTime startTime = LocalDateTime.of(2024, 6, 1, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(2024, 6, 15, 0, 0);

        ContinuousLinearTimeCalculator calculator =
                new ContinuousLinearTimeCalculator(
                        "auction-pricing", startTime, Money.pln(1999), endTime, Money.pln(3399));

        Parameters params = Parameters.empty();

        // when & then
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(params));
    }

    @Test
    void shouldReturnCorrectType() {
        // given
        LocalDateTime startTime = LocalDateTime.of(2024, 6, 1, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(2024, 6, 15, 0, 0);

        ContinuousLinearTimeCalculator calculator =
                new ContinuousLinearTimeCalculator(
                        "auction-pricing", startTime, Money.pln(1999), endTime, Money.pln(3399));

        // when & then
        assertEquals(CalculatorType.CONTINUOUS_LINEAR_TIME, calculator.getType());
    }

    @Test
    void shouldProvideDescription() {
        // given
        LocalDateTime startTime = LocalDateTime.of(2024, 6, 1, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(2024, 6, 15, 0, 0);

        ContinuousLinearTimeCalculator calculator =
                new ContinuousLinearTimeCalculator(
                        "auction-pricing", startTime, Money.pln(1999), endTime, Money.pln(3399));

        // when
        String description = calculator.describe();

        // then
        assertNotNull(description);
        assertTrue(description.contains("1999"));
        assertTrue(description.contains("3399"));
        assertTrue(description.contains("2024-06-01"));
        assertTrue(description.contains("2024-06-15"));
    }

    @Test
    void shouldHandleVeryShortTimeInterval() {
        // given - 1 minute interval
        LocalDateTime startTime = LocalDateTime.of(2024, 6, 1, 10, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(2024, 6, 1, 10, 1, 0);

        ContinuousLinearTimeCalculator calculator =
                new ContinuousLinearTimeCalculator(
                        "fast-auction", startTime, Money.pln(100), endTime, Money.pln(200));

        // 30 seconds after start (50% progress)
        LocalDateTime queryTime = LocalDateTime.of(2024, 6, 1, 10, 0, 30);
        Parameters params = new Parameters(Map.of("time", queryTime));

        // when
        Money result = calculator.calculate(params);

        // then - should be 150 (midpoint)
        assertEquals(0, new BigDecimal("150.00").compareTo(result.value()));
    }

    @Test
    void shouldProvideFormula() {
        // given
        LocalDateTime startTime = LocalDateTime.of(2024, 6, 1, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(2024, 6, 15, 0, 0);

        ContinuousLinearTimeCalculator calculator =
                new ContinuousLinearTimeCalculator(
                        "auction-pricing",
                        startTime,
                        Money.pln("1999"),
                        endTime,
                        Money.pln("3399"));

        // when
        String formula = calculator.formula();

        // then
        String expected =
                "f(t) = startPrice + progress × (endPrice - startPrice)\n"
                        + "where progress = (t - startTime) / (endTime - startTime)\n"
                        + "domain: t ∈ [2024-06-01T00:00, 2024-06-15T00:00]";
        assertEquals(expected, formula);
    }
}
