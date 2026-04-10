package com.softwarearchetypes.planvsexecution.productionanalysis;

import com.softwarearchetypes.planvsexecution.productionanalysis.delta.DeltaResult;
import com.softwarearchetypes.planvsexecution.productionanalysis.modification.ModificationRule;
import com.softwarearchetypes.planvsexecution.productionanalysis.tolerance.ToleranceStrategy;
import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates the analysis and modification of production plans. This is where the complete
 * plan-execution-delta cycle happens.
 */
public class PlanModificationOrchestrator {

    private final ProductionAnalysisFacade analysisFacade;

    public PlanModificationOrchestrator(ProductionAnalysisFacade analysisFacade) {
        this.analysisFacade = analysisFacade;
    }

    public DeltaResult analyzeAndApply(
            ConfigurableProductionPlan configurable,
            List<ActualProduction> actual,
            ToleranceStrategy tolerance) {
        DeltaResult result = analysisFacade.analyze(configurable.activePlan(), actual, tolerance);
        List<ModificationRule> fulfilledRules = findFulfilledRules(configurable, result);
        configurable.fulfilled(fulfilledRules, result);
        return result;
    }

    private List<ModificationRule> findFulfilledRules(
            ConfigurableProductionPlan configurable, DeltaResult result) {
        List<ModificationRule> fulfilled = new ArrayList<>();
        for (ModificationRule rule : configurable.rules()) {
            if (result.fulfills(rule.condition())) {
                fulfilled.add(rule);
            }
        }
        return fulfilled;
    }
}
