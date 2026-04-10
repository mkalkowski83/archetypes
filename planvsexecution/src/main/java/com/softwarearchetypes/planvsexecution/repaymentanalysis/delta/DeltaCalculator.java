package com.softwarearchetypes.planvsexecution.repaymentanalysis.delta;

import com.softwarearchetypes.planvsexecution.repaymentanalysis.Payment;
import com.softwarearchetypes.planvsexecution.repaymentanalysis.PaymentSchedule;
import com.softwarearchetypes.planvsexecution.repaymentanalysis.tolerance.MatchResult;
import com.softwarearchetypes.planvsexecution.repaymentanalysis.tolerance.ToleranceStrategy;
import java.util.ArrayList;
import java.util.List;

public class DeltaCalculator {

    private final ToleranceStrategy toleranceStrategy;

    public DeltaCalculator(ToleranceStrategy toleranceStrategy) {
        this.toleranceStrategy = toleranceStrategy;
    }

    public DeltaResult calculate(PaymentSchedule planned, PaymentSchedule actual) {
        List<PaymentMatch> matched = new ArrayList<>();
        List<Payment> unmatchedPlanned = new ArrayList<>();
        List<Payment> unmatchedActual = new ArrayList<>(actual.payments());

        for (Payment plannedPayment : planned.payments()) {
            PaymentMatch match = findBestMatch(plannedPayment, unmatchedActual);

            if (match != null) {
                matched.add(match);
                unmatchedActual.removeAll(match.actual());
            } else {
                unmatchedPlanned.add(plannedPayment);
            }
        }

        return new DeltaResult(
                matched,
                unmatchedPlanned,
                unmatchedActual,
                DeltaStatistics.calculate(matched, unmatchedPlanned, unmatchedActual));
    }

    private PaymentMatch findBestMatch(Payment planned, List<Payment> candidates) {
        for (int size = 1; size <= candidates.size(); size++) {
            for (int start = 0; start <= candidates.size() - size; start++) {
                List<Payment> group = candidates.subList(start, start + size);
                MatchResult matchResult = toleranceStrategy.matches(planned, group);
                if (matchResult.matched()) {
                    return new PaymentMatch(planned, new ArrayList<>(group), matchResult);
                }
            }
        }
        return null;
    }
}
