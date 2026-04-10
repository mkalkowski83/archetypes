package com.softwarearchetypes.pricing;

import static com.softwarearchetypes.pricing.ApplicabilityConstraint.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for ApplicabilityConstraint implementations.
 *
 * <p>Each test creates a PricingContext via PricingContext.from(Parameters.of(...)) — the same path
 * used during real calculation.
 */
class ApplicabilityConstraintTest {

    // ================================================================
    // alwaysTrue
    // ================================================================

    @Test
    void alwaysTrue_isSatisfiedByAnyContext() {
        assertThat(alwaysTrue().isSatisfiedBy(emptyCtx())).isTrue();
        assertThat(alwaysTrue().isSatisfiedBy(ctx("anything", "value"))).isTrue();
    }

    // ================================================================
    // equalsTo
    // ================================================================

    @Test
    void equalsTo_matchesExactStringValue() {
        ApplicabilityConstraint constraint = equalsTo("cargo-type", "hazmat");

        assertThat(constraint.isSatisfiedBy(ctx("cargo-type", "hazmat"))).isTrue();
        assertThat(constraint.isSatisfiedBy(ctx("cargo-type", "standard"))).isFalse();
    }

    @Test
    void equalsTo_returnsFalseWhenParameterAbsent() {
        assertThat(equalsTo("cargo-type", "hazmat").isSatisfiedBy(emptyCtx())).isFalse();
    }

    // ================================================================
    // in
    // ================================================================

    @Test
    void in_matchesAnyValueFromAllowedSet() {
        ApplicabilityConstraint constraint = in("zone", "A", "B", "C");

        assertThat(constraint.isSatisfiedBy(ctx("zone", "A"))).isTrue();
        assertThat(constraint.isSatisfiedBy(ctx("zone", "C"))).isTrue();
        assertThat(constraint.isSatisfiedBy(ctx("zone", "D"))).isFalse();
    }

    @Test
    void in_returnsFalseWhenParameterAbsent() {
        assertThat(in("zone", "A", "B").isSatisfiedBy(emptyCtx())).isFalse();
    }

    // ================================================================
    // greaterThan
    // ================================================================

    @Test
    void greaterThan_isSatisfiedStrictlyAboveThreshold() {
        ApplicabilityConstraint constraint = greaterThan("weight", 10);

        assertThat(constraint.isSatisfiedBy(ctx("weight", BigDecimal.valueOf(11)))).isTrue();
        assertThat(constraint.isSatisfiedBy(ctx("weight", BigDecimal.valueOf(10))))
                .isFalse(); // exclusive
        assertThat(constraint.isSatisfiedBy(ctx("weight", BigDecimal.valueOf(9)))).isFalse();
    }

    @Test
    void greaterThan_returnsFalseWhenParameterAbsent() {
        assertThat(greaterThan("weight", 10).isSatisfiedBy(emptyCtx())).isFalse();
    }

    @Test
    void greaterThan_returnsFalseForNonNumericValue() {
        assertThat(greaterThan("weight", 10).isSatisfiedBy(ctx("weight", "heavy"))).isFalse();
    }

    // ================================================================
    // greaterThanOrEqualTo
    // ================================================================

    @Test
    void greaterThanOrEqualTo_isSatisfiedAtAndAboveThreshold() {
        ApplicabilityConstraint constraint = greaterThanOrEqualTo("quantity", 5);

        assertThat(constraint.isSatisfiedBy(ctx("quantity", BigDecimal.valueOf(5))))
                .isTrue(); // boundary
        assertThat(constraint.isSatisfiedBy(ctx("quantity", BigDecimal.valueOf(10)))).isTrue();
        assertThat(constraint.isSatisfiedBy(ctx("quantity", BigDecimal.valueOf(4)))).isFalse();
    }

    // ================================================================
    // lessThan
    // ================================================================

    @Test
    void lessThan_isSatisfiedStrictlyBelowThreshold() {
        ApplicabilityConstraint constraint = lessThan("sessions", 5);

        assertThat(constraint.isSatisfiedBy(ctx("sessions", BigDecimal.valueOf(4)))).isTrue();
        assertThat(constraint.isSatisfiedBy(ctx("sessions", BigDecimal.valueOf(5))))
                .isFalse(); // exclusive
        assertThat(constraint.isSatisfiedBy(ctx("sessions", BigDecimal.valueOf(6)))).isFalse();
    }

    // ================================================================
    // lessThanOrEqualTo
    // ================================================================

    @Test
    void lessThanOrEqualTo_isSatisfiedAtAndBelowThreshold() {
        ApplicabilityConstraint constraint = lessThanOrEqualTo("quantity", 100);

        assertThat(constraint.isSatisfiedBy(ctx("quantity", BigDecimal.valueOf(100))))
                .isTrue(); // boundary
        assertThat(constraint.isSatisfiedBy(ctx("quantity", BigDecimal.valueOf(50)))).isTrue();
        assertThat(constraint.isSatisfiedBy(ctx("quantity", BigDecimal.valueOf(101)))).isFalse();
    }

    // ================================================================
    // between
    // ================================================================

    @Test
    void between_isSatisfiedWithinInclusiveBounds() {
        ApplicabilityConstraint constraint = between("weight", 5, 30);

        assertThat(constraint.isSatisfiedBy(ctx("weight", BigDecimal.valueOf(5))))
                .isTrue(); // min inclusive
        assertThat(constraint.isSatisfiedBy(ctx("weight", BigDecimal.valueOf(17)))).isTrue();
        assertThat(constraint.isSatisfiedBy(ctx("weight", BigDecimal.valueOf(30))))
                .isTrue(); // max inclusive
        assertThat(constraint.isSatisfiedBy(ctx("weight", BigDecimal.valueOf(4)))).isFalse();
        assertThat(constraint.isSatisfiedBy(ctx("weight", BigDecimal.valueOf(31)))).isFalse();
    }

    @Test
    void between_returnsFalseWhenParameterAbsent() {
        assertThat(between("weight", 5, 30).isSatisfiedBy(emptyCtx())).isFalse();
    }

    // ================================================================
    // and
    // ================================================================

    @Test
    void and_requiresAllConstraintsSatisfied() {
        ApplicabilityConstraint constraint =
                and(equalsTo("type", "B2C"), greaterThan("sessions", 10));

        assertThat(constraint.isSatisfiedBy(ctx("type", "B2C", "sessions", BigDecimal.valueOf(15))))
                .isTrue();
        assertThat(constraint.isSatisfiedBy(ctx("type", "B2C", "sessions", BigDecimal.valueOf(5))))
                .isFalse();
        assertThat(constraint.isSatisfiedBy(ctx("type", "B2B", "sessions", BigDecimal.valueOf(15))))
                .isFalse();
    }

    // ================================================================
    // or
    // ================================================================

    @Test
    void or_isSatisfiedByAtLeastOneConstraint() {
        ApplicabilityConstraint constraint =
                or(equalsTo("status", "gold"), equalsTo("status", "platinum"));

        assertThat(constraint.isSatisfiedBy(ctx("status", "gold"))).isTrue();
        assertThat(constraint.isSatisfiedBy(ctx("status", "platinum"))).isTrue();
        assertThat(constraint.isSatisfiedBy(ctx("status", "silver"))).isFalse();
    }

    // ================================================================
    // not
    // ================================================================

    @Test
    void not_negatesConstraint() {
        ApplicabilityConstraint constraint = not(equalsTo("excluded", "true"));

        assertThat(constraint.isSatisfiedBy(ctx("excluded", "false"))).isTrue();
        assertThat(constraint.isSatisfiedBy(emptyCtx()))
                .isTrue(); // absent → equalsTo false → not(false) = true
        assertThat(constraint.isSatisfiedBy(ctx("excluded", "true"))).isFalse();
    }

    // ================================================================
    // logical composition
    // ================================================================

    @Test
    void shouldSupportDeepComposition_orOfAnds() {
        // (B2C AND weight >= 5) OR (B2B AND weight >= 3)
        ApplicabilityConstraint constraint =
                or(
                        and(equalsTo("type", "B2C"), greaterThanOrEqualTo("weight", 5)),
                        and(equalsTo("type", "B2B"), greaterThanOrEqualTo("weight", 3)));

        assertThat(constraint.isSatisfiedBy(ctx("type", "B2C", "weight", BigDecimal.valueOf(7))))
                .isTrue();
        assertThat(constraint.isSatisfiedBy(ctx("type", "B2B", "weight", BigDecimal.valueOf(4))))
                .isTrue();
        assertThat(constraint.isSatisfiedBy(ctx("type", "B2C", "weight", BigDecimal.valueOf(2))))
                .isFalse();
        assertThat(constraint.isSatisfiedBy(ctx("type", "B2B", "weight", BigDecimal.valueOf(1))))
                .isFalse();
    }

    @Test
    void shouldSupportNotOfAnd() {
        // NOT (gold AND quantity >= 100) — not a bulk gold customer
        ApplicabilityConstraint constraint =
                not(
                        and(
                                equalsTo("status", "gold"),
                                greaterThanOrEqualTo("quantity", BigDecimal.valueOf(100))));

        assertThat(
                        constraint.isSatisfiedBy(
                                ctx("status", "silver", "quantity", BigDecimal.valueOf(50))))
                .isTrue();
        assertThat(
                        constraint.isSatisfiedBy(
                                ctx("status", "gold", "quantity", BigDecimal.valueOf(50))))
                .isTrue();
        assertThat(
                        constraint.isSatisfiedBy(
                                ctx("status", "gold", "quantity", BigDecimal.valueOf(200))))
                .isFalse();
    }

    // ================================================================
    // helpers
    // ================================================================

    private PricingContext emptyCtx() {
        return PricingContext.from(Parameters.empty());
    }

    private PricingContext ctx(String k1, Object v1) {
        return PricingContext.from(Parameters.of(k1, v1));
    }

    private PricingContext ctx(String k1, Object v1, String k2, Object v2) {
        return PricingContext.from(Parameters.of(k1, v1, k2, v2));
    }
}
