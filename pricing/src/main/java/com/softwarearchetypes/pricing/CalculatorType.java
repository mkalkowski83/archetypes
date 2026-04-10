package com.softwarearchetypes.pricing;

import java.util.Set;

public enum CalculatorType {
    SIMPLE_FIXED(
            "simple-fixed",
            "Fixed amount calculator - returns %s regardless",
            Set.of("amount"),
            Set.of()),
    SIMPLE_INTEREST(
            "simple-interest",
            "Annual interest calculator - calculates %s%% annual interest based on base and time unit",
            Set.of("annualRate"),
            Set.of("base", "unit")),
    STEP_FUNCTION(
            "step-function",
            "Step function calculator - base price %s PLN + increments every %s units",
            Set.of("basePrice", "stepSize", "stepIncrement"),
            Set.of("quantity")),
    DISCRETE_POINTS(
            "discrete-points",
            "Discrete points calculator - price lookup from predefined points",
            Set.of("points"),
            Set.of("quantity")),
    DAILY_INCREMENT(
            "daily-increment",
            "Daily increment calculator - starts at %s, grows by %s per day (discrete)",
            Set.of("startDate", "startPrice", "dailyIncrement"),
            Set.of("date")),
    CONTINUOUS_LINEAR_TIME(
            "continuous-linear-time",
            "Continuous linear time calculator - interpolates between %s and %s",
            Set.of("startTime", "startPrice", "endTime", "endPrice"),
            Set.of("time")),
    COMPOSITE(
            "composite",
            "Composite function calculator - delegates to different calculators based on parameter ranges",
            Set.of("ranges", "rangeSelector"),
            Set.of()),
    PERCENTAGE(
            "percentage",
            "Percentage calculator - calculates %s%% of base amount",
            Set.of("percentageRate"),
            Set.of("baseAmount")),

    // Price Adapters - convert between price interpretations
    UNIT_TO_TOTAL_ADAPTER(
            "unit-to-total-adapter",
            "Converts unit price to total: total = quantity × unitPrice",
            Set.of(),
            Set.of("quantity")),
    UNIT_TO_MARGINAL_ADAPTER(
            "unit-to-marginal-adapter",
            "Converts unit price to marginal: for constant unit price, marginal = unitPrice",
            Set.of(),
            Set.of("quantity")),
    TOTAL_TO_UNIT_ADAPTER(
            "total-to-unit-adapter",
            "Converts total price to unit price: unitPrice = total / quantity",
            Set.of(),
            Set.of("quantity")),
    TOTAL_TO_MARGINAL_ADAPTER(
            "total-to-marginal-adapter",
            "Converts total to marginal: marginal(n) = total(n) - total(n-1)",
            Set.of(),
            Set.of("quantity")),
    MARGINAL_TO_TOTAL_ADAPTER(
            "marginal-to-total-adapter",
            "Converts marginal to total: total = Σ marginal(i) for i=1..quantity",
            Set.of(),
            Set.of("quantity")),
    MARGINAL_TO_UNIT_ADAPTER(
            "marginal-to-unit-adapter",
            "Converts marginal to unit price: unitPrice = Σ marginal(i) / quantity",
            Set.of(),
            Set.of("quantity"));

    private final String typeName;
    private final String descriptionTemplate;
    private final Set<String> requiredCreationFields;
    private final Set<String> requiredCalculationFields;

    CalculatorType(
            String typeName,
            String descriptionTemplate,
            Set<String> requiredCreationFields,
            Set<String> requiredCalculationFields) {
        this.typeName = typeName;
        this.descriptionTemplate = descriptionTemplate;
        this.requiredCreationFields = requiredCreationFields;
        this.requiredCalculationFields = requiredCalculationFields;
    }

    public String getTypeName() {
        return typeName;
    }

    public String formatDescription(Object value) {
        return String.format(descriptionTemplate, value);
    }

    public Set<String> requiredCreationFields() {
        return requiredCreationFields;
    }

    Set<String> requiredCalculationFields() {
        return requiredCalculationFields;
    }
}
