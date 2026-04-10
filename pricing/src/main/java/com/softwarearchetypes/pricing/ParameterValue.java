package com.softwarearchetypes.pricing;

import com.softwarearchetypes.quantity.money.Money;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Represents an expression for computing a parameter value from component results. Allows algebraic
 * operations on component outputs.
 *
 * <p>Usage example: Map.of("baseAmount", SumOf("basePrice", "shipping"))
 */
public sealed interface ParameterValue permits ValueOf, SumOf, DifferenceOf, ProductOf {

    /**
     * Evaluate this expression given calculated component results.
     *
     * @param componentResults map of Components to their calculated Money values
     * @return computed Money value
     */
    Money evaluate(Map<Component, Money> componentResults);
}

/** Reference to a single component's value. Example: ValueOf("basePrice") */
record ValueOf(String componentName) implements ParameterValue {

    @Override
    public Money evaluate(Map<Component, Money> componentResults) {
        Component component =
                componentResults.keySet().stream()
                        .filter(c -> c.name().equals(componentName))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Component '%s' not found"
                                                        .formatted(componentName)));

        Money value = componentResults.get(component);
        if (value == null) {
            throw new IllegalStateException(
                    "Component '%s' has not been calculated yet. Check execution order."
                            .formatted(componentName));
        }

        return value;
    }
}

/** Sum of multiple component values. Example: SumOf("basePrice", "shipping", "handling") */
record SumOf(String... componentNames) implements ParameterValue {

    @Override
    public Money evaluate(Map<Component, Money> componentResults) {
        if (componentNames.length == 0) {
            throw new IllegalArgumentException("SumOf requires at least one component name");
        }

        Money sum = null;
        for (String name : componentNames) {
            Component component =
                    componentResults.keySet().stream()
                            .filter(c -> c.name().equals(name))
                            .findFirst()
                            .orElseThrow(
                                    () ->
                                            new IllegalArgumentException(
                                                    "Component '%s' not found".formatted(name)));

            Money value = componentResults.get(component);
            if (value == null) {
                throw new IllegalStateException(
                        "Component '%s' has not been calculated yet. Check execution order."
                                .formatted(name));
            }

            sum = (sum == null) ? value : sum.add(value);
        }

        return sum;
    }
}

/** Difference between two component values. Example: DifferenceOf("revenue", "costs") */
record DifferenceOf(String minuendComponent, String subtrahendComponent) implements ParameterValue {

    @Override
    public Money evaluate(Map<Component, Money> componentResults) {
        Component minuend =
                componentResults.keySet().stream()
                        .filter(c -> c.name().equals(minuendComponent))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Component '%s' not found"
                                                        .formatted(minuendComponent)));

        Component subtrahend =
                componentResults.keySet().stream()
                        .filter(c -> c.name().equals(subtrahendComponent))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Component '%s' not found"
                                                        .formatted(subtrahendComponent)));

        Money minuendValue = componentResults.get(minuend);
        if (minuendValue == null) {
            throw new IllegalStateException(
                    "Component '%s' has not been calculated yet".formatted(minuendComponent));
        }

        Money subtrahendValue = componentResults.get(subtrahend);
        if (subtrahendValue == null) {
            throw new IllegalStateException(
                    "Component '%s' has not been calculated yet".formatted(subtrahendComponent));
        }

        return minuendValue.subtract(subtrahendValue);
    }
}

/**
 * Product of component value and a numeric factor. Example: ProductOf("basePrice",
 * BigDecimal.valueOf(1.5))
 */
record ProductOf(String componentName, BigDecimal factor) implements ParameterValue {

    @Override
    public Money evaluate(Map<Component, Money> componentResults) {
        Component component =
                componentResults.keySet().stream()
                        .filter(c -> c.name().equals(componentName))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Component '%s' not found"
                                                        .formatted(componentName)));

        Money value = componentResults.get(component);
        if (value == null) {
            throw new IllegalStateException(
                    "Component '%s' has not been calculated yet".formatted(componentName));
        }

        return value.multiply(factor);
    }
}
