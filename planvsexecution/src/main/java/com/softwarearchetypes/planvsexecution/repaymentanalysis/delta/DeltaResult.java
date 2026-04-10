package com.softwarearchetypes.planvsexecution.repaymentanalysis.delta;

import com.softwarearchetypes.planvsexecution.repaymentanalysis.Payment;
import com.softwarearchetypes.planvsexecution.repaymentanalysis.modification.LatePaymentCondition;
import com.softwarearchetypes.planvsexecution.repaymentanalysis.modification.OnTimePaymentCondition;
import com.softwarearchetypes.planvsexecution.repaymentanalysis.modification.ScheduleModificationCondition;
import java.util.List;

public record DeltaResult(
        List<PaymentMatch> matched,
        List<Payment> unmatchedPlanned,
        List<Payment> unmatchedActual,
        DeltaStatistics statistics) {

    public boolean isPerfectMatch() {
        return unmatchedPlanned.isEmpty() && unmatchedActual.isEmpty();
    }

    public boolean hasUnderpayments() {
        return !unmatchedPlanned.isEmpty();
    }

    public boolean hasOverpayments() {
        return !unmatchedActual.isEmpty();
    }

    public int totalPlannedCount() {
        return matched.size() + unmatchedPlanned.size();
    }

    public double matchRate() {
        if (totalPlannedCount() == 0) {
            return 0.0;
        }
        return (double) matched.size() / totalPlannedCount();
    }

    public boolean fulfills(ScheduleModificationCondition condition) {
        if (condition instanceof LatePaymentCondition lateCondition) {
            return statistics.lateCount() >= lateCondition.minCount();
        }
        if (condition instanceof OnTimePaymentCondition onTimeCondition) {
            return statistics.onTimeCount() >= onTimeCondition.minCount();
        }
        return false;
    }
}
