package com.softwarearchetypes.planvsexecution.productionanalysis.tolerance;

import com.softwarearchetypes.planvsexecution.productionanalysis.ActualProduction;
import com.softwarearchetypes.planvsexecution.productionanalysis.PlannedProduction;
import java.util.List;

/** Tolerance strategy based on quantity deviation. Allows ±X% or ±X units deviation. */
class QuantityTolerance implements ToleranceStrategy {

    private final double percentageTolerance;
    private final int absoluteTolerance;

    QuantityTolerance(double percentageTolerance, int absoluteTolerance) {
        this.percentageTolerance = percentageTolerance;
        this.absoluteTolerance = absoluteTolerance;
    }

    @Override
    public MatchResult matches(PlannedProduction planned, List<ActualProduction> actual) {
        if (actual.isEmpty()) {
            return MatchResult.notMatched("No actual production");
        }

        int totalProduced = actual.stream().mapToInt(ActualProduction::producedQuantity).sum();

        int deviation = Math.abs(totalProduced - planned.targetQuantity());

        // Check absolute tolerance
        if (deviation <= absoluteTolerance) {
            return MatchResult.matched(
                    String.format(
                            "Within absolute tolerance: deviation %d ≤ %d",
                            deviation, absoluteTolerance));
        }

        // Check percentage tolerance
        double percentageDeviation = (double) deviation / planned.targetQuantity() * 100;
        if (percentageDeviation <= percentageTolerance) {
            return MatchResult.matched(
                    String.format(
                            "Within percentage tolerance: %.1f%% ≤ %.1f%%",
                            percentageDeviation, percentageTolerance));
        }

        return MatchResult.notMatched(
                String.format(
                        "Exceeds tolerance: deviation %d (%.1f%%), tolerance: %d units or %.1f%%",
                        deviation, percentageDeviation, absoluteTolerance, percentageTolerance));
    }
}
