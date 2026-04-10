package com.softwarearchetypes.planvsexecution.productionanalysis.tolerance;

import com.softwarearchetypes.planvsexecution.productionanalysis.ActualProduction;
import com.softwarearchetypes.planvsexecution.productionanalysis.PlannedProduction;
import java.util.List;

/** Exact matching - no tolerance. Quantities must match exactly. */
class ExactMatch implements ToleranceStrategy {

    @Override
    public MatchResult matches(PlannedProduction planned, List<ActualProduction> actual) {
        if (actual.isEmpty()) {
            return MatchResult.notMatched("No actual production");
        }

        int totalProduced = actual.stream().mapToInt(ActualProduction::producedQuantity).sum();

        if (totalProduced != planned.targetQuantity()) {
            return MatchResult.notMatched(
                    String.format(
                            "Quantity mismatch: planned %d, actual %d",
                            planned.targetQuantity(), totalProduced));
        }

        return MatchResult.matched("Exact quantity match");
    }
}
