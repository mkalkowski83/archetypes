package com.softwarearchetypes.planvsexecution.productionanalysis;

import java.util.List;

/**
 * A production plan - collection of planned production targets. This is the PLAN aggregate: "We
 * want to produce these quantities."
 */
public record ProductionPlan(List<PlannedProduction> targets) {

    public static ProductionPlan of(List<PlannedProduction> targets) {
        return new ProductionPlan(List.copyOf(targets));
    }

    public static ProductionPlan empty() {
        return new ProductionPlan(List.of());
    }

    public int totalTargetQuantity() {
        return targets.stream().mapToInt(PlannedProduction::targetQuantity).sum();
    }

    @Override
    public String toString() {
        return String.format(
                "ProductionPlan[%d products, total=%d units]",
                targets.size(), totalTargetQuantity());
    }
}
