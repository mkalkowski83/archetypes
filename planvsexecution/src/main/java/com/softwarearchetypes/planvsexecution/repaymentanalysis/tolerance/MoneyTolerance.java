package com.softwarearchetypes.planvsexecution.repaymentanalysis.tolerance;

import com.softwarearchetypes.planvsexecution.repaymentanalysis.Payment;
import com.softwarearchetypes.quantity.money.Money;
import java.util.List;

class MoneyTolerance implements ToleranceStrategy {

    private final Money tolerance;

    MoneyTolerance(Money tolerance) {
        if (tolerance.isNegative()) {
            throw new IllegalArgumentException("Money tolerance must be non-negative");
        }
        this.tolerance = tolerance;
    }

    @Override
    public MatchResult matches(Payment planned, List<Payment> actual) {
        Money totalActual =
                actual.stream().map(Payment::amount).reduce(Money.zeroPln(), Money::add);

        Money difference = planned.amount().subtract(totalActual).abs();

        if (difference.isGreaterThan(tolerance)) {
            return MatchResult.notMatched(
                    "Amount difference " + difference + " exceeds tolerance " + tolerance);
        }

        return MatchResult.matched(
                "Amount within tolerance: difference " + difference + " ≤ " + tolerance);
    }
}
