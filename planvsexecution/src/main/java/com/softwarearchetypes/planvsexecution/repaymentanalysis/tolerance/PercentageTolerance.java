package com.softwarearchetypes.planvsexecution.repaymentanalysis.tolerance;

import com.softwarearchetypes.planvsexecution.repaymentanalysis.Payment;
import com.softwarearchetypes.quantity.money.Money;
import com.softwarearchetypes.quantity.money.Percentage;
import java.math.BigDecimal;
import java.util.List;

class PercentageTolerance implements ToleranceStrategy {

    private final Percentage tolerance;

    PercentageTolerance(Percentage tolerance) {
        this.tolerance = tolerance;
    }

    @Override
    public MatchResult matches(Payment planned, List<Payment> actual) {
        Money totalActual =
                actual.stream().map(Payment::amount).reduce(Money.zeroPln(), Money::add);

        if (planned.amount().isZero()) {
            if (totalActual.isZero()) {
                return MatchResult.matched("Both amounts are zero");
            }
            return MatchResult.notMatched("Planned was zero but actual was " + totalActual);
        }

        Money maxDeviation = planned.amount().multiply(tolerance);
        Money difference = planned.amount().subtract(totalActual).abs();

        if (difference.isGreaterThan(maxDeviation)) {
            BigDecimal percentValue =
                    difference
                            .value()
                            .divide(planned.amount().value(), 4, java.math.RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("100"));
            Percentage actualPercentage = new Percentage(percentValue);
            return MatchResult.notMatched(
                    "Amount difference " + actualPercentage + " exceeds tolerance " + tolerance);
        }

        return MatchResult.matched("Amount within " + tolerance + " tolerance");
    }
}
