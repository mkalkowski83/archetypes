package com.softwarearchetypes.planvsexecution.productionanalysis.delta;

import com.softwarearchetypes.planvsexecution.productionanalysis.ActualProduction;
import com.softwarearchetypes.planvsexecution.productionanalysis.PlannedProduction;
import com.softwarearchetypes.planvsexecution.productionanalysis.ProductionPlan;
import com.softwarearchetypes.planvsexecution.productionanalysis.tolerance.MatchResult;
import com.softwarearchetypes.planvsexecution.productionanalysis.tolerance.ToleranceStrategy;
import java.util.ArrayList;
import java.util.List;

/**
 * Calculates the delta between production plan and actual execution. Pure numerical comparison
 * using algebra of delta.
 */
public class DeltaCalculator {

    private final ToleranceStrategy toleranceStrategy;

    public DeltaCalculator(ToleranceStrategy toleranceStrategy) {
        this.toleranceStrategy = toleranceStrategy;
    }

    public DeltaResult calculate(ProductionPlan planned, List<ActualProduction> actual) {
        List<ProductionMatch> matched = new ArrayList<>();
        List<PlannedProduction> unmatchedPlanned = new ArrayList<>();
        List<ActualProduction> unmatchedActual = new ArrayList<>(actual);

        for (PlannedProduction plannedTarget : planned.targets()) {
            ProductionMatch match = findBestMatch(plannedTarget, unmatchedActual);

            if (match != null) {
                matched.add(match);
                unmatchedActual.removeAll(match.actual());
            } else {
                unmatchedPlanned.add(plannedTarget);
            }
        }

        return new DeltaResult(
                matched,
                unmatchedPlanned,
                unmatchedActual,
                DeltaStatistics.calculate(matched, unmatchedPlanned, unmatchedActual));
    }

    private ProductionMatch findBestMatch(
            PlannedProduction planned, List<ActualProduction> candidates) {
        // Try matching with single production batch
        for (ActualProduction candidate : candidates) {
            if (!candidate.productId().equals(planned.productId())) {
                continue;
            }
            MatchResult matchResult = toleranceStrategy.matches(planned, List.of(candidate));
            if (matchResult.matched()) {
                return new ProductionMatch(planned, List.of(candidate), matchResult);
            }
        }

        // Try matching with multiple batches (split production scenarios)
        List<ActualProduction> sameProductCandidates =
                candidates.stream().filter(c -> c.productId().equals(planned.productId())).toList();

        for (int size = 2; size <= Math.min(5, sameProductCandidates.size()); size++) {
            for (int start = 0; start <= sameProductCandidates.size() - size; start++) {
                List<ActualProduction> group = sameProductCandidates.subList(start, start + size);
                MatchResult matchResult = toleranceStrategy.matches(planned, group);
                if (matchResult.matched()) {
                    return new ProductionMatch(planned, new ArrayList<>(group), matchResult);
                }
            }
        }

        return null;
    }
}
