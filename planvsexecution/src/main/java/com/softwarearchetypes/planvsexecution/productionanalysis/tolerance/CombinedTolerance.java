package com.softwarearchetypes.planvsexecution.productionanalysis.tolerance;

import com.softwarearchetypes.planvsexecution.productionanalysis.ActualProduction;
import com.softwarearchetypes.planvsexecution.productionanalysis.PlannedProduction;
import java.util.List;

/**
 * Combines multiple tolerance strategies. All strategies must match for the result to be considered
 * a match.
 */
class CombinedTolerance implements ToleranceStrategy {

    private final List<ToleranceStrategy> strategies;

    CombinedTolerance(List<ToleranceStrategy> strategies) {
        this.strategies = List.copyOf(strategies);
    }

    @Override
    public MatchResult matches(PlannedProduction planned, List<ActualProduction> actual) {
        for (ToleranceStrategy strategy : strategies) {
            MatchResult result = strategy.matches(planned, actual);
            if (!result.matched()) {
                return result;
            }
        }
        return MatchResult.matched("All tolerance strategies matched");
    }
}
