package com.softwarearchetypes.planvsexecution.repaymentanalysis.tolerance;

import com.softwarearchetypes.planvsexecution.repaymentanalysis.Payment;
import java.time.temporal.ChronoUnit;
import java.util.List;

class DateTolerance implements ToleranceStrategy {

    private final long toleranceDays;

    DateTolerance(long toleranceDays) {
        if (toleranceDays < 0) {
            throw new IllegalArgumentException("Date tolerance must be non-negative");
        }
        this.toleranceDays = toleranceDays;
    }

    @Override
    public MatchResult matches(Payment planned, List<Payment> actual) {
        boolean allWithinTolerance =
                actual.stream()
                        .allMatch(
                                p ->
                                        Math.abs(ChronoUnit.DAYS.between(planned.when(), p.when()))
                                                <= toleranceDays);

        if (!allWithinTolerance) {
            return MatchResult.notMatched(
                    "Some payments exceed date tolerance of " + toleranceDays + " days");
        }

        return MatchResult.matched(
                "All payments within date tolerance: " + toleranceDays + " days");
    }
}
