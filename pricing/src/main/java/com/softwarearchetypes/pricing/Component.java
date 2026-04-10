package com.softwarearchetypes.pricing;

import com.softwarearchetypes.quantity.money.Money;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Component represents a semantic part of a price calculation. Unlike CompositeFunctionCalculator
 * which composes functions mathematically (piecewise), Component composes prices semantically (base
 * price + markup + discount).
 *
 * <p>Components can depend on each other - one component's calculated value can be used as input
 * parameter for another component's calculator.
 */
public sealed interface Component permits SimpleComponent, CompositeComponent {

    ComponentId id();

    String name();

    /**
     * Calculate this component's contribution to the total price. Delegates to
     * calculateBreakdown().total().
     *
     * @param parameters input parameters for calculation
     * @return calculated money amount for this component
     */
    default Money calculate(Parameters parameters) {
        return calculateBreakdown(parameters).total();
    }

    /**
     * Calculate with automatic conversion to target interpretation. For SimpleComponent: wraps
     * calculator with appropriate adapter if needed. For CompositeComponent: delegates to children
     * with target interpretation.
     *
     * @param parameters input parameters for calculation
     * @param targetInterpretation desired price interpretation
     * @return calculated money amount in target interpretation
     */
    Money calculate(Parameters parameters, Interpretation targetInterpretation);

    /**
     * Get the price interpretation of this component. For SimpleComponent: delegates to wrapped
     * calculator For CompositeComponent: always returns TOTAL
     */
    Interpretation interpretation();

    /** Calculate and return breakdown showing individual component contributions. */
    default ComponentBreakdown calculateBreakdown(Parameters parameters) {
        return calculateBreakdown(parameters, this.interpretation());
    }

    /**
     * Calculate breakdown with automatic conversion to target interpretation.
     *
     * @param parameters input parameters for calculation
     * @param targetInterpretation desired price interpretation
     * @return breakdown with values in target interpretation
     */
    ComponentBreakdown calculateBreakdown(
            Parameters parameters, Interpretation targetInterpretation);
}

/**
 * SimpleComponent with temporal versioning. Each component maintains a history of versions, each
 * valid during a specific time period.
 *
 * <p>When calculating, the component automatically selects the version valid at the timestamp
 * provided in parameters ("timestamp"), or falls back to current time if not provided.
 *
 * <p>When multiple versions overlap, the one with the youngest validFrom takes precedence.
 *
 * <p>Example: - Version 1: Base price 100 PLN, valid from 2024-01-01 forever - Version 2: Discount
 * 80 PLN, valid from 2024-02-01 to 2024-03-01 - After 2024-03-01: automatically reverts to Version
 * 1
 */
record SimpleComponent(ComponentId id, String name, List<SimpleComponentVersion> versions)
        implements Component {

    public SimpleComponent {
        versions = List.copyOf(versions);
        if (versions.isEmpty()) {
            throw new IllegalArgumentException("Component must have at least one version");
        }
    }

    /** Factory: Create SimpleComponent with initial version and applicability constraint. */
    public static SimpleComponent withInitialVersion(
            String name,
            Calculator calculator,
            Map<String, String> parameterMappings,
            ApplicabilityConstraint applicabilityConstraint,
            Validity validity,
            Clock clock) {
        SimpleComponentVersion initialVersion =
                new SimpleComponentVersion(
                        calculator,
                        parameterMappings,
                        applicabilityConstraint,
                        validity,
                        LocalDateTime.now(clock));
        return new SimpleComponent(ComponentId.generate(), name, List.of(initialVersion));
    }

    /** Factory: Create SimpleComponent with initial version (always applicable). */
    public static SimpleComponent withInitialVersion(
            String name,
            Calculator calculator,
            Map<String, String> parameterMappings,
            Validity validity,
            Clock clock) {
        return withInitialVersion(
                name,
                calculator,
                parameterMappings,
                ApplicabilityConstraint.alwaysTrue(),
                validity,
                clock);
    }

    /**
     * Factory: Create SimpleComponent with initial version (no parameter mappings, always
     * applicable).
     */
    public static SimpleComponent withInitialVersion(
            String name, Calculator calculator, Validity validity, Clock clock) {
        return withInitialVersion(
                name, calculator, Map.of(), ApplicabilityConstraint.alwaysTrue(), validity, clock);
    }

    /**
     * Backward compatibility: Create SimpleComponent valid "always" (from MIN to MAX). For testing
     * and simple use cases where temporal versioning is not needed.
     */
    public static SimpleComponent of(String name, Calculator calculator) {
        return withInitialVersion(
                name, calculator, Map.of(), Validity.always(), Clock.systemDefaultZone());
    }

    /**
     * Backward compatibility: Create SimpleComponent valid "always" with parameter mappings. For
     * testing and simple use cases where temporal versioning is not needed.
     */
    public static SimpleComponent of(
            String name, Calculator calculator, Map<String, String> parameterMappings) {
        return withInitialVersion(
                name, calculator, parameterMappings, Validity.always(), Clock.systemDefaultZone());
    }

    /**
     * Add new version to this component (immutable operation). Uses default validation strategy:
     * REJECT_IDENTICAL.
     *
     * @param newVersion version to add
     * @return new SimpleComponent with added version
     * @throws IllegalArgumentException if version with identical validity already exists
     */
    public SimpleComponent updateWith(SimpleComponentVersion newVersion) {
        return updateWith(newVersion, VersionUpdateStrategy.REJECT_IDENTICAL);
    }

    /**
     * Add new version to this component with custom validation strategy (immutable operation).
     *
     * @param newVersion version to add
     * @param strategy validation strategy for version conflicts
     * @return new SimpleComponent with added version
     * @throws IllegalArgumentException if validation fails
     */
    public SimpleComponent updateWith(
            SimpleComponentVersion newVersion, VersionUpdateStrategy strategy) {
        strategy.validate(versions, newVersion.validity());

        List<SimpleComponentVersion> updated = new ArrayList<>(versions);
        updated.add(newVersion);
        return new SimpleComponent(id, name, updated);
    }

    @Override
    public Interpretation interpretation() {
        // All versions should have same interpretation (not enforced, but recommended)
        // Return interpretation of first version as representative
        return versions.get(0).calculator().interpretation();
    }

    @Override
    public Money calculate(Parameters parameters, Interpretation targetInterpretation) {
        PricingContext context = PricingContext.from(parameters);
        SimpleComponentVersion version = versionAt(context.timestamp());

        if (!version.isApplicableFor(context)) {
            return Money.pln(BigDecimal.ZERO);
        }

        Parameters transformedParams = transformParameters(parameters, version.parameterMappings());
        Calculator adaptedCalculator =
                InterpretationAdapters.adapt(version.calculator(), targetInterpretation);
        return adaptedCalculator.calculate(transformedParams);
    }

    @Override
    public ComponentBreakdown calculateBreakdown(
            Parameters parameters, Interpretation targetInterpretation) {
        Money result = this.calculate(parameters, targetInterpretation);
        return new ComponentBreakdown(name, result, List.of());
    }

    /**
     * Find version valid at given point in time. When multiple versions are valid, returns the one
     * with youngest validFrom. If validFrom is identical, uses definedAt as tiebreaker (youngest
     * wins).
     */
    private SimpleComponentVersion versionAt(LocalDateTime time) {
        return versions.stream()
                .filter(v -> v.validity().isValidAt(time))
                .max(
                        Comparator.comparing((SimpleComponentVersion v) -> v.validity().validFrom())
                                .thenComparing(SimpleComponentVersion::definedAt))
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "No version of component '%s' (%s) valid at %s"
                                                .formatted(name, id, time)));
    }

    /** Transform parameters from component parameter names to calculator parameter names. */
    private Parameters transformParameters(
            Parameters original, Map<String, String> parameterMappings) {
        if (parameterMappings.isEmpty()) {
            return original;
        }

        Parameters transformed = Parameters.empty();

        // Apply mappings
        for (Map.Entry<String, String> entry : parameterMappings.entrySet()) {
            String componentParam = entry.getKey();
            String calculatorParam = entry.getValue();

            if (original.contains(componentParam)) {
                transformed = transformed.with(calculatorParam, original.get(componentParam));
            }
        }

        // Copy unmapped parameters
        for (String key : original.keys()) {
            if (!transformed.contains(key) && !parameterMappings.containsKey(key)) {
                transformed = transformed.with(key, original.get(key));
            }
        }

        return transformed;
    }

    /** Returns required parameter names for this component (from first version). */
    public Set<String> requiredParameters() {
        SimpleComponentVersion firstVersion = versions.get(0);
        if (!firstVersion.parameterMappings().isEmpty()) {
            return firstVersion.parameterMappings().keySet();
        }
        return firstVersion.calculator().getType().requiredCalculationFields();
    }
}

/**
 * CompositeComponent with temporal versioning. Each composite maintains a history of different
 * compositions (children + dependencies).
 *
 * <p>Over time, the composition can change: - Version 1: sum of [BasePrice, Tax] - Version 2: sum
 * of [BasePrice, Tax, SeasonalSurcharge] (added new child)
 *
 * <p>When calculating, the component automatically selects the version valid at the timestamp
 * provided in parameters ("timestamp"), or falls back to current time if not provided.
 *
 * <p>Example use case: - eMobility pricing: add parking fee starting May 1st - Banking: add new
 * monthly fee from next quarter
 */
record CompositeComponent(ComponentId id, String name, List<CompositeComponentVersion> versions)
        implements Component {

    public CompositeComponent {
        versions = List.copyOf(versions);
        if (versions.isEmpty()) {
            throw new IllegalArgumentException("Component must have at least one version");
        }
    }

    /** Factory: Create CompositeComponent with initial version and applicability constraint. */
    public static CompositeComponent withInitialVersion(
            String name,
            List<Component> children,
            Map<ComponentId, Map<String, ParameterValue>> dependencies,
            ApplicabilityConstraint applicabilityConstraint,
            Validity validity,
            Clock clock) {
        CompositeComponentVersion initialVersion =
                new CompositeComponentVersion(
                        children,
                        dependencies,
                        applicabilityConstraint,
                        validity,
                        LocalDateTime.now(clock));
        return new CompositeComponent(ComponentId.generate(), name, List.of(initialVersion));
    }

    /** Factory: Create CompositeComponent with initial version (always applicable). */
    public static CompositeComponent withInitialVersion(
            String name,
            List<Component> children,
            Map<ComponentId, Map<String, ParameterValue>> dependencies,
            Validity validity,
            Clock clock) {
        return withInitialVersion(
                name,
                children,
                dependencies,
                ApplicabilityConstraint.alwaysTrue(),
                validity,
                clock);
    }

    /**
     * Factory: Create CompositeComponent with initial version (no dependencies, always applicable).
     */
    public static CompositeComponent withInitialVersion(
            String name, List<Component> children, Validity validity, Clock clock) {
        return withInitialVersion(
                name, children, Map.of(), ApplicabilityConstraint.alwaysTrue(), validity, clock);
    }

    /**
     * Backward compatibility: Create CompositeComponent valid "always" (no dependencies). For
     * testing and simple use cases where temporal versioning is not needed.
     */
    public static CompositeComponent of(String name, List<Component> children) {
        return withInitialVersion(
                name, children, Map.of(), Validity.always(), Clock.systemDefaultZone());
    }

    /**
     * Backward compatibility: Create CompositeComponent valid "always" (varargs). For testing and
     * simple use cases where temporal versioning is not needed.
     */
    public static CompositeComponent of(String name, Component... children) {
        return withInitialVersion(
                name, List.of(children), Map.of(), Validity.always(), Clock.systemDefaultZone());
    }

    public static CompositeComponent of(
            String name,
            Map<String, Map<String, ParameterValue>> nameDependencies,
            Component... children) {
        return of(name, nameDependencies, Arrays.asList(children));
    }

    /**
     * Backward compatibility: Create CompositeComponent with name-based dependencies (varargs).
     * Dependencies use child component names (not IDs). Valid "always" - for testing and simple use
     * cases.
     */
    public static CompositeComponent of(
            String name,
            Map<String, Map<String, ParameterValue>> nameDependencies,
            List<Component> children) {
        // Convert name-based dependencies to id-based for CompositeComponentVersion
        Map<ComponentId, Map<String, ParameterValue>> idDependencies = new HashMap<>();

        for (Map.Entry<String, Map<String, ParameterValue>> entry : nameDependencies.entrySet()) {
            String childName = entry.getKey();
            Component child =
                    children.stream()
                            .filter(c -> c.name().equals(childName))
                            .findFirst()
                            .orElseThrow(
                                    () ->
                                            new IllegalArgumentException(
                                                    "Child component '%s' not found in composite '%s'"
                                                            .formatted(childName, name)));
            idDependencies.put(child.id(), entry.getValue());
        }

        // Create version with converted dependencies
        CompositeComponentVersion version =
                new CompositeComponentVersion(
                        children,
                        idDependencies,
                        Validity.always(),
                        LocalDateTime.now(Clock.systemDefaultZone()));

        return new CompositeComponent(ComponentId.generate(), name, List.of(version));
    }

    /**
     * Add new version to this component (immutable operation). Uses default validation strategy:
     * REJECT_IDENTICAL.
     *
     * @param newVersion version to add
     * @return new CompositeComponent with added version
     * @throws IllegalArgumentException if version with identical validity already exists
     */
    public CompositeComponent updateWith(CompositeComponentVersion newVersion) {
        return updateWith(newVersion, VersionUpdateStrategy.REJECT_IDENTICAL);
    }

    /**
     * Add new version to this component with custom validation strategy (immutable operation).
     *
     * @param newVersion version to add
     * @param strategy validation strategy for version conflicts
     * @return new CompositeComponent with added version
     * @throws IllegalArgumentException if validation fails
     */
    public CompositeComponent updateWith(
            CompositeComponentVersion newVersion, VersionUpdateStrategy strategy) {
        strategy.validate(versions, newVersion.validity());

        List<CompositeComponentVersion> updated = new ArrayList<>(versions);
        updated.add(newVersion);
        return new CompositeComponent(id, name, updated);
    }

    @Override
    public Interpretation interpretation() {
        // CompositeComponent always returns TOTAL - it sums children converted to TOTAL
        return Interpretation.TOTAL;
    }

    @Override
    public Money calculate(Parameters parameters, Interpretation targetInterpretation) {
        PricingContext context = PricingContext.from(parameters);
        CompositeComponentVersion version = versionAt(context.timestamp());

        if (!version.isApplicableFor(context)) {
            return Money.pln(BigDecimal.ZERO);
        }

        if (version.children().isEmpty()) {
            throw new IllegalStateException(
                    "Composite component %s has no children".formatted(name));
        }

        // Build component results map for parameter enrichment
        Map<Component, Money> componentResults = new HashMap<>();
        for (Component child : version.children()) {
            componentResults.put(child, null);
        }

        // Calculate each child with target interpretation and sum
        Money total = null;
        for (Component child : version.children()) {
            Parameters enrichedParams =
                    enrichParameters(child, parameters, componentResults, version.dependencies());

            // Delegate to child - it will handle conversion if needed
            Money childResult = child.calculate(enrichedParams, targetInterpretation);

            componentResults.put(child, childResult);
            total = (total == null) ? childResult : total.add(childResult);
        }

        return total;
    }

    @Override
    public ComponentBreakdown calculateBreakdown(
            Parameters parameters, Interpretation targetInterpretation) {
        PricingContext context = PricingContext.from(parameters);
        CompositeComponentVersion version = versionAt(context.timestamp());

        if (!version.isApplicableFor(context)) {
            return new ComponentBreakdown(name, Money.pln(BigDecimal.ZERO), List.of());
        }

        if (version.children().isEmpty()) {
            throw new IllegalStateException(
                    "Composite component %s has no children".formatted(name));
        }

        // Initialize map with all components mapped to null
        Map<Component, Money> componentResults = new HashMap<>();
        for (Component child : version.children()) {
            componentResults.put(child, null);
        }

        List<ComponentBreakdown> childBreakdowns = new ArrayList<>();

        Money total = null;
        for (Component child : version.children()) {
            Parameters enrichedParams =
                    enrichParameters(child, parameters, componentResults, version.dependencies());

            // Get child breakdown with target interpretation
            ComponentBreakdown childBreakdown =
                    child.calculateBreakdown(enrichedParams, targetInterpretation);

            // Store result in component results map (for ValueOf references)
            componentResults.put(child, childBreakdown.total());
            childBreakdowns.add(childBreakdown);

            // Sum using target interpretation
            total = (total == null) ? childBreakdown.total() : total.add(childBreakdown.total());
        }

        return new ComponentBreakdown(name, total, childBreakdowns);
    }

    /**
     * Find version valid at given point in time. When multiple versions are valid, returns the one
     * with youngest validFrom. If validFrom is identical, uses definedAt as tiebreaker (youngest
     * wins).
     */
    private CompositeComponentVersion versionAt(LocalDateTime time) {
        return versions.stream()
                .filter(v -> v.validity().isValidAt(time))
                .max(
                        Comparator.comparing(
                                        (CompositeComponentVersion v) -> v.validity().validFrom())
                                .thenComparing(CompositeComponentVersion::definedAt))
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "No version of component '%s' (%s) valid at %s"
                                                .formatted(name, id, time)));
    }

    /** Enrich parameters for a child component based on declared dependencies. */
    private Parameters enrichParameters(
            Component child,
            Parameters baseParameters,
            Map<Component, Money> componentResults,
            Map<ComponentId, Map<String, ParameterValue>> dependencies) {
        Map<String, ParameterValue> childDependencies = dependencies.get(child.id());

        if (childDependencies == null || childDependencies.isEmpty()) {
            return baseParameters;
        }

        Parameters enriched = baseParameters;
        for (Map.Entry<String, ParameterValue> entry : childDependencies.entrySet()) {
            String targetParamName = entry.getKey();
            ParameterValue expression = entry.getValue();

            Money value = expression.evaluate(componentResults);
            enriched = enriched.with(targetParamName, value);
        }

        return enriched;
    }
}
