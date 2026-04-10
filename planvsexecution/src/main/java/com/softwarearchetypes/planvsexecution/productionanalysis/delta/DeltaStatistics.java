package com.softwarearchetypes.planvsexecution.productionanalysis.delta;

import com.softwarearchetypes.planvsexecution.productionanalysis.ActualProduction;
import com.softwarearchetypes.planvsexecution.productionanalysis.PlannedProduction;
import java.util.List;

/**
 * Statistical summary of the delta between plan and execution. Purely numerical comparison -
 * focuses on quantities.
 */
public record DeltaStatistics(
        int totalUnderProducedQuantity,
        int totalOverProducedQuantity,
        int netQuantityDifference,
        int underProducedProductsCount,
        int overProducedProductsCount) {

    public static DeltaStatistics calculate(
            List<ProductionMatch> matched,
            List<PlannedProduction> unmatchedPlanned,
            List<ActualProduction> unmatchedActual) {

        int underProduced =
                matched.stream()
                                .filter(ProductionMatch::isUnderProduced)
                                .mapToInt(m -> Math.abs(m.quantityDeviation()))
                                .sum()
                        + unmatchedPlanned.stream()
                                .mapToInt(PlannedProduction::targetQuantity)
                                .sum();

        int overProduced =
                matched.stream()
                                .filter(ProductionMatch::isOverProduced)
                                .mapToInt(ProductionMatch::quantityDeviation)
                                .sum()
                        + unmatchedActual.stream()
                                .mapToInt(ActualProduction::producedQuantity)
                                .sum();

        int netDifference = overProduced - underProduced;

        int underProducedCount =
                (int) matched.stream().filter(ProductionMatch::isUnderProduced).count()
                        + unmatchedPlanned.size();

        int overProducedCount =
                (int) matched.stream().filter(ProductionMatch::isOverProduced).count()
                        + unmatchedActual.size();

        return new DeltaStatistics(
                underProduced, overProduced, netDifference, underProducedCount, overProducedCount);
    }

    @Override
    public String toString() {
        return String.format(
                "Stats[underProduced=%d units (%d products), overProduced=%d units (%d products), net=%d]",
                totalUnderProducedQuantity,
                underProducedProductsCount,
                totalOverProducedQuantity,
                overProducedProductsCount,
                netQuantityDifference);
    }
}
