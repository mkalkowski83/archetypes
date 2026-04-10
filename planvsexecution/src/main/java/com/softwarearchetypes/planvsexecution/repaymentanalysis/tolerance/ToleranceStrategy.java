package com.softwarearchetypes.planvsexecution.repaymentanalysis.tolerance;

import com.softwarearchetypes.planvsexecution.repaymentanalysis.Payment;
import java.util.List;

@FunctionalInterface
public interface ToleranceStrategy {

    MatchResult matches(Payment planned, List<Payment> actual);

    default ToleranceStrategy and(ToleranceStrategy other) {
        return (planned, actual) -> {
            MatchResult thisResult = this.matches(planned, actual);
            if (!thisResult.matched()) {
                return thisResult;
            }
            MatchResult otherResult = other.matches(planned, actual);
            if (!otherResult.matched()) {
                return otherResult;
            }
            return MatchResult.matched(
                    "Both criteria matched: "
                            + thisResult.reason()
                            + " and "
                            + otherResult.reason());
        };
    }

    default ToleranceStrategy or(ToleranceStrategy other) {
        return (planned, actual) -> {
            MatchResult thisResult = this.matches(planned, actual);
            if (thisResult.matched()) {
                return thisResult;
            }
            MatchResult otherResult = other.matches(planned, actual);
            if (otherResult.matched()) {
                return otherResult;
            }
            return MatchResult.notMatched(
                    "Neither criteria matched: "
                            + thisResult.reason()
                            + " or "
                            + otherResult.reason());
        };
    }

    default ToleranceStrategy negate() {
        return (planned, actual) -> {
            MatchResult result = this.matches(planned, actual);
            if (result.matched()) {
                return MatchResult.notMatched("Negated: " + result.reason());
            } else {
                return MatchResult.matched("Negated: " + result.reason());
            }
        };
    }
}
