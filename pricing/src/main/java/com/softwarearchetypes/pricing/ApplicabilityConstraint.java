package com.softwarearchetypes.pricing;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Constraint evaluated against a {@link PricingContext} to decide whether a pricing component
 * should contribute to the price calculation.
 *
 * <p>Supports full logical composition:
 *
 * <pre>
 *   and(
 *       equalsTo("customerType", "B2C"),
 *       greaterThan("minutes", 10)
 *   )
 * </pre>
 *
 * <p>When a component's constraint is NOT satisfied, the component returns {@code Money.zero()}.
 *
 * @see SimpleComponentVersion#isApplicableFor(PricingContext)
 */
public sealed interface ApplicabilityConstraint
        permits EqualsConstraint,
                InConstraint,
                GreaterThanConstraint,
                GreaterThanOrEqualConstraint,
                LessThanConstraint,
                LessThanOrEqualConstraint,
                BetweenConstraint,
                AndConstraint,
                OrConstraint,
                NotConstraint,
                AlwaysTrueConstraint {

    boolean isSatisfiedBy(PricingContext context);

    // ---- factory methods ----

    static ApplicabilityConstraint alwaysTrue() {
        return new AlwaysTrueConstraint();
    }

    static ApplicabilityConstraint equalsTo(String parameterName, String expectedValue) {
        return new EqualsConstraint(parameterName, expectedValue);
    }

    static ApplicabilityConstraint in(String parameterName, Set<String> allowedValues) {
        return new InConstraint(parameterName, allowedValues);
    }

    static ApplicabilityConstraint in(String parameterName, String... allowedValues) {
        return new InConstraint(parameterName, Set.of(allowedValues));
    }

    static ApplicabilityConstraint greaterThan(String parameterName, int threshold) {
        return new GreaterThanConstraint(parameterName, BigDecimal.valueOf(threshold));
    }

    static ApplicabilityConstraint greaterThan(String parameterName, BigDecimal threshold) {
        return new GreaterThanConstraint(parameterName, threshold);
    }

    static ApplicabilityConstraint greaterThanOrEqualTo(String parameterName, int threshold) {
        return new GreaterThanOrEqualConstraint(parameterName, BigDecimal.valueOf(threshold));
    }

    static ApplicabilityConstraint greaterThanOrEqualTo(
            String parameterName, BigDecimal threshold) {
        return new GreaterThanOrEqualConstraint(parameterName, threshold);
    }

    static ApplicabilityConstraint lessThan(String parameterName, int threshold) {
        return new LessThanConstraint(parameterName, BigDecimal.valueOf(threshold));
    }

    static ApplicabilityConstraint lessThan(String parameterName, BigDecimal threshold) {
        return new LessThanConstraint(parameterName, threshold);
    }

    static ApplicabilityConstraint lessThanOrEqualTo(String parameterName, int threshold) {
        return new LessThanOrEqualConstraint(parameterName, BigDecimal.valueOf(threshold));
    }

    static ApplicabilityConstraint lessThanOrEqualTo(String parameterName, BigDecimal threshold) {
        return new LessThanOrEqualConstraint(parameterName, threshold);
    }

    static ApplicabilityConstraint between(String parameterName, int min, int max) {
        return new BetweenConstraint(
                parameterName, BigDecimal.valueOf(min), BigDecimal.valueOf(max));
    }

    static ApplicabilityConstraint between(String parameterName, BigDecimal min, BigDecimal max) {
        return new BetweenConstraint(parameterName, min, max);
    }

    static ApplicabilityConstraint and(ApplicabilityConstraint... constraints) {
        return new AndConstraint(Arrays.asList(constraints));
    }

    static ApplicabilityConstraint or(ApplicabilityConstraint... constraints) {
        return new OrConstraint(Arrays.asList(constraints));
    }

    static ApplicabilityConstraint not(ApplicabilityConstraint constraint) {
        return new NotConstraint(constraint);
    }
}

record EqualsConstraint(String parameterName, String expectedValue)
        implements ApplicabilityConstraint {
    @Override
    public boolean isSatisfiedBy(PricingContext context) {
        return context.get(parameterName).map(value -> value.equals(expectedValue)).orElse(false);
    }
}

record InConstraint(String parameterName, Set<String> allowedValues)
        implements ApplicabilityConstraint {
    @Override
    public boolean isSatisfiedBy(PricingContext context) {
        return context.get(parameterName).map(allowedValues::contains).orElse(false);
    }
}

record GreaterThanConstraint(String parameterName, BigDecimal threshold)
        implements ApplicabilityConstraint {
    @Override
    public boolean isSatisfiedBy(PricingContext context) {
        return context.get(parameterName)
                .map(
                        value -> {
                            try {
                                return new BigDecimal(value).compareTo(threshold) > 0;
                            } catch (NumberFormatException e) {
                                return false;
                            }
                        })
                .orElse(false);
    }
}

record GreaterThanOrEqualConstraint(String parameterName, BigDecimal threshold)
        implements ApplicabilityConstraint {
    @Override
    public boolean isSatisfiedBy(PricingContext context) {
        return context.get(parameterName)
                .map(
                        value -> {
                            try {
                                return new BigDecimal(value).compareTo(threshold) >= 0;
                            } catch (NumberFormatException e) {
                                return false;
                            }
                        })
                .orElse(false);
    }
}

record LessThanConstraint(String parameterName, BigDecimal threshold)
        implements ApplicabilityConstraint {
    @Override
    public boolean isSatisfiedBy(PricingContext context) {
        return context.get(parameterName)
                .map(
                        value -> {
                            try {
                                return new BigDecimal(value).compareTo(threshold) < 0;
                            } catch (NumberFormatException e) {
                                return false;
                            }
                        })
                .orElse(false);
    }
}

record LessThanOrEqualConstraint(String parameterName, BigDecimal threshold)
        implements ApplicabilityConstraint {
    @Override
    public boolean isSatisfiedBy(PricingContext context) {
        return context.get(parameterName)
                .map(
                        value -> {
                            try {
                                return new BigDecimal(value).compareTo(threshold) <= 0;
                            } catch (NumberFormatException e) {
                                return false;
                            }
                        })
                .orElse(false);
    }
}

record BetweenConstraint(String parameterName, BigDecimal min, BigDecimal max)
        implements ApplicabilityConstraint {
    @Override
    public boolean isSatisfiedBy(PricingContext context) {
        return context.get(parameterName)
                .map(
                        value -> {
                            try {
                                BigDecimal numValue = new BigDecimal(value);
                                return numValue.compareTo(min) >= 0 && numValue.compareTo(max) <= 0;
                            } catch (NumberFormatException e) {
                                return false;
                            }
                        })
                .orElse(false);
    }
}

record AndConstraint(List<ApplicabilityConstraint> constraints) implements ApplicabilityConstraint {
    @Override
    public boolean isSatisfiedBy(PricingContext context) {
        return constraints.stream().allMatch(c -> c.isSatisfiedBy(context));
    }
}

record OrConstraint(List<ApplicabilityConstraint> constraints) implements ApplicabilityConstraint {
    @Override
    public boolean isSatisfiedBy(PricingContext context) {
        return constraints.stream().anyMatch(c -> c.isSatisfiedBy(context));
    }
}

record NotConstraint(ApplicabilityConstraint constraint) implements ApplicabilityConstraint {
    @Override
    public boolean isSatisfiedBy(PricingContext context) {
        return !constraint.isSatisfiedBy(context);
    }
}

record AlwaysTrueConstraint() implements ApplicabilityConstraint {
    @Override
    public boolean isSatisfiedBy(PricingContext context) {
        return true;
    }
}
