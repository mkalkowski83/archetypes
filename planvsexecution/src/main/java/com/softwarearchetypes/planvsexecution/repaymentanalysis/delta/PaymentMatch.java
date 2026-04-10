package com.softwarearchetypes.planvsexecution.repaymentanalysis.delta;

import com.softwarearchetypes.planvsexecution.repaymentanalysis.Payment;
import com.softwarearchetypes.planvsexecution.repaymentanalysis.tolerance.MatchResult;
import com.softwarearchetypes.quantity.money.Money;
import java.util.List;

public record PaymentMatch(Payment planned, List<Payment> actual, MatchResult matchResult) {

    public Money totalActualAmount() {
        return actual.stream().map(Payment::amount).reduce(Money.zeroPln(), Money::add);
    }
}
