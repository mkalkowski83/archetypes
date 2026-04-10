package com.softwarearchetypes.planvsexecution.resolutionmismatch;

import java.util.List;
import java.util.function.Function;

class ProductionTolerance {

    private final int allowedDeviation;

    private ProductionTolerance(int allowedDeviation) {
        this.allowedDeviation = allowedDeviation;
    }

    static Builder builder() {
        return new Builder();
    }

    boolean isWithinTolerance(
            MonthlyProductionPlan plan,
            DailyProductionExecutionHistory execution,
            Function<MonthlyProductionPlan, List<DailyProductionExecution>> planToDaily) {
        List<DailyProductionExecution> plannedDaily = planToDaily.apply(plan);

        int totalPlanned = plannedDaily.stream().mapToInt(DailyProductionExecution::produced).sum();

        int totalActual =
                execution.days().stream()
                        .mapToInt(day -> day.produced() - day.defects() + day.rework())
                        .sum();

        int delta = Math.abs(totalPlanned - totalActual);

        return delta <= allowedDeviation;
    }

    static class Builder {
        private int allowedDeviation;

        Builder allowedDeviation(int allowedDeviation) {
            this.allowedDeviation = allowedDeviation;
            return this;
        }

        ProductionTolerance build() {
            return new ProductionTolerance(allowedDeviation);
        }
    }
}
