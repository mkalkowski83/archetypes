package com.softwarearchetypes.planvsexecution.repaymentanalysis;

import com.softwarearchetypes.planvsexecution.repaymentanalysis.delta.DeltaResult;
import com.softwarearchetypes.planvsexecution.repaymentanalysis.modification.ModificationRule;
import com.softwarearchetypes.planvsexecution.repaymentanalysis.tolerance.ToleranceStrategy;
import java.util.ArrayList;
import java.util.List;

public class ScheduleModificationOrchestrator {

    private final ScheduleAnalysisFacade analysisFacade;

    public ScheduleModificationOrchestrator(ScheduleAnalysisFacade analysisFacade) {
        this.analysisFacade = analysisFacade;
    }

    public DeltaResult analyze(
            ConfigurablePaymentSchedule plan,
            List<PaymentProcessed> events,
            ToleranceStrategy tolerance) {
        return analysisFacade.analyze(plan.activeSchedule(), events, tolerance);
    }

    public DeltaResult analyze(
            ConfigurablePaymentSchedule configurable,
            PaymentSchedule actual,
            ToleranceStrategy tolerance) {
        return analysisFacade.analyze(configurable.activeSchedule(), actual, tolerance);
    }

    public DeltaResult analyzeAndApply(
            ConfigurablePaymentSchedule plan,
            List<PaymentProcessed> events,
            ToleranceStrategy tolerance) {
        DeltaResult result = analyze(plan, events, tolerance);
        List<ModificationRule> fulfilledRules = findFulfilledRules(plan, result);
        plan = plan.fulfilled(fulfilledRules, result);
        // workaround - mutacja stanu planu tutaj i sprawdzenie w tescie czy przekazany plan jest
        // zmieniony (z persystencja po prostu ta asercja wyciagnelaby plan z bazy)
        // save(plan);
        return result;
    }

    public DeltaResult analyzeAndApply(
            ConfigurablePaymentSchedule configurable,
            PaymentSchedule actual,
            ToleranceStrategy tolerance) {
        DeltaResult result = analyze(configurable, actual, tolerance);
        List<ModificationRule> fulfilledRules = findFulfilledRules(configurable, result);
        configurable = configurable.fulfilled(fulfilledRules, result);
        // save(configurable);
        return result;
    }

    private List<ModificationRule> findFulfilledRules(
            ConfigurablePaymentSchedule configurable, DeltaResult result) {
        List<ModificationRule> fulfilled = new ArrayList<>();
        for (ModificationRule rule : configurable.rules()) {
            if (result.fulfills(rule.condition())) {
                fulfilled.add(rule);
            }
        }
        return fulfilled;
    }
}
