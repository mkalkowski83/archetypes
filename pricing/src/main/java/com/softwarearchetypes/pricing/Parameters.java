package com.softwarearchetypes.pricing;

import com.softwarearchetypes.quantity.money.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Parameters {
    private final Map<String, Object> values;

    public Parameters() {
        this.values = new HashMap<>();
    }

    public Parameters(Map<String, Object> values) {
        this.values = new HashMap<>(values);
    }

    public static Parameters empty() {
        return new Parameters();
    }

    public static Parameters of(String key, Object value) {
        return new Parameters(Map.of(key, value));
    }

    public static Parameters of(String k1, Object v1, String k2, Object v2) {
        return new Parameters(Map.of(k1, v1, k2, v2));
    }

    public static Parameters of(String k1, Object v1, String k2, Object v2, String k3, Object v3) {
        return new Parameters(Map.of(k1, v1, k2, v2, k3, v3));
    }

    public static Parameters of(
            String k1,
            Object v1,
            String k2,
            Object v2,
            String k3,
            Object v3,
            String k4,
            Object v4) {
        return new Parameters(Map.of(k1, v1, k2, v2, k3, v3, k4, v4));
    }

    public static Parameters of(
            String k1,
            Object v1,
            String k2,
            Object v2,
            String k3,
            Object v3,
            String k4,
            Object v4,
            String k5,
            Object v5) {
        return new Parameters(Map.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5));
    }

    public BigDecimal getBigDecimal(String key) {
        Object value = values.get(key);
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return new BigDecimal(value.toString());
        }
        if (value instanceof String) {
            return new BigDecimal((String) value);
        }
        throw new IllegalArgumentException("Cannot convert " + value + " to BigDecimal");
    }

    public Money getMoney(String key) {
        Object value = values.get(key);
        if (value instanceof Money) {
            return (Money) value;
        }
        if (value instanceof String) {
            // Parse format: "PLN 1999.00" or "EUR 199.50"
            String str = (String) value;
            String[] parts = str.trim().split("\\s+");
            if (parts.length != 2) {
                throw new IllegalArgumentException(
                        "Invalid Money format: " + value + ". Expected format: 'PLN 1999.00'");
            }
            String currency = parts[0].toUpperCase();
            BigDecimal amount = new BigDecimal(parts[1]);
            return Money.of(amount, currency);
        }
        throw new IllegalArgumentException("Cannot convert " + value + " to Money");
    }

    LocalDate getLocalDate(String key) {
        Object value = values.get(key);
        if (value instanceof LocalDate) {
            return (LocalDate) value;
        }
        if (value instanceof String) {
            // Parse ISO format: "2024-06-01"
            return LocalDate.parse((String) value);
        }
        throw new IllegalArgumentException("Cannot convert " + value + " to LocalDate");
    }

    LocalDateTime getTime(String key) {
        Object value = values.get(key);
        if (value instanceof LocalDateTime) {
            return (LocalDateTime) value;
        }
        if (value instanceof String) {
            // Parse ISO format: "2024-06-01T10:15:30"
            return LocalDateTime.parse((String) value);
        }
        throw new IllegalArgumentException("Cannot convert " + value + " to LocalDateTime");
    }

    /**
     * Returns the timestamp parameter for versioned component calculations. Used to determine which
     * version of a component to use.
     *
     * @return Optional containing timestamp if present, empty otherwise
     */
    public java.util.Optional<LocalDateTime> timestamp() {
        if (contains("timestamp")) {
            return java.util.Optional.of(getTime("timestamp"));
        }
        return java.util.Optional.empty();
    }

    /**
     * Returns the timestamp parameter, or throws if not present. Use for strict validation when
     * timestamp is mandatory.
     *
     * @return timestamp for version lookup
     * @throws IllegalArgumentException if timestamp not present
     */
    public LocalDateTime requireTimestamp() {
        return timestamp()
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "Parameters must contain 'timestamp' for versioned calculations"));
    }

    public boolean contains(String key) {
        return values.containsKey(key);
    }

    public boolean containsAll(Set<String> keys) {
        return values.keySet().containsAll(keys);
    }

    public Object get(String key) {
        return values.get(key);
    }

    @Override
    public String toString() {
        return "Parameters" + values;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Parameters that = (Parameters) o;
        return values.equals(that.values);
    }

    @Override
    public int hashCode() {
        return values.hashCode();
    }

    public Set<String> keys() {
        return values.keySet();
    }

    public Map<String, Object> getValues() {
        return values;
    }

    public void setValues(Map<String, Object> values) {
        this.values.clear();
        this.values.putAll(values);
    }

    /**
     * Returns a new Parameters instance with an additional key-value pair. Does not modify this
     * instance (immutable style).
     *
     * @param key parameter name
     * @param value parameter value
     * @return new Parameters with the added entry
     */
    public Parameters with(String key, Object value) {
        Map<String, Object> newValues = new HashMap<>(this.values);
        newValues.put(key, value);
        return new Parameters(newValues);
    }
}
