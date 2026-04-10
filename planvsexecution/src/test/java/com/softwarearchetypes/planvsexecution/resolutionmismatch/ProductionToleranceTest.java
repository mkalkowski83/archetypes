package com.softwarearchetypes.planvsexecution.resolutionmismatch;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class ProductionToleranceTest {

    private static final Function<MonthlyProductionPlan, List<DailyProductionExecution>>
            SPLIT_EVENLY_30_DAYS =
                    plan -> {
                        int dailyTarget = plan.targetQuantity() / 30;
                        return IntStream.range(1, 31)
                                .mapToObj(
                                        day ->
                                                new DailyProductionExecution(
                                                        LocalDate.of(
                                                                plan.month().getYear(),
                                                                plan.month().getMonth(),
                                                                day),
                                                        dailyTarget,
                                                        0,
                                                        0))
                                .toList();
                    };

    @SuppressWarnings("unused")
    private static MonthlyProductionPlan aggregateToMonthly(
            DailyProductionExecutionHistory execution, YearMonth month) {
        int totalProduced =
                execution.days().stream()
                        .filter(day -> YearMonth.from(day.date()).equals(month))
                        .mapToInt(day -> day.produced() - day.defects() + day.rework())
                        .sum();
        return new MonthlyProductionPlan(month, totalProduced);
    }

    @SuppressWarnings("unused")
    private static MonthlyProductionPlan aggregateGrossProduction(
            DailyProductionExecutionHistory execution, YearMonth month) {
        int totalProduced =
                execution.days().stream()
                        .filter(day -> YearMonth.from(day.date()).equals(month))
                        .mapToInt(DailyProductionExecution::produced)
                        .sum();
        return new MonthlyProductionPlan(month, totalProduced);
    }

    @Test
    void shouldMapMonthlyPlanToDailyBeforeCalculatingDelta() {
        // given
        var plan = new MonthlyProductionPlan(YearMonth.of(2025, 1), 300);
        var execution =
                new DailyProductionExecutionHistory(
                        List.of(
                                new DailyProductionExecution(LocalDate.of(2025, 1, 2), 270, 12, 0),
                                new DailyProductionExecution(LocalDate.of(2025, 1, 4), 18, 0, 0),
                                new DailyProductionExecution(LocalDate.of(2025, 1, 7), 12, 0, 0),
                                new DailyProductionExecution(LocalDate.of(2025, 1, 12), 18, 6, 0)));
        var tolerance = ProductionTolerance.builder().allowedDeviation(10).build();

        // when
        boolean result = tolerance.isWithinTolerance(plan, execution, SPLIT_EVENLY_30_DAYS);

        // then
        assertTrue(result);
    }

    @Test
    void shouldDetectDeviationWhenActualExceedsTolerance() {
        // given
        var plan = new MonthlyProductionPlan(YearMonth.of(2025, 1), 300);
        var execution =
                new DailyProductionExecutionHistory(
                        List.of(
                                new DailyProductionExecution(LocalDate.of(2025, 1, 2), 270, 12, 0),
                                new DailyProductionExecution(LocalDate.of(2025, 1, 4), 18, 0, 0),
                                new DailyProductionExecution(LocalDate.of(2025, 1, 7), 10, 0, 0)));
        var tolerance = ProductionTolerance.builder().allowedDeviation(10).build();

        // when
        boolean result = tolerance.isWithinTolerance(plan, execution, SPLIT_EVENLY_30_DAYS);

        // then
        assertFalse(result);
    }

    @Test
    void shouldShowThatDifferentMappingStrategiesGiveDifferentResults() {
        // given
        var plan = new MonthlyProductionPlan(YearMonth.of(2025, 1), 300);
        var execution =
                new DailyProductionExecutionHistory(
                        List.of(
                                new DailyProductionExecution(LocalDate.of(2025, 1, 2), 270, 12, 0),
                                new DailyProductionExecution(LocalDate.of(2025, 1, 4), 25, 0, 0)));
        var tolerance = ProductionTolerance.builder().allowedDeviation(20).build();
        Function<MonthlyProductionPlan, List<DailyProductionExecution>> splitWorkdays =
                p -> {
                    int dailyTarget = (int) Math.ceil(p.targetQuantity() / 22.0);
                    return IntStream.range(1, 23)
                            .mapToObj(
                                    day ->
                                            new DailyProductionExecution(
                                                    LocalDate.of(
                                                            p.month().getYear(),
                                                            p.month().getMonth(),
                                                            day),
                                                    dailyTarget,
                                                    0,
                                                    0))
                            .toList();
                };

        // when
        boolean result30Days = tolerance.isWithinTolerance(plan, execution, SPLIT_EVENLY_30_DAYS);
        boolean resultWorkdays = tolerance.isWithinTolerance(plan, execution, splitWorkdays);

        // then
        assertTrue(result30Days);
        assertFalse(resultWorkdays);
    }
}
