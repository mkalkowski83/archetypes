package com.softwarearchetypes.pricing;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class DateRangeTest {

    @Test
    void should_support_local_date_values() {
        // given
        DateRange range =
                CalculatorRange.date(
                        LocalDate.of(2024, 6, 1),
                        LocalDate.of(2024, 9, 1),
                        CalculatorId.generate());

        // when & then
        assertTrue(range.supports(LocalDate.of(2024, 7, 15)));
        assertFalse(range.supports("2024-07-15"));
        assertFalse(range.supports(2024));
    }

    @Test
    void should_contain_date_in_range() {
        // given - summer season
        DateRange range =
                CalculatorRange.date(
                        LocalDate.of(2024, 6, 1),
                        LocalDate.of(2024, 9, 1),
                        CalculatorId.generate());

        // when & then
        assertTrue(range.contains(LocalDate.of(2024, 6, 1))); // inclusive from
        assertTrue(range.contains(LocalDate.of(2024, 7, 15))); // middle
        assertTrue(range.contains(LocalDate.of(2024, 8, 31))); // just before to
        assertFalse(range.contains(LocalDate.of(2024, 9, 1))); // exclusive to
        assertFalse(range.contains(LocalDate.of(2024, 5, 31))); // before
        assertFalse(range.contains(LocalDate.of(2024, 9, 2))); // after
    }

    @Test
    void should_not_contain_date_of_wrong_type() {
        // given
        DateRange range =
                CalculatorRange.date(
                        LocalDate.of(2024, 6, 1),
                        LocalDate.of(2024, 9, 1),
                        CalculatorId.generate());

        // when & then
        assertFalse(range.contains("2024-07-15"));
        assertFalse(range.contains(2024));
    }

    @Test
    void should_throw_when_from_not_before_to() {
        // when & then
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        CalculatorRange.date(
                                LocalDate.of(2024, 9, 1),
                                LocalDate.of(2024, 6, 1),
                                CalculatorId.generate()));
    }

    @Test
    void should_throw_when_from_equals_to() {
        // when & then
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        CalculatorRange.date(
                                LocalDate.of(2024, 6, 1),
                                LocalDate.of(2024, 6, 1),
                                CalculatorId.generate()));
    }

    @Test
    void should_detect_overlap_when_ranges_overlap() {
        // given
        DateRange summer =
                CalculatorRange.date(
                        LocalDate.of(2024, 6, 1),
                        LocalDate.of(2024, 9, 1),
                        CalculatorId.generate());
        DateRange lateSummer =
                CalculatorRange.date(
                        LocalDate.of(2024, 8, 1),
                        LocalDate.of(2024, 10, 1),
                        CalculatorId.generate());

        // when & then
        assertTrue(summer.overlaps(lateSummer));
        assertTrue(lateSummer.overlaps(summer));
    }

    @Test
    void should_not_detect_overlap_when_ranges_adjacent() {
        // given
        DateRange summer =
                CalculatorRange.date(
                        LocalDate.of(2024, 6, 1),
                        LocalDate.of(2024, 9, 1),
                        CalculatorId.generate());
        DateRange fall =
                CalculatorRange.date(
                        LocalDate.of(2024, 9, 1),
                        LocalDate.of(2024, 12, 1),
                        CalculatorId.generate());

        // when & then
        assertFalse(summer.overlaps(fall));
        assertFalse(fall.overlaps(summer));
    }

    @Test
    void should_detect_overlap_when_one_range_contains_another() {
        // given
        DateRange year =
                CalculatorRange.date(
                        LocalDate.of(2024, 1, 1),
                        LocalDate.of(2025, 1, 1),
                        CalculatorId.generate());
        DateRange summer =
                CalculatorRange.date(
                        LocalDate.of(2024, 6, 1),
                        LocalDate.of(2024, 9, 1),
                        CalculatorId.generate());

        // when & then
        assertTrue(year.overlaps(summer));
        assertTrue(summer.overlaps(year));
    }

    @Test
    void should_be_compatible_with_other_date_ranges() {
        // given
        DateRange range1 =
                CalculatorRange.date(
                        LocalDate.of(2024, 1, 1),
                        LocalDate.of(2024, 6, 1),
                        CalculatorId.generate());
        DateRange range2 =
                CalculatorRange.date(
                        LocalDate.of(2024, 6, 1),
                        LocalDate.of(2024, 12, 1),
                        CalculatorId.generate());

        // when & then
        assertTrue(range1.isCompatibleWith(range2));
    }

    @Test
    void should_not_be_compatible_with_time_range() {
        // given
        DateRange dateRange =
                CalculatorRange.date(
                        LocalDate.of(2024, 1, 1),
                        LocalDate.of(2024, 12, 1),
                        CalculatorId.generate());
        TimeRange timeRange =
                CalculatorRange.time(
                        LocalTime.of(8, 0), LocalTime.of(18, 0), CalculatorId.generate());

        // when & then
        assertFalse(dateRange.isCompatibleWith(timeRange));
    }
}
