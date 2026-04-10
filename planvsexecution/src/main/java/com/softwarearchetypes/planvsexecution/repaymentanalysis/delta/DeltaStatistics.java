package com.softwarearchetypes.planvsexecution.repaymentanalysis.delta;

import com.softwarearchetypes.planvsexecution.repaymentanalysis.Payment;
import com.softwarearchetypes.quantity.money.Money;
import java.util.List;

public record DeltaStatistics(
        Money totalPlannedAmount,
        Money totalActualAmount,
        Money totalMatchedAmount,
        Money totalUnderpaidAmount,
        Money totalOverpaidAmount,
        int plannedCount,
        int actualCount,
        int matchedCount,
        int underpaidCount,
        int overpaidCount,
        int onTimeCount,
        int lateCount) {

    static DeltaStatistics calculate(
            List<PaymentMatch> matched,
            List<Payment> unmatchedPlanned,
            List<Payment> unmatchedActual) {
        Money totalPlanned =
                sumPayments(matched.stream().map(PaymentMatch::planned).toList())
                        .add(sumPayments(unmatchedPlanned));

        Money totalActual =
                matched.stream()
                        .map(PaymentMatch::totalActualAmount)
                        .reduce(Money.zeroPln(), Money::add)
                        .add(sumPayments(unmatchedActual));

        Money totalMatched =
                matched.stream()
                        .map(PaymentMatch::totalActualAmount)
                        .reduce(Money.zeroPln(), Money::add);

        Money totalUnderpaid = sumPayments(unmatchedPlanned);
        Money totalOverpaid = sumPayments(unmatchedActual);

        int onTime =
                (int)
                        matched.stream()
                                .filter(
                                        m ->
                                                m.actual().stream()
                                                        .allMatch(
                                                                p ->
                                                                        !p.when()
                                                                                .isAfter(
                                                                                        m.planned()
                                                                                                .when())))
                                .count();
        int late =
                (int)
                        matched.stream()
                                .filter(
                                        m ->
                                                m.actual().stream()
                                                        .anyMatch(
                                                                p ->
                                                                        p.when()
                                                                                .isAfter(
                                                                                        m.planned()
                                                                                                .when())))
                                .count();

        return new DeltaStatistics(
                totalPlanned,
                totalActual,
                totalMatched,
                totalUnderpaid,
                totalOverpaid,
                matched.size() + unmatchedPlanned.size(),
                matched.size() + unmatchedActual.size(),
                matched.size(),
                unmatchedPlanned.size(),
                unmatchedActual.size(),
                onTime,
                late);
    }

    private static Money sumPayments(List<Payment> payments) {
        return payments.stream().map(Payment::amount).reduce(Money.zeroPln(), Money::add);
    }

    public Money netDifference() {
        return totalActualAmount.subtract(totalPlannedAmount);
    }
}
