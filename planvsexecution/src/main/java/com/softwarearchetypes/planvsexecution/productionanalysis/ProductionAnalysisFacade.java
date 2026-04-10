package com.softwarearchetypes.planvsexecution.productionanalysis;

import com.softwarearchetypes.planvsexecution.productionanalysis.delta.DeltaCalculator;
import com.softwarearchetypes.planvsexecution.productionanalysis.delta.DeltaResult;
import com.softwarearchetypes.planvsexecution.productionanalysis.tolerance.ToleranceStrategy;
import java.util.List;

/**
 * Facade for production analysis. Orchestrates the delta calculation - pure numerical comparison.
 */
public class ProductionAnalysisFacade {

    public DeltaResult analyze(
            ProductionPlan planned, List<ActualProduction> actual, ToleranceStrategy tolerance) {

        DeltaCalculator calculator = new DeltaCalculator(tolerance);
        return calculator.calculate(planned, actual);
    }
}
