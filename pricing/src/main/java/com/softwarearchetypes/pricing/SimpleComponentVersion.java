package com.softwarearchetypes.pricing;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

/**
 * Version of a SimpleComponent — represents calculator configuration valid during a time period and
 * applicable under specific business conditions.
 *
 * <p>Three orthogonal axes of a pricing component version:
 *
 * <ul>
 *   <li><b>Calculator</b> — how do we calculate? (the math)
 *   <li><b>Validity</b> — when does it apply? (time)
 *   <li><b>Applicability</b> — for whom / under what conditions? (business rules)
 * </ul>
 *
 * <p>A version fires if and only if BOTH conditions hold:
 *
 * <pre>
 *   validity.isValidAt(context.timestamp())
 *       &amp;&amp; applicabilityConstraint.isSatisfiedBy(context)
 * </pre>
 *
 * Example:
 *
 * <pre>
 *   new SimpleComponentVersion(
 *       fixedPerMinute,
 *       Map.of(),
 *       and(greaterThan("minutes", 10), equalsTo("customerType", "B2C")),
 *       Validity.from(LocalDateTime.of(2024, 5, 1, 0, 0)),
 *       now(clock)
 *   )
 * </pre>
 *
 * Interpretation: "From May, charge per minute — but only for B2C customers and only when the
 * session exceeds 10 minutes."
 */
record SimpleComponentVersion(
        Calculator calculator,
        Map<String, String> parameterMappings,
        ApplicabilityConstraint applicabilityConstraint,
        Validity validity,
        LocalDateTime definedAt)
        implements ComponentVersion {

    public SimpleComponentVersion {
        parameterMappings = Map.copyOf(parameterMappings);
        Objects.requireNonNull(definedAt, "definedAt cannot be null");
        Objects.requireNonNull(applicabilityConstraint, "applicabilityConstraint cannot be null");
    }

    /** Backward-compatible constructor — component always applicable (no business condition). */
    public SimpleComponentVersion(
            Calculator calculator,
            Map<String, String> parameterMappings,
            Validity validity,
            LocalDateTime definedAt) {
        this(
                calculator,
                parameterMappings,
                ApplicabilityConstraint.alwaysTrue(),
                validity,
                definedAt);
    }

    /**
     * Returns true when this version should be used for the given pricing context. Combines the
     * time dimension (validity) with the business dimension (applicability).
     */
    public boolean isApplicableFor(PricingContext context) {
        return validity.isValidAt(context.timestamp())
                && applicabilityConstraint.isSatisfiedBy(context);
    }

    // ---- factory helpers ----

    /** Create a version with explicit applicability constraint. */
    public static SimpleComponentVersion of(
            Calculator calculator,
            Map<String, String> parameterMappings,
            ApplicabilityConstraint applicabilityConstraint,
            Validity validity,
            Clock clock) {
        return new SimpleComponentVersion(
                calculator,
                parameterMappings,
                applicabilityConstraint,
                validity,
                LocalDateTime.now(clock));
    }

    /** Create a version that always applies (no business condition). */
    public static SimpleComponentVersion of(
            Calculator calculator,
            Map<String, String> parameterMappings,
            Validity validity,
            Clock clock) {
        return new SimpleComponentVersion(
                calculator,
                parameterMappings,
                ApplicabilityConstraint.alwaysTrue(),
                validity,
                LocalDateTime.now(clock));
    }

    /** Create a version without parameter mappings that always applies. */
    public static SimpleComponentVersion of(Calculator calculator, Validity validity, Clock clock) {
        return new SimpleComponentVersion(
                calculator,
                Map.of(),
                ApplicabilityConstraint.alwaysTrue(),
                validity,
                LocalDateTime.now(clock));
    }
}
