package com.softwarearchetypes.planvsexecution.productionanalysis.delta;

import com.softwarearchetypes.planvsexecution.productionanalysis.ActualProduction;
import com.softwarearchetypes.planvsexecution.productionanalysis.PlannedProduction;
import com.softwarearchetypes.planvsexecution.productionanalysis.modification.ScheduleModificationCondition;
import com.softwarearchetypes.planvsexecution.productionanalysis.modification.UnderProductionCondition;
import java.util.List;

/**
 * The result of comparing production plan with actual execution. This is the DELTA - first-class
 * citizen representing numerical comparison.
 */
public record DeltaResult(
        List<ProductionMatch> matched,
        List<PlannedProduction> unmatchedPlanned,
        List<ActualProduction> unmatchedActual,
        DeltaStatistics statistics) {

    public boolean isPerfectMatch() {
        return unmatchedPlanned.isEmpty()
                && unmatchedActual.isEmpty()
                && matched.stream().allMatch(m -> m.quantityDeviation() == 0);
    }

    public boolean hasUnderProduction() {
        return !unmatchedPlanned.isEmpty()
                || matched.stream().anyMatch(ProductionMatch::isUnderProduced);
    }

    public boolean hasOverProduction() {
        return !unmatchedActual.isEmpty()
                || matched.stream().anyMatch(ProductionMatch::isOverProduced);
    }

    public int totalPlannedProducts() {
        return matched.size() + unmatchedPlanned.size();
    }

    public double matchRate() {
        if (totalPlannedProducts() == 0) {
            return 0.0;
        }
        return (double) matched.size() / totalPlannedProducts();
    }

    /**
     * Checks if this delta fulfills a given modification condition. Different interpretations of
     * the same numerical delta!
     */
    public boolean fulfills(ScheduleModificationCondition condition) {
        if (condition instanceof UnderProductionCondition underCondition) {
            return statistics.totalUnderProducedQuantity() >= underCondition.minQuantity();
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format(
                "Delta[matched=%d, unmatched planned=%d, unmatched actual=%d, %s]",
                matched.size(), unmatchedPlanned.size(), unmatchedActual.size(), statistics);
    }
}
