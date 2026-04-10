package com.softwarearchetypes.pricing;

import static com.softwarearchetypes.pricing.CalculatorType.COMPOSITE;
import static java.math.BigDecimal.valueOf;
import static java.time.temporal.ChronoUnit.DAYS;

import com.softwarearchetypes.quantity.money.Money;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

interface Calculator {

    Money calculate(Parameters parameters);

    String describe();

    String formula();

    default Interpretation interpretation() {
        return Interpretation.TOTAL;
    }

    /**
     * Simulates calculation for multiple points in parameter space.
     *
     * @param points list of parameter sets to evaluate
     * @return map from each parameter set to its calculated price
     */
    default Map<Parameters, Money> simulate(List<Parameters> points) {
        Map<Parameters, Money> results = new LinkedHashMap<>();
        for (Parameters params : points) {
            results.put(params, calculate(params));
        }
        return results;
    }

    CalculatorType getType();

    CalculatorId getId();

    String name();
}

record SimpleFixedCalculator(
        CalculatorId id, String name, Money amount, Interpretation interpretation)
        implements Calculator {

    public SimpleFixedCalculator(String name, Money amount) {
        this(CalculatorId.generate(), name, amount, Interpretation.TOTAL);
    }

    public SimpleFixedCalculator(String name, Money amount, Interpretation interpretation) {
        this(CalculatorId.generate(), name, amount, interpretation);
    }

    @Override
    public Money calculate(Parameters parameters) {
        return amount;
    }

    @Override
    public String describe() {
        return getType().formatDescription(amount);
    }

    @Override
    public String formula() {
        return "f(x) = %s".formatted(amount);
    }

    @Override
    public Interpretation interpretation() {
        return interpretation;
    }

    @Override
    public CalculatorType getType() {
        return CalculatorType.SIMPLE_FIXED;
    }

    @Override
    public CalculatorId getId() {
        return id;
    }
}

record SimpleInterestCalculator(CalculatorId id, String name, BigDecimal annualRate)
        implements Calculator {

    private static final int SCALE = 10;

    public SimpleInterestCalculator(String name, BigDecimal annualRate) {
        this(CalculatorId.generate(), name, annualRate);
    }

    @Override
    public Money calculate(Parameters parameters) {
        if (!parameters.containsAll(getType().requiredCalculationFields())) {
            throw new IllegalArgumentException(
                    "SimpleInterestCalculator requiredPreviousKeys %s parameters"
                            .formatted(getType().requiredCalculationFields()));
        }

        Money base = (Money) parameters.get("base");
        ChronoUnit unit = (ChronoUnit) parameters.get("unit");

        BigDecimal rate = annualRate.divide(valueOf(100), SCALE, RoundingMode.HALF_UP);
        BigDecimal unitRate = rate.divide(unitsPerYear(unit), SCALE, RoundingMode.HALF_UP);

        // Preserve currency from base
        return base.multiply(unitRate);
    }

    @Override
    public String describe() {
        return getType().formatDescription(annualRate);
    }

    @Override
    public String formula() {
        return "f(base, unit) = base × (rate/100) × (1/unitsPerYear(unit))\nwhere rate = %s%%"
                .formatted(annualRate);
    }

    @Override
    public CalculatorType getType() {
        return CalculatorType.SIMPLE_INTEREST;
    }

    @Override
    public CalculatorId getId() {
        return id;
    }

    private BigDecimal unitsPerYear(ChronoUnit unit) {
        return switch (unit) {
            case DAYS -> valueOf(365);
            case WEEKS -> valueOf(52);
            case MONTHS -> valueOf(12);
            case YEARS -> valueOf(1);
            default ->
                    throw new IllegalArgumentException(
                            "Unsupported unit for annual calculation: " + unit);
        };
    }
}

/**
 * Step function calculator - increases price by fixed increment every N units. Example: base price
 * 100 PLN, step size 10, step increment 5 PLN - EXCLUSIVE (default): 0-9 units: 100 PLN, 10-19
 * units: 105 PLN - INCLUSIVE: 0-10 units: 100 PLN, 11-20 units: 105 PLN
 */
record StepFunctionCalculator(
        CalculatorId id,
        String name,
        Money basePrice,
        BigDecimal stepSize,
        BigDecimal stepIncrement,
        Interpretation interpretation,
        StepBoundary stepBoundary)
        implements Calculator {

    // Canonical constructor with null handling
    public StepFunctionCalculator {
        // Default to TOTAL if interpretation is null
        if (interpretation == null) {
            interpretation = Interpretation.TOTAL;
        }
        // Default to EXCLUSIVE if stepBoundary is null
        if (stepBoundary == null) {
            stepBoundary = StepBoundary.EXCLUSIVE;
        }
    }

    public StepFunctionCalculator(
            String name, Money basePrice, BigDecimal stepSize, BigDecimal stepIncrement) {
        this(
                CalculatorId.generate(),
                name,
                basePrice,
                stepSize,
                stepIncrement,
                Interpretation.TOTAL,
                null);
    }

    public StepFunctionCalculator(
            String name,
            Money basePrice,
            BigDecimal stepSize,
            BigDecimal stepIncrement,
            Interpretation interpretation) {
        this(
                CalculatorId.generate(),
                name,
                basePrice,
                stepSize,
                stepIncrement,
                interpretation,
                null);
    }

    public StepFunctionCalculator(
            String name,
            Money basePrice,
            BigDecimal stepSize,
            BigDecimal stepIncrement,
            Interpretation interpretation,
            StepBoundary stepBoundary) {
        this(
                CalculatorId.generate(),
                name,
                basePrice,
                stepSize,
                stepIncrement,
                interpretation,
                stepBoundary);
    }

    @Override
    public Money calculate(Parameters parameters) {
        if (!parameters.containsAll(getType().requiredCalculationFields())) {
            throw new IllegalArgumentException(
                    "StepFunctionCalculator requires %s parameters"
                            .formatted(getType().requiredCalculationFields()));
        }

        BigDecimal quantity = parameters.getBigDecimal("quantity");

        // Calculate number of complete steps based on boundary type
        BigDecimal steps;
        if (stepBoundary == StepBoundary.INCLUSIVE && quantity.compareTo(BigDecimal.ZERO) > 0) {
            // For inclusive boundaries: 1-5 → step 0, 6-10 → step 1, etc.
            steps = quantity.subtract(BigDecimal.ONE).divide(stepSize, 0, RoundingMode.DOWN);
        } else {
            // For exclusive boundaries (default): 0-4 → step 0, 5-9 → step 1, etc.
            steps = quantity.divide(stepSize, 0, RoundingMode.DOWN);
        }

        // Calculate total increment value and round to avoid precision issues
        BigDecimal totalIncrementValue =
                stepIncrement
                        .multiply(steps)
                        .setScale(10, RoundingMode.HALF_UP)
                        .stripTrailingZeros();

        // Create increment Money in the same currency as basePrice
        Money incrementTotal = Money.of(totalIncrementValue, basePrice.currency());

        return basePrice.add(incrementTotal);
    }

    @Override
    public String describe() {
        return String.format(
                "Step function calculator - base price %s + increments every %s units",
                basePrice, stepSize);
    }

    @Override
    public String formula() {
        return "f(quantity) = basePrice + ⌊quantity/%s⌋ × %s\nwhere basePrice = %s"
                .formatted(
                        stepSize.stripTrailingZeros().toPlainString(),
                        stepIncrement.stripTrailingZeros().toPlainString(),
                        basePrice);
    }

    @Override
    public Interpretation interpretation() {
        return interpretation;
    }

    @Override
    public CalculatorType getType() {
        return CalculatorType.STEP_FUNCTION;
    }

    @Override
    public CalculatorId getId() {
        return id;
    }
}

/**
 * Discrete points calculator - price lookup from predefined quantity-price pairs. Throws exception
 * if quantity is not in the predefined set. Example: {5 → 100 PLN, 10 → 180 PLN, 20 → 350 PLN} -
 * quantity 5 → returns 100 PLN - quantity 10 → returns 180 PLN - quantity 7 → throws exception (not
 * defined)
 */
record DiscretePointsCalculator(
        CalculatorId id, String name, Map<BigDecimal, Money> points, Interpretation interpretation)
        implements Calculator {

    public DiscretePointsCalculator(String name, Map<BigDecimal, Money> points) {
        this(CalculatorId.generate(), name, new HashMap<>(points), Interpretation.TOTAL);
    }

    public DiscretePointsCalculator(
            String name, Map<BigDecimal, Money> points, Interpretation interpretation) {
        this(CalculatorId.generate(), name, new HashMap<>(points), interpretation);
    }

    @Override
    public Money calculate(Parameters parameters) {
        if (!parameters.containsAll(getType().requiredCalculationFields())) {
            throw new IllegalArgumentException(
                    "DiscretePointsCalculator requires %s parameters"
                            .formatted(getType().requiredCalculationFields()));
        }

        BigDecimal quantity = parameters.getBigDecimal("quantity");

        // Look up the price for the given quantity
        Money price = points.get(quantity);
        if (price == null) {
            throw new IllegalArgumentException(
                    "Quantity %s is not defined in the price points. Available quantities: %s"
                            .formatted(quantity, points.keySet()));
        }

        return price;
    }

    @Override
    public String describe() {
        return String.format(
                "Discrete points calculator with %d price points: %s", points.size(), points);
    }

    @Override
    public String formula() {
        StringBuilder sb = new StringBuilder("f(quantity) = lookup(quantity)\nDefined points:\n");
        points.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(
                        e ->
                                sb.append(
                                        "  quantity = %s → %s\n"
                                                .formatted(
                                                        e.getKey()
                                                                .stripTrailingZeros()
                                                                .toPlainString(),
                                                        e.getValue())));
        return sb.toString().trim();
    }

    @Override
    public Interpretation interpretation() {
        return interpretation;
    }

    @Override
    public CalculatorType getType() {
        return CalculatorType.DISCRETE_POINTS;
    }

    @Override
    public CalculatorId getId() {
        return id;
    }
}

/**
 * Daily increment calculator - price grows by fixed amount each day (discrete). Example: Pre-sale
 * pricing that increases by 100 PLN per day over 14 days - Day 0 (start): 1999 PLN - Day 1: 2099
 * PLN - Day 7: 2699 PLN - Day 14: 3399 PLN NOTE: Operates on full days - same price throughout each
 * day (discrete, not continuous).
 */
record DailyIncrementCalculator(
        CalculatorId id,
        String name,
        LocalDate startDate,
        Money startPrice,
        Money dailyIncrement,
        Interpretation interpretation)
        implements Calculator {

    public DailyIncrementCalculator(
            String name, LocalDate startDate, Money startPrice, Money dailyIncrement) {
        this(
                CalculatorId.generate(),
                name,
                startDate,
                startPrice,
                dailyIncrement,
                Interpretation.TOTAL);
    }

    public DailyIncrementCalculator(
            String name,
            LocalDate startDate,
            Money startPrice,
            Money dailyIncrement,
            Interpretation interpretation) {
        this(CalculatorId.generate(), name, startDate, startPrice, dailyIncrement, interpretation);
    }

    @Override
    public Money calculate(Parameters parameters) {
        if (!parameters.containsAll(getType().requiredCalculationFields())) {
            throw new IllegalArgumentException(
                    "DailyIncrementCalculator requires %s parameters"
                            .formatted(getType().requiredCalculationFields()));
        }

        LocalDate date = (LocalDate) parameters.get("date");

        // Calculate days from start (can be negative if before startDate)
        long daysFromStart = DAYS.between(startDate, date);

        // Discrete calculation: startPrice + (days * dailyIncrement)
        BigDecimal daysDecimal = BigDecimal.valueOf(daysFromStart);
        Money totalIncrement = dailyIncrement.multiply(daysDecimal);

        return startPrice.add(totalIncrement);
    }

    @Override
    public String describe() {
        return String.format(
                "Daily increment calculator - starts at %s on %s, grows by %s per day",
                startPrice, startDate, dailyIncrement);
    }

    @Override
    public String formula() {
        return "f(date) = startPrice + daysFromStart × dailyIncrement\nwhere:\n  startDate = %s\n  startPrice = %s\n  dailyIncrement = %s"
                .formatted(startDate, startPrice, dailyIncrement);
    }

    @Override
    public Interpretation interpretation() {
        return interpretation;
    }

    @Override
    public CalculatorType getType() {
        return CalculatorType.DAILY_INCREMENT;
    }

    @Override
    public CalculatorId getId() {
        return id;
    }
}

/**
 * Continuous linear time calculator - price changes continuously based on precise time. Uses linear
 * interpolation between start and end points. Example: Auction where price increases from 1999 PLN
 * to 3399 PLN over 14 days - Day 0, 00:00 → 1999.00 PLN - Day 0, 12:00 → 2049.00 PLN (interpolated)
 * - Day 7, 00:00 → 2699.00 PLN - Day 7, 06:00 → 2724.00 PLN (interpolated) - Day 14, 00:00 →
 * 3399.00 PLN NOTE: Continuous - price changes every second, not just daily.
 */
record ContinuousLinearTimeCalculator(
        CalculatorId id,
        String name,
        LocalDateTime startTime,
        Money startPrice,
        LocalDateTime endTime,
        Money endPrice,
        Interpretation interpretation)
        implements Calculator {

    public ContinuousLinearTimeCalculator(
            String name,
            LocalDateTime startTime,
            Money startPrice,
            LocalDateTime endTime,
            Money endPrice) {
        this(
                CalculatorId.generate(),
                name,
                startTime,
                startPrice,
                endTime,
                endPrice,
                Interpretation.TOTAL);
    }

    public ContinuousLinearTimeCalculator(
            String name,
            LocalDateTime startTime,
            Money startPrice,
            LocalDateTime endTime,
            Money endPrice,
            Interpretation interpretation) {
        this(
                CalculatorId.generate(),
                name,
                startTime,
                startPrice,
                endTime,
                endPrice,
                interpretation);
    }

    @Override
    public Money calculate(Parameters parameters) {
        if (!parameters.containsAll(getType().requiredCalculationFields())) {
            throw new IllegalArgumentException(
                    "ContinuousLinearTimeCalculator requires %s parameters"
                            .formatted(getType().requiredCalculationFields()));
        }

        LocalDateTime queryTime = parameters.getTime("time");

        // Handle edge cases
        if (queryTime.isBefore(startTime)) {
            throw new IllegalArgumentException(
                    "Query time %s is before start time %s".formatted(queryTime, startTime));
        }
        if (queryTime.isAfter(endTime)) {
            throw new IllegalArgumentException(
                    "Query time %s is after end time %s".formatted(queryTime, endTime));
        }

        // Calculate total duration and elapsed duration in seconds
        long totalSeconds = Duration.between(startTime, endTime).getSeconds();
        long elapsedSeconds = Duration.between(startTime, queryTime).getSeconds();

        // Linear interpolation: progress from 0.0 to 1.0
        BigDecimal progress =
                BigDecimal.valueOf(elapsedSeconds)
                        .divide(BigDecimal.valueOf(totalSeconds), 10, RoundingMode.HALF_UP);

        // Calculate price: startPrice + (progress * priceRange)
        Money priceRange = endPrice.subtract(startPrice);
        Money interpolatedIncrease = priceRange.multiply(progress);

        return startPrice.add(interpolatedIncrease);
    }

    @Override
    public String describe() {
        return String.format(
                "Continuous linear time calculator - from %s to %s between %s and %s",
                startPrice, endPrice, startTime, endTime);
    }

    @Override
    public String formula() {
        return "f(t) = startPrice + progress × (endPrice - startPrice)\nwhere progress = (t - startTime) / (endTime - startTime)\ndomain: t ∈ [%s, %s]"
                .formatted(startTime, endTime);
    }

    @Override
    public Interpretation interpretation() {
        return interpretation;
    }

    @Override
    public CalculatorType getType() {
        return CalculatorType.CONTINUOUS_LINEAR_TIME;
    }

    @Override
    public CalculatorId getId() {
        return id;
    }
}

/**
 * Composite function calculator - delegates to different calculators based on parameter ranges.
 * Supports different parameter types: numeric (quantity, weight), time (LocalTime), date
 * (LocalDate).
 *
 * <p>Examples: - Numeric ranges for quantity: [0, 10) → "small", [10, 50) → "medium", [50, ∞) →
 * "large" - Time ranges for pricing: [8:00, 18:00) → "day-rate", [18:00, 8:00) → "night-rate" -
 * Date ranges for seasons: [2024-06-01, 2024-09-01) → "summer", [2024-09-01, 2024-12-01) → "fall"
 */
record CompositeFunctionCalculator(
        CalculatorId id, String name, Ranges ranges, CalculatorRepository repository)
        implements Calculator {

    public CompositeFunctionCalculator(
            String name, Ranges ranges, CalculatorRepository repository) {
        this(CalculatorId.generate(), name, ranges, repository);
    }

    public CompositeFunctionCalculator {
        // Validate that all component calculators exist and have the same interpretation
        validateUniformInterpretation(ranges, repository);
    }

    private static void validateUniformInterpretation(
            Ranges ranges, CalculatorRepository repository) {
        var calculatorIds =
                ranges.toList().stream()
                        .map(CalculatorRange::calculatorId)
                        .collect(Collectors.toSet());

        if (calculatorIds.isEmpty()) {
            throw new IllegalArgumentException("Composite calculator must have at least one range");
        }

        // Get all component calculators in one query
        var calculators = repository.findByIds(calculatorIds);

        // Verify all were found
        if (calculators.size() != calculatorIds.size()) {
            var foundIds = calculators.stream().map(Calculator::getId).toList();
            var missingIds = calculatorIds.stream().filter(id -> !foundIds.contains(id)).toList();
            throw new IllegalArgumentException(
                    "Calculators not found in repository: %s".formatted(missingIds));
        }

        // Check that all have the same interpretation
        var interpretations =
                calculators.stream().map(Calculator::interpretation).distinct().toList();

        if (interpretations.size() > 1) {
            throw new IllegalArgumentException(
                    "All component calculators in composite must have the same interpretation. Found: %s"
                            .formatted(
                                    calculators.stream()
                                            .map(calc -> calc.name() + ":" + calc.interpretation())
                                            .toList()));
        }
    }

    @Override
    public Interpretation interpretation() {
        // All component calculators have the same interpretation (validated in constructor)
        return ranges.toList().stream()
                .findFirst()
                .map(CalculatorRange::calculatorId)
                .flatMap(repository::findById)
                .map(Calculator::interpretation)
                .orElse(Interpretation.TOTAL); // fallback (should never happen due to validation)
    }

    @Override
    public Money calculate(Parameters parameters) {
        // Ranges knows which parameter to check and finds the matching range
        CalculatorRange matchingRange =
                ranges.findMatching(parameters)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "No matching range found in %s".formatted(ranges)));

        // Find the calculator for this range
        Calculator calculator =
                repository
                        .findById(matchingRange.calculatorId())
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Calculator '%s' not found in repository"
                                                        .formatted(matchingRange.calculatorId())));

        // Delegate calculation to the matched calculator
        return calculator.calculate(parameters);
    }

    @Override
    public String describe() {
        return String.format("Composite function calculator: %s", ranges);
    }

    @Override
    public String formula() {
        StringBuilder sb = new StringBuilder("f(x) = piecewise function:\n");
        ranges.toList()
                .forEach(
                        range -> {
                            Calculator calc =
                                    repository
                                            .findById(range.calculatorId())
                                            .orElseThrow(
                                                    () ->
                                                            new IllegalArgumentException(
                                                                    "Calculator '%s' not found"
                                                                            .formatted(
                                                                                    range
                                                                                            .calculatorId())));
                            sb.append(
                                    "  %s → %s: %s\n"
                                            .formatted(
                                                    range.describe(),
                                                    calc.name(),
                                                    calc.formula().replace("\n", " ")));
                        });
        return sb.toString().trim();
    }

    @Override
    public CalculatorType getType() {
        return COMPOSITE;
    }

    @Override
    public CalculatorId getId() {
        return id;
    }
}

// ============================================================================
// PRICE ADAPTERS - Convert between price interpretations
// ============================================================================

/** Adapter: Unit Price → Total Price Formula: Total = UnitPrice × quantity */
record UnitToTotalAdapter(CalculatorId id, String name, Calculator sourceCalculator)
        implements Calculator {

    public static UnitToTotalAdapter wrap(String name, Calculator sourceCalculator) {
        if (sourceCalculator.interpretation() != Interpretation.UNIT) {
            throw new IllegalArgumentException(
                    "UnitToTotalAdapter requires UNIT calculator, got: "
                            + sourceCalculator.interpretation());
        }
        return new UnitToTotalAdapter(CalculatorId.generate(), name, sourceCalculator);
    }

    @Override
    public CalculatorType getType() {
        return CalculatorType.UNIT_TO_TOTAL_ADAPTER;
    }

    @Override
    public Money calculate(Parameters params) {
        BigDecimal quantity = params.getBigDecimal("quantity");
        Money unitPrice = sourceCalculator.calculate(params);
        return unitPrice.multiply(quantity);
    }

    @Override
    public String formula() {
        return "quantity × (" + sourceCalculator.formula() + ")";
    }

    @Override
    public String describe() {
        return "Unit to Total: " + sourceCalculator.describe();
    }

    @Override
    public Interpretation interpretation() {
        return Interpretation.TOTAL;
    }

    @Override
    public CalculatorId getId() {
        return id;
    }
}

/**
 * Adapter: Unit Price → Marginal Price Formula: Marginal(n) = Total(n) - Total(n-1) where Total(n)
 * = UnitPrice(n) × n NOTE: Works correctly for both constant and variable unit prices
 */
record UnitToMarginalAdapter(CalculatorId id, String name, Calculator sourceCalculator)
        implements Calculator {

    public static UnitToMarginalAdapter wrap(String name, Calculator sourceCalculator) {
        if (sourceCalculator.interpretation() != Interpretation.UNIT) {
            throw new IllegalArgumentException(
                    "UnitToMarginalAdapter requires UNIT calculator, got: "
                            + sourceCalculator.interpretation());
        }
        return new UnitToMarginalAdapter(CalculatorId.generate(), name, sourceCalculator);
    }

    @Override
    public CalculatorType getType() {
        return CalculatorType.UNIT_TO_MARGINAL_ADAPTER;
    }

    @Override
    public Money calculate(Parameters params) {
        BigDecimal quantity = params.getBigDecimal("quantity");

        if (quantity.compareTo(BigDecimal.ONE) < 0) {
            throw new IllegalArgumentException("Marginal price requires quantity >= 1");
        }

        // Total(n) = UnitPrice(n) × n
        Money unitPriceN = sourceCalculator.calculate(params);
        Money totalN = unitPriceN.multiply(quantity);

        if (quantity.equals(BigDecimal.ONE)) {
            // First unit: marginal = total
            return totalN;
        }

        // Total(n-1) = UnitPrice(n-1) × (n-1)
        BigDecimal quantityMinusOne = quantity.subtract(BigDecimal.ONE);
        Parameters paramsN1 = Parameters.of("quantity", quantityMinusOne);
        Money unitPriceN1 = sourceCalculator.calculate(paramsN1);
        Money totalN1 = unitPriceN1.multiply(quantityMinusOne);

        // Marginal(n) = Total(n) - Total(n-1)
        return totalN.subtract(totalN1);
    }

    @Override
    public String formula() {
        return "marginal(n) = (unit(n) × n) - (unit(n-1) × (n-1)) where unit = "
                + sourceCalculator.formula();
    }

    @Override
    public String describe() {
        return "Unit to Marginal (derivative): " + sourceCalculator.describe();
    }

    @Override
    public Interpretation interpretation() {
        return Interpretation.MARGINAL;
    }

    @Override
    public CalculatorId getId() {
        return id;
    }
}

/** Adapter: Total Price → Unit Price (Average) Formula: UnitPrice = Total / quantity */
record TotalToUnitAdapter(CalculatorId id, String name, Calculator sourceCalculator)
        implements Calculator {

    public static TotalToUnitAdapter wrap(String name, Calculator sourceCalculator) {
        if (sourceCalculator.interpretation() != Interpretation.TOTAL) {
            throw new IllegalArgumentException(
                    "TotalToUnitAdapter requires TOTAL calculator, got: "
                            + sourceCalculator.interpretation());
        }
        return new TotalToUnitAdapter(CalculatorId.generate(), name, sourceCalculator);
    }

    @Override
    public CalculatorType getType() {
        return CalculatorType.TOTAL_TO_UNIT_ADAPTER;
    }

    @Override
    public Money calculate(Parameters params) {
        BigDecimal quantity = params.getBigDecimal("quantity");
        Money total = sourceCalculator.calculate(params);
        return total.divide(quantity);
    }

    @Override
    public String formula() {
        return "(" + sourceCalculator.formula() + ") / quantity";
    }

    @Override
    public String describe() {
        return "Total to Unit (average): " + sourceCalculator.describe();
    }

    @Override
    public Interpretation interpretation() {
        return Interpretation.UNIT;
    }

    @Override
    public CalculatorId getId() {
        return id;
    }
}

/**
 * Adapter: Total Price → Marginal Price Formula: Marginal(n) = Total(n) - Total(n-1) NOTE: Requires
 * calling sourceCalculator TWICE (for n and n-1)
 */
record TotalToMarginalAdapter(CalculatorId id, String name, Calculator sourceCalculator)
        implements Calculator {

    public static TotalToMarginalAdapter wrap(String name, Calculator sourceCalculator) {
        if (sourceCalculator.interpretation() != Interpretation.TOTAL) {
            throw new IllegalArgumentException(
                    "TotalToMarginalAdapter requires TOTAL calculator, got: "
                            + sourceCalculator.interpretation());
        }
        return new TotalToMarginalAdapter(CalculatorId.generate(), name, sourceCalculator);
    }

    @Override
    public CalculatorType getType() {
        return CalculatorType.TOTAL_TO_MARGINAL_ADAPTER;
    }

    @Override
    public Money calculate(Parameters params) {
        BigDecimal quantity = params.getBigDecimal("quantity");

        if (quantity.compareTo(BigDecimal.ONE) < 0) {
            throw new IllegalArgumentException("Marginal price requires quantity >= 1");
        }

        // Total for n units
        Money totalN = sourceCalculator.calculate(params);

        if (quantity.equals(BigDecimal.ONE)) {
            // First unit: marginal = total
            return totalN;
        }

        // Total for n-1 units
        BigDecimal quantityMinusOne = quantity.subtract(BigDecimal.ONE);
        Parameters paramsN1 = Parameters.of("quantity", quantityMinusOne);
        Money totalN1 = sourceCalculator.calculate(paramsN1);

        // Marginal(n) = Total(n) - Total(n-1)
        return totalN.subtract(totalN1);
    }

    @Override
    public String formula() {
        return "marginal(n) = total(n) - total(n-1) where total = " + sourceCalculator.formula();
    }

    @Override
    public String describe() {
        return "Total to Marginal (derivative): " + sourceCalculator.describe();
    }

    @Override
    public Interpretation interpretation() {
        return Interpretation.MARGINAL;
    }

    @Override
    public CalculatorId getId() {
        return id;
    }
}

/**
 * Adapter: Marginal Price → Total Price Formula: Total(q) = Σ[i=1→q] Marginal(i) NOTE: Requires q
 * calls to sourceCalculator - can be expensive!
 */
record MarginalToTotalAdapter(CalculatorId id, String name, Calculator sourceCalculator)
        implements Calculator {

    public static MarginalToTotalAdapter wrap(String name, Calculator sourceCalculator) {
        if (sourceCalculator.interpretation() != Interpretation.MARGINAL) {
            throw new IllegalArgumentException(
                    "MarginalToTotalAdapter requires MARGINAL calculator, got: "
                            + sourceCalculator.interpretation());
        }
        return new MarginalToTotalAdapter(CalculatorId.generate(), name, sourceCalculator);
    }

    @Override
    public CalculatorType getType() {
        return CalculatorType.MARGINAL_TO_TOTAL_ADAPTER;
    }

    @Override
    public Money calculate(Parameters params) {
        BigDecimal quantity = params.getBigDecimal("quantity");

        // Calculate first marginal and use it as starting total
        Money total = sourceCalculator.calculate(Parameters.of("quantity", BigDecimal.ONE));

        // Sum remaining marginal prices from 2 to quantity
        for (int i = 2; i <= quantity.intValue(); i++) {
            Parameters marginalParams = Parameters.of("quantity", new BigDecimal(i));
            Money marginal = sourceCalculator.calculate(marginalParams);
            total = total.add(marginal);
        }

        return total;
    }

    @Override
    public String formula() {
        return "Σ[i=1→q] (" + sourceCalculator.formula() + ")";
    }

    @Override
    public String describe() {
        return "Marginal to Total (sum): " + sourceCalculator.describe();
    }

    @Override
    public Interpretation interpretation() {
        return Interpretation.TOTAL;
    }

    @Override
    public CalculatorId getId() {
        return id;
    }
}

/**
 * Adapter: Marginal Price → Unit Price (Average) Formula: UnitPrice(q) = (Σ[i=1→q] Marginal(i)) / q
 * Implementation: Marginal → Total → Unit (two-step conversion)
 */
record MarginalToUnitAdapter(CalculatorId id, String name, Calculator sourceCalculator)
        implements Calculator {

    public static MarginalToUnitAdapter wrap(String name, Calculator sourceCalculator) {
        if (sourceCalculator.interpretation() != Interpretation.MARGINAL) {
            throw new IllegalArgumentException(
                    "MarginalToUnitAdapter requires MARGINAL calculator, got: "
                            + sourceCalculator.interpretation());
        }
        return new MarginalToUnitAdapter(CalculatorId.generate(), name, sourceCalculator);
    }

    @Override
    public CalculatorType getType() {
        return CalculatorType.MARGINAL_TO_UNIT_ADAPTER;
    }

    @Override
    public Money calculate(Parameters params) {
        BigDecimal quantity = params.getBigDecimal("quantity");

        // Step 1: Marginal → Total (sum) - start with first marginal
        Money total = sourceCalculator.calculate(Parameters.of("quantity", BigDecimal.ONE));

        // Sum remaining marginal prices from 2 to quantity
        for (int i = 2; i <= quantity.intValue(); i++) {
            Parameters marginalParams = Parameters.of("quantity", new BigDecimal(i));
            Money marginal = sourceCalculator.calculate(marginalParams);
            total = total.add(marginal);
        }

        // Step 2: Total → Unit (divide)
        return total.divide(quantity);
    }

    @Override
    public String formula() {
        return "(Σ[i=1→q] (" + sourceCalculator.formula() + ")) / quantity";
    }

    @Override
    public String describe() {
        return "Marginal to Unit (average): " + sourceCalculator.describe();
    }

    @Override
    public Interpretation interpretation() {
        return Interpretation.UNIT;
    }

    @Override
    public CalculatorId getId() {
        return id;
    }
}

/**
 * Percentage Calculator - calculates percentage of a base amount. Requires "baseAmount" parameter
 * (Money) and applies the configured percentage rate.
 *
 * <p>Example: PercentageCalculator(10%) with baseAmount=100 PLN → 10 PLN
 */
record PercentageCalculator(CalculatorId id, String name, BigDecimal percentageRate)
        implements Calculator {

    public PercentageCalculator(String name, BigDecimal percentageRate) {
        this(CalculatorId.generate(), name, percentageRate);
    }

    @Override
    public CalculatorType getType() {
        return CalculatorType.PERCENTAGE;
    }

    @Override
    public Money calculate(Parameters params) {
        Money baseAmount = params.getMoney("baseAmount");
        BigDecimal rate = percentageRate.divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP);
        Money result = baseAmount.multiply(rate);
        // Round to 2 decimal places for currency
        return Money.of(result.value().setScale(2, RoundingMode.HALF_UP), result.currency());
    }

    @Override
    public String formula() {
        return "baseAmount × " + percentageRate + "%";
    }

    @Override
    public String describe() {
        return "Percentage: " + percentageRate + "% of base amount";
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public CalculatorId getId() {
        return id;
    }
}
