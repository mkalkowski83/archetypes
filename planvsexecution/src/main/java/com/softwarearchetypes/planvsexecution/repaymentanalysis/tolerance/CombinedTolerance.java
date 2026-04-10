package com.softwarearchetypes.planvsexecution.repaymentanalysis.tolerance;

import com.softwarearchetypes.planvsexecution.repaymentanalysis.Payment;
import java.util.List;

class CombinedTolerance implements ToleranceStrategy {

    private final ToleranceStrategy moneyStrategy;
    private final ToleranceStrategy dateStrategy;

    CombinedTolerance(ToleranceStrategy moneyStrategy, ToleranceStrategy dateStrategy) {
        this.moneyStrategy = moneyStrategy;
        this.dateStrategy = dateStrategy;
    }

    @Override
    public MatchResult matches(Payment planned, List<Payment> actual) {
        return moneyStrategy.and(dateStrategy).matches(planned, actual);
    }
}
