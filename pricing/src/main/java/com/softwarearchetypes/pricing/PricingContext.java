package com.softwarearchetypes.pricing;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Unified context for pricing decisions — wraps both the time dimension (validity) and the business
 * dimension (applicability constraints).
 *
 * <p>Used in {@link SimpleComponentVersion#isApplicableFor(PricingContext)} to determine whether a
 * component version should fire in a given situation:
 *
 * <pre>
 *   return validity.isValidAt(context.timestamp())
 *          && applicability.isSatisfiedBy(context);
 * </pre>
 *
 * <p>Attributes are stored as Strings so that all constraint implementations can work uniformly
 * regardless of the original parameter type. Numeric parameters (BigDecimal) are converted via
 * {@code toPlainString()}.
 */
public class PricingContext {

    private final Map<String, String> attributes;
    private final LocalDateTime timestamp;

    private PricingContext(Map<String, String> attributes, LocalDateTime timestamp) {
        this.attributes = Map.copyOf(attributes);
        this.timestamp = timestamp;
    }

    /**
     * Build a PricingContext from calculation Parameters.
     *
     * <p>String and numeric (BigDecimal / Number) values are included as-is. The timestamp is taken
     * from the "timestamp" parameter if present, falling back to {@code LocalDateTime.now()}.
     */
    public static PricingContext from(Parameters parameters) {
        LocalDateTime timestamp = parameters.timestamp().orElseGet(LocalDateTime::now);

        Map<String, String> attributes = new HashMap<>();
        for (String key : parameters.keys()) {
            Object value = parameters.get(key);
            if (value instanceof String s) {
                attributes.put(key, s);
            } else if (value instanceof BigDecimal bd) {
                attributes.put(key, bd.toPlainString());
            } else if (value instanceof Number n) {
                attributes.put(key, n.toString());
            }
            // Money, Interpretation, LocalDateTime etc. are intentionally omitted —
            // they are not meaningful for string-based constraint evaluation.
        }

        return new PricingContext(attributes, timestamp);
    }

    /**
     * Returns the point in time at which this context was evaluated. Used by {@link
     * Validity#isValidAt(LocalDateTime)}.
     */
    public LocalDateTime timestamp() {
        return timestamp;
    }

    /**
     * Returns an attribute value for constraint evaluation. Returns {@code Optional.empty()} if the
     * key is absent, allowing constraints to safely return {@code false}.
     */
    public Optional<String> get(String key) {
        return Optional.ofNullable(attributes.get(key));
    }

    public String getOrDefault(String key, String defaultValue) {
        return attributes.getOrDefault(key, defaultValue);
    }

    public boolean has(String key) {
        return attributes.containsKey(key);
    }

    @Override
    public String toString() {
        return "PricingContext{timestamp=" + timestamp + ", attributes=" + attributes + "}";
    }
}
