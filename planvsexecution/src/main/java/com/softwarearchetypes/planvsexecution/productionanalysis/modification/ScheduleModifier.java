package com.softwarearchetypes.planvsexecution.productionanalysis.modification;

import com.softwarearchetypes.planvsexecution.productionanalysis.ProductionPlan;
import com.softwarearchetypes.planvsexecution.productionanalysis.delta.DeltaResult;

/**
 * Modifier that changes the production plan based on delta analysis. This is where SIMULATIONS
 * happen - we modify the plan without changing reality.
 */
public interface ScheduleModifier {
    ProductionPlan modify(ProductionPlan currentPlan, DeltaResult deltaResult);
}
