package com.softwarearchetypes.planvsexecution.productionanalysis.delta;

import com.softwarearchetypes.planvsexecution.productionanalysis.ActualProduction;
import com.softwarearchetypes.planvsexecution.productionanalysis.PlannedProduction;
import com.softwarearchetypes.planvsexecution.productionanalysis.tolerance.MatchResult;
import java.util.List;

/**
 * Represents a match between planned and actual production. This is part of the DELTA - the
 * comparison result.
 */
public record ProductionMatch(
        PlannedProduction planned, List<ActualProduction> actual, MatchResult matchResult) {

    public int totalProducedQuantity() {
        return actual.stream().mapToInt(ActualProduction::producedQuantity).sum();
    }

    public int quantityDeviation() {
        return totalProducedQuantity() - planned.targetQuantity();
    }

    public boolean isUnderProduced() {
        return totalProducedQuantity() < planned.targetQuantity();
    }

    public boolean isOverProduced() {
        return totalProducedQuantity() > planned.targetQuantity();
    }

    @Override
    public String toString() {
        return String.format(
                "Match[%s: %d actual batches, qty: %d/%d units]",
                planned.productId(),
                actual.size(),
                totalProducedQuantity(),
                planned.targetQuantity());
    }
}
