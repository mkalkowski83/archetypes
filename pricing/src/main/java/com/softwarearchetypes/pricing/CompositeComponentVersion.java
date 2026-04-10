package com.softwarearchetypes.pricing;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Version of a CompositeComponent - represents composition of children valid during a time period.
 *
 * <p>Three orthogonal axes: - Children + Dependencies: which components and how they exchange
 * values (composition) - Validity: when this composition is active (time) -
 * ApplicabilityConstraint: for whom / under what conditions (business rules)
 *
 * <p>A version fires if and only if BOTH conditions hold:
 *
 * <pre>
 *   validity.isValidAt(context.timestamp())
 *       &amp;&amp; applicabilityConstraint.isSatisfiedBy(context)
 * </pre>
 *
 * Example - eMobility pricing changing over time: - Version 1 (Jan-Apr): children = [EnergyCharge,
 * ParkingFee], valid [2024-01-01, 2024-05-01) - Version 2 (May+): children = [EnergyCharge,
 * ParkingFee, SeasonalSurcharge], valid [2024-05-01, ∞)
 *
 * <p>Each child component maintains its own version history independently.
 */
record CompositeComponentVersion(
        List<Component> children,
        Map<ComponentId, Map<String, ParameterValue>> dependencies,
        ApplicabilityConstraint applicabilityConstraint,
        Validity validity,
        LocalDateTime definedAt)
        implements ComponentVersion {

    public CompositeComponentVersion {
        children = List.copyOf(children);
        dependencies =
                Map.copyOf(
                        dependencies.entrySet().stream()
                                .collect(
                                        Collectors.toMap(
                                                Map.Entry::getKey, e -> Map.copyOf(e.getValue()))));
        Objects.requireNonNull(applicabilityConstraint, "applicabilityConstraint cannot be null");
        Objects.requireNonNull(definedAt, "definedAt cannot be null");
    }

    /** Backward-compatible constructor — composite always applicable (no business condition). */
    public CompositeComponentVersion(
            List<Component> children,
            Map<ComponentId, Map<String, ParameterValue>> dependencies,
            Validity validity,
            LocalDateTime definedAt) {
        this(children, dependencies, ApplicabilityConstraint.alwaysTrue(), validity, definedAt);
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

    /** Create version with explicit applicability constraint. */
    public static CompositeComponentVersion of(
            List<Component> children,
            Map<ComponentId, Map<String, ParameterValue>> dependencies,
            ApplicabilityConstraint applicabilityConstraint,
            Validity validity,
            Clock clock) {
        return new CompositeComponentVersion(
                children,
                dependencies,
                applicabilityConstraint,
                validity,
                LocalDateTime.now(clock));
    }

    /** Create version with clock - definedAt will be set to now(clock). */
    public static CompositeComponentVersion of(
            List<Component> children,
            Map<ComponentId, Map<String, ParameterValue>> dependencies,
            Validity validity,
            Clock clock) {
        return new CompositeComponentVersion(
                children,
                dependencies,
                ApplicabilityConstraint.alwaysTrue(),
                validity,
                LocalDateTime.now(clock));
    }

    /** Create version without dependencies (children are independent). */
    public static CompositeComponentVersion of(
            List<Component> children, Validity validity, Clock clock) {
        return new CompositeComponentVersion(
                children,
                Map.of(),
                ApplicabilityConstraint.alwaysTrue(),
                validity,
                LocalDateTime.now(clock));
    }

    /** Create version without dependencies (children are independent). */
    public static CompositeComponentVersion of(
            Validity validity, Clock clock, Component... children) {
        return new CompositeComponentVersion(
                List.of(children),
                Map.of(),
                ApplicabilityConstraint.alwaysTrue(),
                validity,
                LocalDateTime.now(clock));
    }
}
