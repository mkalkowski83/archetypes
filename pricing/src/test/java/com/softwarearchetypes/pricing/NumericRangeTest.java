package com.softwarearchetypes.pricing;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class NumericRangeTest {

    @Test
    void should_support_big_decimal_values() {
        // given
        NumericRange range =
                CalculatorRange.numeric(
                        new BigDecimal("0"), new BigDecimal("10"), CalculatorId.generate());

        // when & then
        assertTrue(range.supports(new BigDecimal("5")));
        assertFalse(range.supports(5));
        assertFalse(range.supports("5"));
        assertFalse(range.supports(5.0));
    }

    @Test
    void should_contain_value_in_range() {
        // given
        NumericRange range =
                CalculatorRange.numeric(
                        new BigDecimal("10"), new BigDecimal("20"), CalculatorId.generate());

        // when & then
        assertTrue(range.contains(new BigDecimal("10"))); // inclusive min
        assertTrue(range.contains(new BigDecimal("15"))); // middle
        assertFalse(range.contains(new BigDecimal("20"))); // exclusive max
        assertFalse(range.contains(new BigDecimal("5"))); // below
        assertFalse(range.contains(new BigDecimal("25"))); // above
    }

    @Test
    void should_not_contain_value_of_wrong_type() {
        // given
        NumericRange range =
                CalculatorRange.numeric(
                        new BigDecimal("0"), new BigDecimal("10"), CalculatorId.generate());

        // when & then
        assertFalse(range.contains("5"));
        assertFalse(range.contains(5));
    }

    @Test
    void should_throw_when_min_greater_than_max() {
        // when & then
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        CalculatorRange.numeric(
                                new BigDecimal("20"),
                                new BigDecimal("10"),
                                CalculatorId.generate()));
    }

    @Test
    void should_throw_when_min_equals_max() {
        // when & then
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        CalculatorRange.numeric(
                                new BigDecimal("10"),
                                new BigDecimal("10"),
                                CalculatorId.generate()));
    }

    @Test
    void should_detect_overlap_when_ranges_overlap() {
        // given
        NumericRange range1 =
                CalculatorRange.numeric(
                        new BigDecimal("0"), new BigDecimal("10"), CalculatorId.generate());
        NumericRange range2 =
                CalculatorRange.numeric(
                        new BigDecimal("5"), new BigDecimal("15"), CalculatorId.generate());

        // when & then
        assertTrue(range1.overlaps(range2));
        assertTrue(range2.overlaps(range1)); // symmetric
    }

    @Test
    void should_not_detect_overlap_when_ranges_adjacent() {
        // given
        NumericRange range1 =
                CalculatorRange.numeric(
                        new BigDecimal("0"), new BigDecimal("10"), CalculatorId.generate());
        NumericRange range2 =
                CalculatorRange.numeric(
                        new BigDecimal("10"), new BigDecimal("20"), CalculatorId.generate());

        // when & then
        assertFalse(range1.overlaps(range2));
        assertFalse(range2.overlaps(range1));
    }

    @Test
    void should_detect_overlap_when_one_range_contains_another() {
        // given
        NumericRange larger =
                CalculatorRange.numeric(
                        new BigDecimal("0"), new BigDecimal("100"), CalculatorId.generate());
        NumericRange smaller =
                CalculatorRange.numeric(
                        new BigDecimal("20"), new BigDecimal("30"), CalculatorId.generate());

        // when & then
        assertTrue(larger.overlaps(smaller));
        assertTrue(smaller.overlaps(larger));
    }

    @Test
    void should_be_compatible_with_other_numeric_ranges() {
        // given
        NumericRange range1 =
                CalculatorRange.numeric(
                        new BigDecimal("0"), new BigDecimal("10"), CalculatorId.generate());
        NumericRange range2 =
                CalculatorRange.numeric(
                        new BigDecimal("20"), new BigDecimal("30"), CalculatorId.generate());

        // when & then
        assertTrue(range1.isCompatibleWith(range2));
    }
}
