package com.softwarearchetypes.quantity.money;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PercentageTest {

    @Test
    void shouldCreatePercentageFromInt() {
        // when
        Percentage percentage = Percentage.of(50);

        // then
        assertNotNull(percentage);
        assertEquals(0, new BigDecimal("50.00000").compareTo(percentage.value()));
    }

    @Test
    void shouldCreatePercentageFromBigDecimal() {
        // when
        Percentage percentage = Percentage.of(new BigDecimal("25.5"));

        // then
        assertNotNull(percentage);
        assertEquals(0, new BigDecimal("25.50000").compareTo(percentage.value()));
    }

    @Test
    void shouldCreateZeroPercentage() {
        // when
        Percentage percentage = Percentage.zero();

        // then
        assertEquals(0, BigDecimal.ZERO.compareTo(percentage.value()));
    }

    @Test
    void shouldCreateOneHundredPercentage() {
        // when
        Percentage percentage = Percentage.oneHundred();

        // then
        assertEquals(0, new BigDecimal("100.00000").compareTo(percentage.value()));
    }

    @Test
    void shouldThrowExceptionForNegativePercentage() {
        // when & then
        assertThrows(IllegalArgumentException.class, () -> Percentage.of(new BigDecimal("-10")));
    }

    @Test
    void shouldAddPercentages() {
        // given
        Percentage p1 = Percentage.of(30);
        Percentage p2 = Percentage.of(20);

        // when
        Percentage result = p1.add(p2);

        // then
        assertEquals(0, new BigDecimal("50.00000").compareTo(result.value()));
    }

    @Test
    void shouldSubtractPercentages() {
        // given
        Percentage p1 = Percentage.of(50);
        Percentage p2 = Percentage.of(20);

        // when
        Percentage result = p1.subtract(p2);

        // then
        assertEquals(0, new BigDecimal("30.00000").compareTo(result.value()));
    }

    @Test
    void shouldMultiplyPercentages() {
        // given
        Percentage p1 = Percentage.of(50); // 50%
        Percentage p2 = Percentage.of(20); // 20%

        // when - 50% of 20% = 10%
        Percentage result = p1.multiply(p2);

        // then
        assertEquals(0, new BigDecimal("10.00000").compareTo(result.value()));
    }

    @Test
    void shouldMultiplyPercentagesWithDecimals() {
        // given
        Percentage p1 = Percentage.of(new BigDecimal("33.33"));
        Percentage p2 = Percentage.of(new BigDecimal("50"));

        // when - 33.33% of 50% = 16.665%
        Percentage result = p1.multiply(p2);

        // then
        assertEquals(0, new BigDecimal("16.66500").compareTo(result.value()));
    }

    @Test
    void shouldFormatToStringWithTwoDecimalPlaces() {
        // given
        Percentage percentage = Percentage.of(new BigDecimal("25.5"));

        // when
        String formatted = percentage.toString();

        // then
        assertEquals("25.5%", formatted);
    }

    @Test
    void shouldFormatToStringWithoutTrailingZeros() {
        // given
        Percentage percentage = Percentage.of(50);

        // when
        String formatted = percentage.toString();

        // then
        assertEquals("50%", formatted);
    }

    @Test
    void shouldFormatZeroPercentage() {
        // given
        Percentage percentage = Percentage.zero();

        // when
        String formatted = percentage.toString();

        // then
        assertEquals("0%", formatted);
    }

    @Test
    void shouldHandleVerySmallPercentages() {
        // given
        Percentage percentage = Percentage.of(new BigDecimal("0.01"));

        // when
        String formatted = percentage.toString();

        // then
        assertEquals("0.01%", formatted);
    }

    @Test
    void shouldHandleVeryLargePercentages() {
        // given
        Percentage percentage = Percentage.of(new BigDecimal("999.99"));

        // when
        String formatted = percentage.toString();

        // then
        assertEquals("999.99%", formatted);
    }

    @Test
    void shouldSubtractToNearZero() {
        // given
        Percentage p1 = Percentage.of(10);
        Percentage p2 = Percentage.of(10);

        // when
        Percentage result = p1.subtract(p2);

        // then
        assertEquals(0, BigDecimal.ZERO.compareTo(result.value()));
    }

    @Test
    void shouldThrowWhenSubtractionResultsInNegative() {
        // given
        Percentage p1 = Percentage.of(10);
        Percentage p2 = Percentage.of(20);

        // when & then
        assertThrows(IllegalArgumentException.class, () -> p1.subtract(p2));
    }
}
