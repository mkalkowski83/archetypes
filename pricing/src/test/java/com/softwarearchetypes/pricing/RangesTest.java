package com.softwarearchetypes.pricing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RangesTest {

    @Test
    void should_create_ranges_with_valid_non_overlapping_ranges() {
        // given
        List<CalculatorRange> rangesList =
                List.of(
                        CalculatorRange.numeric(
                                new BigDecimal("0"), new BigDecimal("10"), CalculatorId.generate()),
                        CalculatorRange.numeric(
                                new BigDecimal("10"),
                                new BigDecimal("50"),
                                CalculatorId.generate()),
                        CalculatorRange.numeric(
                                new BigDecimal("50"),
                                new BigDecimal("100"),
                                CalculatorId.generate()));

        // when
        Ranges ranges = new Ranges("quantity", rangesList);

        // then
        assertEquals(3, ranges.size());
    }

    @Test
    void should_throw_when_ranges_are_empty() {
        // when & then
        assertThrows(IllegalArgumentException.class, () -> new Ranges("quantity", List.of()));
    }

    @Test
    void should_throw_when_range_selector_is_null() {
        // given
        List<CalculatorRange> rangesList =
                List.of(
                        CalculatorRange.numeric(
                                new BigDecimal("0"),
                                new BigDecimal("10"),
                                CalculatorId.generate()));

        // when & then
        assertThrows(IllegalArgumentException.class, () -> new Ranges(null, rangesList));
    }

    @Test
    void should_throw_when_range_selector_is_blank() {
        // given
        List<CalculatorRange> rangesList =
                List.of(
                        CalculatorRange.numeric(
                                new BigDecimal("0"),
                                new BigDecimal("10"),
                                CalculatorId.generate()));

        // when & then
        assertThrows(IllegalArgumentException.class, () -> new Ranges("  ", rangesList));
    }

    @Test
    void should_throw_when_ranges_overlap() {
        // given
        List<CalculatorRange> rangesList =
                List.of(
                        CalculatorRange.numeric(
                                new BigDecimal("0"), new BigDecimal("10"), CalculatorId.generate()),
                        CalculatorRange.numeric(
                                new BigDecimal("5"),
                                new BigDecimal("15"),
                                CalculatorId.generate()) // overlaps!
                        );

        // when & then
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class, () -> new Ranges("quantity", rangesList));

        assertTrue(exception.getMessage().contains("overlap"));
    }

    @Test
    void should_throw_when_ranges_have_incompatible_types() {
        // given
        List<CalculatorRange> rangesList =
                List.of(
                        CalculatorRange.numeric(
                                new BigDecimal("0"), new BigDecimal("10"), CalculatorId.generate()),
                        CalculatorRange.time(
                                LocalTime.of(8, 0),
                                LocalTime.of(18, 0),
                                CalculatorId.generate()) // different type!
                        );

        // when & then
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> new Ranges("param", rangesList));

        assertTrue(exception.getMessage().contains("same type"));
    }

    @Test
    void should_find_matching_range_for_numeric_value() {
        // given
        CalculatorId matchingRangeCalculatorId = CalculatorId.generate();

        Ranges ranges =
                new Ranges(
                        "quantity",
                        List.of(
                                CalculatorRange.numeric(
                                        new BigDecimal("0"),
                                        new BigDecimal("10"),
                                        CalculatorId.generate()),
                                CalculatorRange.numeric(
                                        new BigDecimal("10"),
                                        new BigDecimal("50"),
                                        matchingRangeCalculatorId),
                                CalculatorRange.numeric(
                                        new BigDecimal("50"),
                                        new BigDecimal("100"),
                                        CalculatorId.generate())));

        Parameters params = new Parameters(Map.of("quantity", new BigDecimal("25")));

        // when
        CalculatorRange matchingRange = ranges.findMatching(params).orElseThrow();

        // then
        assertEquals(matchingRangeCalculatorId, matchingRange.calculatorId());
    }

    @Test
    void should_find_matching_range_for_time_value() {
        // given
        CalculatorId matchingRangeCalculatorId = CalculatorId.generate();

        Ranges ranges =
                new Ranges(
                        "time",
                        List.of(
                                CalculatorRange.time(
                                        LocalTime.of(8, 0),
                                        LocalTime.of(18, 0),
                                        matchingRangeCalculatorId),
                                CalculatorRange.time(
                                        LocalTime.of(18, 0),
                                        LocalTime.of(8, 0),
                                        CalculatorId.generate())));

        Parameters params = new Parameters(Map.of("time", LocalTime.of(15, 30)));

        // when
        CalculatorRange matchingRange = ranges.findMatching(params).orElseThrow();

        // then
        assertEquals(matchingRangeCalculatorId, matchingRange.calculatorId());
    }

    @Test
    void should_return_empty_when_no_matching_range() {
        // given
        Ranges ranges =
                new Ranges(
                        "quantity",
                        List.of(
                                CalculatorRange.numeric(
                                        new BigDecimal("10"),
                                        new BigDecimal("50"),
                                        CalculatorId.generate())));

        Parameters params = new Parameters(Map.of("quantity", new BigDecimal("5"))); // below range

        // when
        var result = ranges.findMatching(params);

        // then
        assertTrue(result.isEmpty());
    }

    @Test
    void should_throw_when_parameter_not_found() {
        // given
        Ranges ranges =
                new Ranges(
                        "quantity",
                        List.of(
                                CalculatorRange.numeric(
                                        new BigDecimal("0"),
                                        new BigDecimal("10"),
                                        CalculatorId.generate())));

        Parameters params =
                new Parameters(Map.of("weight", new BigDecimal("5"))); // wrong parameter!

        // when & then
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> ranges.findMatching(params));

        assertTrue(exception.getMessage().contains("quantity"));
        assertTrue(exception.getMessage().contains("required"));
    }

    @Test
    void should_find_first_matching_range_when_adjacent() {
        // given
        CalculatorId matchingRangeCalculatorId = CalculatorId.generate();

        Ranges ranges =
                new Ranges(
                        "quantity",
                        List.of(
                                CalculatorRange.numeric(
                                        new BigDecimal("0"),
                                        new BigDecimal("10"),
                                        CalculatorId.generate()),
                                CalculatorRange.numeric(
                                        new BigDecimal("10"),
                                        new BigDecimal("20"),
                                        matchingRangeCalculatorId)));

        Parameters paramsAt10 = new Parameters(Map.of("quantity", new BigDecimal("10")));

        // when
        CalculatorRange matchingRange = ranges.findMatching(paramsAt10).orElseThrow();

        // then - 10 belongs to second range [10, 20)
        assertEquals(matchingRangeCalculatorId, matchingRange.calculatorId());
    }
}
