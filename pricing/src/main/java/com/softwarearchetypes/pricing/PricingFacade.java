package com.softwarearchetypes.pricing;

import static java.time.LocalDateTime.now;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;

import com.softwarearchetypes.quantity.money.Money;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PricingFacade {

    private final CalculatorRepository calculatorRepository;
    private final ComponentRepository componentRepository;
    private final Clock clock;

    PricingFacade(
            CalculatorRepository calculatorRepository,
            ComponentRepository componentRepository,
            Clock clock) {
        this.calculatorRepository = calculatorRepository;
        this.componentRepository = componentRepository;
        this.clock = clock;
    }

    // Backward compatibility constructor
    PricingFacade(CalculatorRepository calculatorRepository, Clock clock) {
        this(calculatorRepository, new InMemoryComponentRepository(), clock);
    }

    public List<CalculatorView> availableCalculators() {
        return calculatorRepository.findAll().stream().map(CalculatorView::from).toList();
    }

    public Calculator addCalculator(String name, CalculatorType type, Parameters parameters) {
        Calculator calculator = createCalculator(name, type, parameters);
        calculatorRepository.save(calculator);
        return calculator;
    }

    public Money calculate(String calculatorName, Parameters parameters) {
        return calculatorRepository
                .findByName(calculatorName)
                .map(c -> c.calculate(parameters))
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "could not find calculator %s".formatted(calculatorName)));
    }

    /**
     * Calculate total price - automatically wraps calculator if it doesn't return TOTAL. This is a
     * convenience method that handles interpretation conversion transparently.
     */
    public Money calculateTotal(String calculatorName, Parameters parameters) {
        Calculator calc =
                calculatorRepository
                        .findByName(calculatorName)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "could not find calculator %s"
                                                        .formatted(calculatorName)));

        // Auto-wrap if calculator doesn't return TOTAL
        Calculator totalCalc =
                switch (calc.interpretation()) {
                    case TOTAL -> calc; // already returns total
                    case UNIT -> UnitToTotalAdapter.wrap(calc.name() + "-to-total", calc);
                    case MARGINAL -> MarginalToTotalAdapter.wrap(calc.name() + "-to-total", calc);
                };

        return totalCalc.calculate(parameters);
    }

    /**
     * Calculate unit price (average) - automatically wraps calculator if it doesn't return UNIT.
     * This is a convenience method that handles interpretation conversion transparently.
     */
    public Money calculateUnitPrice(String calculatorName, Parameters parameters) {
        Calculator calc =
                calculatorRepository
                        .findByName(calculatorName)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "could not find calculator %s"
                                                        .formatted(calculatorName)));

        // Auto-wrap if calculator doesn't return UNIT
        Calculator unitCalc =
                switch (calc.interpretation()) {
                    case UNIT -> calc; // already returns unit price
                    case TOTAL -> TotalToUnitAdapter.wrap(calc.name() + "-to-unit", calc);
                    case MARGINAL -> MarginalToUnitAdapter.wrap(calc.name() + "-to-unit", calc);
                };

        return unitCalc.calculate(parameters);
    }

    /**
     * Calculate marginal price - automatically wraps calculator if it doesn't return MARGINAL. This
     * is a convenience method that handles interpretation conversion transparently.
     *
     * <p>NOTE: Not all conversions to MARGINAL are possible! - MARGINAL → MARGINAL: OK (identity) -
     * UNIT → MARGINAL: OK (for constant unit price) - TOTAL → MARGINAL: OK (derivative: marginal(n)
     * = total(n) - total(n-1))
     */
    public Money calculateMarginal(String calculatorName, Parameters parameters) {
        Calculator calc =
                calculatorRepository
                        .findByName(calculatorName)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "could not find calculator %s"
                                                        .formatted(calculatorName)));

        // Auto-wrap if calculator doesn't return MARGINAL
        Calculator marginalCalc =
                switch (calc.interpretation()) {
                    case MARGINAL -> calc; // already returns marginal
                    case UNIT -> UnitToMarginalAdapter.wrap(calc.name() + "-to-marginal", calc);
                    case TOTAL -> TotalToMarginalAdapter.wrap(calc.name() + "-to-marginal", calc);
                };

        return marginalCalc.calculate(parameters);
    }

    public Map<CalculatorType, List<CalculatorView>> listCalculatorsWithDescriptions() {
        return calculatorRepository.findAll().stream()
                .collect(
                        groupingBy(
                                Calculator::getType,
                                mapping(CalculatorView::from, Collectors.toList())));
    }

    public List<CalculatorType> availableCalculatorTypes() {
        return Arrays.asList(CalculatorType.values());
    }

    public SimpleComponent createSimpleComponent(String componentName, String calculatorName) {
        return createSimpleComponent(
                componentName,
                calculatorName,
                Map.of(),
                ApplicabilityConstraint.alwaysTrue(),
                Validity.from(now(clock)));
    }

    public SimpleComponent createSimpleComponent(
            String componentName, String calculatorName, Map<String, String> parameterMappings) {
        return createSimpleComponent(
                componentName,
                calculatorName,
                parameterMappings,
                ApplicabilityConstraint.alwaysTrue(),
                Validity.from(now(clock)));
    }

    public SimpleComponent createSimpleComponent(
            String componentName,
            String calculatorName,
            Map<String, String> parameterMappings,
            Validity validity) {
        return createSimpleComponent(
                componentName,
                calculatorName,
                parameterMappings,
                ApplicabilityConstraint.alwaysTrue(),
                validity);
    }

    public SimpleComponent createSimpleComponent(
            String componentName,
            String calculatorName,
            ApplicabilityConstraint applicabilityConstraint) {
        return createSimpleComponent(
                componentName,
                calculatorName,
                Map.of(),
                applicabilityConstraint,
                Validity.from(now(clock)));
    }

    public SimpleComponent createSimpleComponent(
            String componentName,
            String calculatorName,
            Map<String, String> parameterMappings,
            ApplicabilityConstraint applicabilityConstraint) {
        return createSimpleComponent(
                componentName,
                calculatorName,
                parameterMappings,
                applicabilityConstraint,
                Validity.from(now(clock)));
    }

    public SimpleComponent createSimpleComponent(
            String componentName,
            String calculatorName,
            Map<String, String> parameterMappings,
            ApplicabilityConstraint applicabilityConstraint,
            Validity validity) {
        Calculator calculator =
                calculatorRepository
                        .findByName(calculatorName)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Calculator '%s' not found"
                                                        .formatted(calculatorName)));

        return componentRepository
                .findByName(componentName)
                .map(
                        existing -> {
                            if (!(existing instanceof SimpleComponent simpleComponent)) {
                                throw new IllegalArgumentException(
                                        "Component '%s' exists but is not a SimpleComponent"
                                                .formatted(componentName));
                            }
                            SimpleComponentVersion newVersion =
                                    new SimpleComponentVersion(
                                            calculator,
                                            parameterMappings,
                                            applicabilityConstraint,
                                            validity,
                                            now(clock));
                            SimpleComponent updated = simpleComponent.updateWith(newVersion);
                            componentRepository.save(updated);
                            return updated;
                        })
                .orElseGet(
                        () -> {
                            SimpleComponent component =
                                    SimpleComponent.withInitialVersion(
                                            componentName,
                                            calculator,
                                            parameterMappings,
                                            applicabilityConstraint,
                                            validity,
                                            clock);
                            componentRepository.save(component);
                            return component;
                        });
    }

    public CompositeComponent createCompositeComponent(
            String compositeName, String... childComponentNames) {
        return createCompositeComponent(compositeName, Map.of(), childComponentNames);
    }

    public CompositeComponent createCompositeComponent(
            String compositeName,
            Map<String, Map<String, ParameterValue>> dependencies,
            String... childComponentNames) {
        return createCompositeComponent(
                compositeName,
                dependencies,
                ApplicabilityConstraint.alwaysTrue(),
                Validity.from(now(clock)),
                childComponentNames);
    }

    public CompositeComponent createCompositeComponent(
            String compositeName,
            Map<String, Map<String, ParameterValue>> dependencies,
            Validity validity,
            String... childComponentNames) {
        return createCompositeComponent(
                compositeName,
                dependencies,
                ApplicabilityConstraint.alwaysTrue(),
                validity,
                childComponentNames);
    }

    public CompositeComponent createCompositeComponent(
            String compositeName,
            Map<String, Map<String, ParameterValue>> dependencies,
            ApplicabilityConstraint applicabilityConstraint,
            String... childComponentNames) {
        return createCompositeComponent(
                compositeName,
                dependencies,
                applicabilityConstraint,
                Validity.from(now(clock)),
                childComponentNames);
    }

    public CompositeComponent createCompositeComponent(
            String compositeName,
            Map<String, Map<String, ParameterValue>> dependencies,
            ApplicabilityConstraint applicabilityConstraint,
            Validity validity,
            String... childComponentNames) {
        // Preserve order by looking up components one by one
        List<Component> children = new java.util.ArrayList<>();
        for (String childName : childComponentNames) {
            Component child =
                    componentRepository
                            .findByName(childName)
                            .orElseThrow(
                                    () ->
                                            new IllegalArgumentException(
                                                    "Component '%s' not found"
                                                            .formatted(childName)));
            children.add(child);
        }

        // Convert name-based dependencies to id-based
        Map<ComponentId, Map<String, ParameterValue>> idBasedDeps = new java.util.HashMap<>();
        for (Map.Entry<String, Map<String, ParameterValue>> entry : dependencies.entrySet()) {
            String childName = entry.getKey();
            Component child =
                    children.stream()
                            .filter(c -> c.name().equals(childName))
                            .findFirst()
                            .orElseThrow(
                                    () ->
                                            new IllegalArgumentException(
                                                    "Child component '%s' not found in children list"
                                                            .formatted(childName)));
            idBasedDeps.put(child.id(), entry.getValue());
        }

        // Sprawdź czy komponent już istnieje
        return componentRepository
                .findByName(compositeName)
                .map(
                        existing -> {
                            // Istnieje - dodaj nową wersję
                            if (!(existing instanceof CompositeComponent compositeComponent)) {
                                throw new IllegalArgumentException(
                                        "Component '%s' exists but is not a CompositeComponent"
                                                .formatted(compositeName));
                            }
                            CompositeComponentVersion newVersion =
                                    new CompositeComponentVersion(
                                            children,
                                            idBasedDeps,
                                            applicabilityConstraint,
                                            validity,
                                            now(clock));
                            CompositeComponent updated = compositeComponent.updateWith(newVersion);
                            componentRepository.save(updated);
                            return updated;
                        })
                .orElseGet(
                        () -> {
                            // Nie istnieje - utwórz nowy
                            CompositeComponent component =
                                    CompositeComponent.withInitialVersion(
                                            compositeName,
                                            children,
                                            idBasedDeps,
                                            applicabilityConstraint,
                                            validity,
                                            clock);
                            componentRepository.save(component);
                            return component;
                        });
    }

    public Money calculateComponent(String componentName, Parameters parameters) {
        Component component =
                componentRepository
                        .findByName(componentName)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Component '%s' not found"
                                                        .formatted(componentName)));

        return component.calculate(parameters);
    }

    public ComponentBreakdown calculateComponentBreakdown(
            String componentName, Parameters parameters) {
        Component component =
                componentRepository
                        .findByName(componentName)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Component '%s' not found"
                                                        .formatted(componentName)));

        return component.calculateBreakdown(parameters);
    }

    private Calculator createCalculator(String name, CalculatorType type, Parameters parameters) {
        if (!parameters.containsAll(type.requiredCreationFields())) {
            throw new IllegalArgumentException(
                    "Calculator %s requiredPreviousKeys field %s, but passed only %s"
                            .formatted(type, type.requiredCreationFields(), parameters.keys()));
        }

        // Optional interpretation parameter
        Interpretation interpretation =
                parameters.contains("interpretation")
                        ? (Interpretation) parameters.get("interpretation")
                        : null;

        return switch (type) {
            case SIMPLE_FIXED ->
                    interpretation != null
                            ? new SimpleFixedCalculator(
                                    name, parameters.getMoney("amount"), interpretation)
                            : new SimpleFixedCalculator(name, parameters.getMoney("amount"));
            case SIMPLE_INTEREST ->
                    new SimpleInterestCalculator(name, parameters.getBigDecimal("annualRate"));
            case STEP_FUNCTION -> {
                StepBoundary stepBoundary = (StepBoundary) parameters.get("stepBoundary");
                yield new StepFunctionCalculator(
                        name,
                        parameters.getMoney("basePrice"),
                        parameters.getBigDecimal("stepSize"),
                        parameters.getBigDecimal("stepIncrement"),
                        interpretation,
                        stepBoundary);
            }
            case DISCRETE_POINTS -> {
                Map<BigDecimal, Money> points = (Map<BigDecimal, Money>) parameters.get("points");
                yield interpretation != null
                        ? new DiscretePointsCalculator(name, points, interpretation)
                        : new DiscretePointsCalculator(name, points);
            }
            case DAILY_INCREMENT ->
                    interpretation != null
                            ? new DailyIncrementCalculator(
                                    name,
                                    parameters.getLocalDate("startDate"),
                                    parameters.getMoney("startPrice"),
                                    parameters.getMoney("dailyIncrement"),
                                    interpretation)
                            : new DailyIncrementCalculator(
                                    name,
                                    parameters.getLocalDate("startDate"),
                                    parameters.getMoney("startPrice"),
                                    parameters.getMoney("dailyIncrement"));
            case CONTINUOUS_LINEAR_TIME ->
                    interpretation != null
                            ? new ContinuousLinearTimeCalculator(
                                    name,
                                    parameters.getTime("startTime"),
                                    parameters.getMoney("startPrice"),
                                    parameters.getTime("endTime"),
                                    parameters.getMoney("endPrice"),
                                    interpretation)
                            : new ContinuousLinearTimeCalculator(
                                    name,
                                    parameters.getTime("startTime"),
                                    parameters.getMoney("startPrice"),
                                    parameters.getTime("endTime"),
                                    parameters.getMoney("endPrice"));
            case COMPOSITE -> {
                @SuppressWarnings("unchecked")
                List<CalculatorRange> rangesList = (List<CalculatorRange>) parameters.get("ranges");
                String rangeSelector = (String) parameters.get("rangeSelector");

                if (rangeSelector == null) {
                    throw new IllegalArgumentException(
                            "COMPOSITE calculator requires 'rangeSelector' parameter");
                }

                Ranges ranges = new Ranges(rangeSelector, rangesList);
                yield new CompositeFunctionCalculator(name, ranges, calculatorRepository);
            }
            case PERCENTAGE -> {
                BigDecimal percentageRate = (BigDecimal) parameters.get("percentageRate");
                yield new PercentageCalculator(name, percentageRate);
            }
            default ->
                    throw new IllegalArgumentException(
                            "Calculator type %s cannot be created directly".formatted(type));
        };
    }
}
