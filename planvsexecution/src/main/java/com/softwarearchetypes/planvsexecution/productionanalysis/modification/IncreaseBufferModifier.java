package com.softwarearchetypes.planvsexecution.productionanalysis.modification;

import com.softwarearchetypes.planvsexecution.productionanalysis.PlannedProduction;
import com.softwarearchetypes.planvsexecution.productionanalysis.ProductionPlan;
import com.softwarearchetypes.planvsexecution.productionanalysis.delta.DeltaResult;
import java.util.ArrayList;
import java.util.List;

/**
 * Increases production buffer quantities when under-production is detected. Simulation: "what if we
 * planned more to compensate?"
 */
public class IncreaseBufferModifier implements ScheduleModifier {

    private final double bufferPercentage;

    public IncreaseBufferModifier(double bufferPercentage) {
        this.bufferPercentage = bufferPercentage;
    }

    public static IncreaseBufferModifier by(double percentage) {
        return new IncreaseBufferModifier(percentage);
    }

    @Override
    public ProductionPlan modify(ProductionPlan currentPlan, DeltaResult deltaResult) {
        List<PlannedProduction> modifiedTargets = new ArrayList<>();

        for (PlannedProduction target : currentPlan.targets()) {
            int bufferQuantity = (int) (target.targetQuantity() * bufferPercentage / 100);
            int newQuantity = target.targetQuantity() + bufferQuantity;

            modifiedTargets.add(
                    new PlannedProduction(target.id(), target.productId(), newQuantity));
        }

        return ProductionPlan.of(modifiedTargets);
    }
}
