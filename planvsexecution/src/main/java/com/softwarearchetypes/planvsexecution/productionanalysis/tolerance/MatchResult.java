package com.softwarearchetypes.planvsexecution.productionanalysis.tolerance;

/**
 * Result of matching a planned order against actual production(s). Encapsulates whether they match
 * and why/why not.
 */
public record MatchResult(boolean matched, String reason) {

    public static MatchResult matched(String reason) {
        return new MatchResult(true, reason);
    }

    public static MatchResult notMatched(String reason) {
        return new MatchResult(false, reason);
    }

    @Override
    public String toString() {
        return matched ? "✓ " + reason : "✗ " + reason;
    }
}
