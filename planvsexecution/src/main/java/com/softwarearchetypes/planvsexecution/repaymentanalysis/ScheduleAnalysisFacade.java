package com.softwarearchetypes.planvsexecution.repaymentanalysis;

import com.softwarearchetypes.planvsexecution.repaymentanalysis.delta.DeltaCalculator;
import com.softwarearchetypes.planvsexecution.repaymentanalysis.delta.DeltaResult;
import com.softwarearchetypes.planvsexecution.repaymentanalysis.tolerance.ToleranceStrategy;
import java.util.List;

public class ScheduleAnalysisFacade {

    public DeltaResult analyze(
            PaymentSchedule planned, List<PaymentProcessed> events, ToleranceStrategy tolerance) {
        return analyze(planned, PaymentSchedule.fromEvents(events), tolerance);
    }

    public DeltaResult analyze(
            PaymentSchedule planned, PaymentSchedule actual, ToleranceStrategy tolerance) {
        DeltaCalculator calculator = new DeltaCalculator(tolerance);
        return calculator.calculate(planned, actual);
    }
}
