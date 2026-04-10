package com.softwarearchetypes.planvsexecution.repaymentanalysis.tolerance;

import com.softwarearchetypes.planvsexecution.repaymentanalysis.Payment;
import com.softwarearchetypes.quantity.money.Money;
import java.time.Instant;
import java.util.List;

class PartialPaymentsStrategy implements ToleranceStrategy {

    private final Money tolerance;
    private final Instant deadline;

    PartialPaymentsStrategy(Money tolerance, Instant deadline) {
        if (tolerance.isNegative()) {
            throw new IllegalArgumentException("Money tolerance must be non-negative");
        }
        this.tolerance = tolerance;
        this.deadline = deadline;
    }

    @Override
    public MatchResult matches(Payment planned, List<Payment> actual) {
        Money totalActual =
                actual.stream()
                        .filter(p -> !p.when().isAfter(deadline))
                        .map(Payment::amount)
                        .reduce(Money.zeroPln(), Money::add);

        Money difference = planned.amount().subtract(totalActual).abs();

        if (difference.isGreaterThan(tolerance)) {
            return MatchResult.notMatched(
                    "Partial payments sum "
                            + totalActual
                            + " differs from planned "
                            + planned.amount()
                            + " by "
                            + difference
                            + " which exceeds tolerance "
                            + tolerance);
        }

        int paymentsAfterDeadline =
                (int) actual.stream().filter(p -> p.when().isAfter(deadline)).count();

        if (paymentsAfterDeadline > 0) {
            return MatchResult.matched(
                    "Partial payments within tolerance: "
                            + actual.size()
                            + " payments totaling "
                            + totalActual
                            + " ("
                            + paymentsAfterDeadline
                            + " after deadline excluded)");
        }

        return MatchResult.matched(
                "Partial payments within tolerance: "
                        + actual.size()
                        + " payments totaling "
                        + totalActual);
    }
}
