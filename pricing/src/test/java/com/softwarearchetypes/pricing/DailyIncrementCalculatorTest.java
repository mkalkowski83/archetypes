package com.softwarearchetypes.pricing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softwarearchetypes.quantity.money.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DailyIncrementCalculatorTest {

    @Test
    void shouldCalculatePriceAtStartDate() {
        // given
        DailyIncrementCalculator calculator =
                new DailyIncrementCalculator(
                        "presale-pricing",
                        LocalDate.of(2024, 6, 1), // start date
                        Money.pln(1999), // start price
                        Money.pln(100) // daily increment
                        );

        Parameters params = new Parameters(Map.of("date", LocalDate.of(2024, 6, 1)));

        // when
        Money result = calculator.calculate(params);

        // then
        assertEquals(0, new BigDecimal("1999.00").compareTo(result.value()));
    }

    @Test
    void shouldCalculatePriceAfterOneDay() {
        // given
        DailyIncrementCalculator calculator =
                new DailyIncrementCalculator(
                        "presale-pricing",
                        LocalDate.of(2024, 6, 1),
                        Money.pln(1999),
                        Money.pln(100));

        Parameters params = new Parameters(Map.of("date", LocalDate.of(2024, 6, 2)));

        // when
        Money result = calculator.calculate(params);

        // then - base price + 1 day * 100 = 1999 + 100 = 2099
        assertEquals(0, new BigDecimal("2099.00").compareTo(result.value()));
    }

    @Test
    void shouldCalculatePriceAfterSevenDays() {
        // given
        DailyIncrementCalculator calculator =
                new DailyIncrementCalculator(
                        "presale-pricing",
                        LocalDate.of(2024, 6, 1),
                        Money.pln(1999),
                        Money.pln(100));

        Parameters params = new Parameters(Map.of("date", LocalDate.of(2024, 6, 8)));

        // when
        Money result = calculator.calculate(params);

        // then - base price + 7 days * 100 = 1999 + 700 = 2699
        assertEquals(0, new BigDecimal("2699.00").compareTo(result.value()));
    }

    @Test
    void shouldCalculatePriceAfterFourteenDays() {
        // given
        DailyIncrementCalculator calculator =
                new DailyIncrementCalculator(
                        "presale-pricing",
                        LocalDate.of(2024, 6, 1),
                        Money.pln(1999),
                        Money.pln(100));

        Parameters params = new Parameters(Map.of("date", LocalDate.of(2024, 6, 15)));

        // when
        Money result = calculator.calculate(params);

        // then - base price + 14 days * 100 = 1999 + 1400 = 3399
        assertEquals(0, new BigDecimal("3399.00").compareTo(result.value()));
    }

    @Test
    void shouldCalculatePriceBeforeStartDate() {
        // given
        DailyIncrementCalculator calculator =
                new DailyIncrementCalculator(
                        "presale-pricing",
                        LocalDate.of(2024, 6, 1),
                        Money.pln(1999),
                        Money.pln(100));

        // Date before start date
        Parameters params = new Parameters(Map.of("date", LocalDate.of(2024, 5, 31)));

        // when
        Money result = calculator.calculate(params);

        // then - base price + (-1 day) * 100 = 1999 - 100 = 1899
        assertEquals(0, new BigDecimal("1899.00").compareTo(result.value()));
    }

    @Test
    void shouldWorkWithDifferentCurrencies() {
        // given
        DailyIncrementCalculator calculator =
                new DailyIncrementCalculator(
                        "presale-pricing-eur",
                        LocalDate.of(2024, 6, 1),
                        Money.eur(199),
                        Money.eur(10));

        Parameters params = new Parameters(Map.of("date", LocalDate.of(2024, 6, 8)));

        // when
        Money result = calculator.calculate(params);

        // then - base price + 7 days * 10 = 199 + 70 = 269 EUR
        assertEquals(0, new BigDecimal("269.00").compareTo(result.value()));
        assertTrue(result.toString().contains("EUR"));
    }

    @Test
    void shouldThrowExceptionWhenDateParameterMissing() {
        // given
        DailyIncrementCalculator calculator =
                new DailyIncrementCalculator(
                        "presale-pricing",
                        LocalDate.of(2024, 6, 1),
                        Money.pln(1999),
                        Money.pln(100));

        Parameters params = Parameters.empty();

        // when & then
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(params));
    }

    @Test
    void shouldReturnCorrectType() {
        // given
        DailyIncrementCalculator calculator =
                new DailyIncrementCalculator(
                        "presale-pricing",
                        LocalDate.of(2024, 6, 1),
                        Money.pln(1999),
                        Money.pln(100));

        // when & then
        assertEquals(CalculatorType.DAILY_INCREMENT, calculator.getType());
    }

    @Test
    void shouldProvideDescription() {
        // given
        DailyIncrementCalculator calculator =
                new DailyIncrementCalculator(
                        "presale-pricing",
                        LocalDate.of(2024, 6, 1),
                        Money.pln(1999),
                        Money.pln(100));

        // when
        String description = calculator.describe();

        // then
        assertNotNull(description);
        assertTrue(description.contains("1999"));
        assertTrue(description.contains("100"));
        assertTrue(description.contains("2024-06-01"));
    }

    @Test
    void shouldHandleDecreasingPriceWithNegativeIncrement() {
        // given - price decreases over time
        DailyIncrementCalculator calculator =
                new DailyIncrementCalculator(
                        "clearance-sale",
                        LocalDate.of(2024, 6, 1),
                        Money.pln(5000),
                        Money.pln(-100) // negative increment = price goes down
                        );

        Parameters params = new Parameters(Map.of("date", LocalDate.of(2024, 6, 11)));

        // when
        Money result = calculator.calculate(params);

        // then - base price + 10 days * (-100) = 5000 - 1000 = 4000
        assertEquals(0, new BigDecimal("4000.00").compareTo(result.value()));
    }

    @Test
    void shouldProvideFormula() {
        // given
        DailyIncrementCalculator calculator =
                new DailyIncrementCalculator(
                        "presale-pricing",
                        LocalDate.of(2024, 6, 1),
                        Money.pln("1999"),
                        Money.pln("100"));

        // when
        String formula = calculator.formula();

        // then
        String expected =
                "f(date) = startPrice + daysFromStart × dailyIncrement\n"
                        + "where:\n"
                        + "  startDate = 2024-06-01\n"
                        + "  startPrice = PLN 1999\n"
                        + "  dailyIncrement = PLN 100";
        assertEquals(expected, formula);
    }
}
