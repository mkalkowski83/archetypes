package com.softwarearchetypes.pricing;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class TimeRangeTest {

    @Test
    void should_support_local_time_values() {
        // given
        TimeRange range =
                CalculatorRange.time(
                        LocalTime.of(8, 0), LocalTime.of(18, 0), CalculatorId.generate());

        // when & then
        assertTrue(range.supports(LocalTime.of(12, 0)));
        assertFalse(range.supports("12:00"));
        assertFalse(range.supports(12));
    }

    @Test
    void should_contain_time_in_normal_range() {
        // given - normal range 8:00-18:00
        TimeRange range =
                CalculatorRange.time(
                        LocalTime.of(8, 0), LocalTime.of(18, 0), CalculatorId.generate());

        // when & then
        assertTrue(range.contains(LocalTime.of(8, 0))); // inclusive from
        assertTrue(range.contains(LocalTime.of(12, 0))); // middle
        assertTrue(range.contains(LocalTime.of(17, 59))); // just before to
        assertFalse(range.contains(LocalTime.of(18, 0))); // exclusive to
        assertFalse(range.contains(LocalTime.of(7, 59))); // before
        assertFalse(range.contains(LocalTime.of(18, 1))); // after
    }

    @Test
    void should_contain_time_in_range_crossing_midnight() {
        // given - range crossing midnight 22:00-06:00
        TimeRange range =
                CalculatorRange.time(
                        LocalTime.of(22, 0), LocalTime.of(6, 0), CalculatorId.generate());

        // when & then
        assertTrue(range.contains(LocalTime.of(22, 0))); // start
        assertTrue(range.contains(LocalTime.of(23, 30))); // late evening
        assertTrue(range.contains(LocalTime.of(0, 0))); // midnight
        assertTrue(range.contains(LocalTime.of(3, 0))); // early morning
        assertTrue(range.contains(LocalTime.of(5, 59))); // just before end
        assertFalse(range.contains(LocalTime.of(6, 0))); // exclusive end
        assertFalse(range.contains(LocalTime.of(12, 0))); // middle of day
        assertFalse(range.contains(LocalTime.of(18, 0))); // evening
    }

    @Test
    void should_not_contain_time_of_wrong_type() {
        // given
        TimeRange range =
                CalculatorRange.time(
                        LocalTime.of(8, 0), LocalTime.of(18, 0), CalculatorId.generate());

        // when & then
        assertFalse(range.contains("12:00"));
        assertFalse(range.contains(12));
    }

    @Test
    void should_detect_overlap_when_both_ranges_normal_and_overlap() {
        // given
        TimeRange range1 =
                CalculatorRange.time(
                        LocalTime.of(8, 0), LocalTime.of(18, 0), CalculatorId.generate());
        TimeRange range2 =
                CalculatorRange.time(
                        LocalTime.of(15, 0), LocalTime.of(20, 0), CalculatorId.generate());

        // when & then
        assertTrue(range1.overlaps(range2));
        assertTrue(range2.overlaps(range1));
    }

    @Test
    void should_not_detect_overlap_when_both_ranges_normal_and_adjacent() {
        // given
        TimeRange range1 =
                CalculatorRange.time(
                        LocalTime.of(8, 0), LocalTime.of(18, 0), CalculatorId.generate());
        TimeRange range2 =
                CalculatorRange.time(
                        LocalTime.of(18, 0), LocalTime.of(22, 0), CalculatorId.generate());

        // when & then
        assertFalse(range1.overlaps(range2));
        assertFalse(range2.overlaps(range1));
    }

    @Test
    void should_not_detect_overlap_when_one_crosses_midnight_and_other_fits_in_gap() {
        // given
        TimeRange night =
                CalculatorRange.time(
                        LocalTime.of(22, 0), LocalTime.of(6, 0), CalculatorId.generate());
        TimeRange day =
                CalculatorRange.time(
                        LocalTime.of(8, 0), LocalTime.of(18, 0), CalculatorId.generate());

        // when & then - day fits in gap [6:00, 22:00)
        assertFalse(night.overlaps(day));
        assertFalse(day.overlaps(night));
    }

    @Test
    void should_detect_overlap_when_one_crosses_midnight_and_other_overlaps() {
        // given
        TimeRange night =
                CalculatorRange.time(
                        LocalTime.of(22, 0), LocalTime.of(6, 0), CalculatorId.generate());
        TimeRange lateEvening =
                CalculatorRange.time(
                        LocalTime.of(20, 0), LocalTime.of(23, 0), CalculatorId.generate());

        // when & then
        assertTrue(night.overlaps(lateEvening));
        assertTrue(lateEvening.overlaps(night));
    }

    @Test
    void should_detect_overlap_when_both_cross_midnight() {
        // given
        TimeRange night1 =
                CalculatorRange.time(
                        LocalTime.of(22, 0), LocalTime.of(6, 0), CalculatorId.generate());
        TimeRange night2 =
                CalculatorRange.time(
                        LocalTime.of(20, 0), LocalTime.of(8, 0), CalculatorId.generate());

        // when & then - both contain midnight, so they overlap
        assertTrue(night1.overlaps(night2));
        assertTrue(night2.overlaps(night1));
    }

    @Test
    void should_be_compatible_with_other_time_ranges() {
        // given
        TimeRange range1 =
                CalculatorRange.time(
                        LocalTime.of(8, 0), LocalTime.of(18, 0), CalculatorId.generate());
        TimeRange range2 =
                CalculatorRange.time(
                        LocalTime.of(18, 0), LocalTime.of(22, 0), CalculatorId.generate());

        // when & then
        assertTrue(range1.isCompatibleWith(range2));
    }

    @Test
    void should_not_be_compatible_with_numeric_range() {
        // given
        TimeRange timeRange =
                CalculatorRange.time(
                        LocalTime.of(8, 0), LocalTime.of(18, 0), CalculatorId.generate());
        NumericRange numericRange =
                CalculatorRange.numeric(
                        java.math.BigDecimal.ZERO,
                        java.math.BigDecimal.TEN,
                        CalculatorId.generate());

        // when & then
        assertFalse(timeRange.isCompatibleWith(numericRange));
    }

    @Test
    void should_throw_when_checking_overlap_with_incompatible_range() {
        // given
        TimeRange timeRange =
                CalculatorRange.time(
                        LocalTime.of(8, 0), LocalTime.of(18, 0), CalculatorId.generate());
        NumericRange numericRange =
                CalculatorRange.numeric(
                        java.math.BigDecimal.ZERO,
                        java.math.BigDecimal.TEN,
                        CalculatorId.generate());

        // when & then
        assertThrows(IllegalArgumentException.class, () -> timeRange.overlaps(numericRange));
    }
}
