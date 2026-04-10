package com.softwarearchetypes.planvsexecution.repaymentanalysis.tolerance;

import com.softwarearchetypes.planvsexecution.repaymentanalysis.Payment;
import java.util.List;

class ExactMatch implements ToleranceStrategy {

    @Override
    public MatchResult matches(Payment planned, List<Payment> actual) {
        if (actual.size() != 1) {
            return MatchResult.notMatched(
                    "Exact match requires single payment, got " + actual.size());
        }

        Payment actualPayment = actual.get(0);
        boolean amountMatches = planned.amount().equals(actualPayment.amount());
        boolean dateMatches = planned.when().equals(actualPayment.when());

        if (amountMatches && dateMatches) {
            return MatchResult.matched("Exact match on amount and date");
        }

        if (!amountMatches && !dateMatches) {
            return MatchResult.notMatched(
                    "Amount differs: expected "
                            + planned.amount()
                            + ", got "
                            + actualPayment.amount()
                            + "; Date differs: expected "
                            + planned.when()
                            + ", got "
                            + actualPayment.when());
        }

        if (!amountMatches) {
            return MatchResult.notMatched(
                    "Amount differs: expected "
                            + planned.amount()
                            + ", got "
                            + actualPayment.amount());
        }

        return MatchResult.notMatched(
                "Date differs: expected " + planned.when() + ", got " + actualPayment.when());
    }
}
