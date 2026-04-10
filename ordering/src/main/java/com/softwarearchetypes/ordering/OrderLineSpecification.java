package com.softwarearchetypes.ordering;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

record OrderLineSpecification(Map<String, String> attributes) {

    public static OrderLineSpecification empty() {
        return new OrderLineSpecification(Map.of());
    }

    public static OrderLineSpecification of(String key, String value) {
        return new OrderLineSpecification(Map.of(key, value));
    }

    public static OrderLineSpecification of(
            String key1, String value1, String key2, String value2) {
        return new OrderLineSpecification(Map.of(key1, value1, key2, value2));
    }

    public static OrderLineSpecification of(Map<String, String> attributes) {
        return new OrderLineSpecification(Map.copyOf(attributes));
    }

    public OrderLineSpecification with(String key, String value) {
        Map<String, String> newAttributes = new HashMap<>(attributes);
        newAttributes.put(key, value);
        return new OrderLineSpecification(newAttributes);
    }

    public Optional<String> get(String key) {
        return Optional.ofNullable(attributes.get(key));
    }

    public boolean has(String key) {
        return attributes.containsKey(key);
    }

    public Map<String, String> features() {
        return attributes.entrySet().stream()
                .filter(e -> !e.getKey().startsWith("component."))
                .filter(e -> !e.getKey().startsWith("_"))
                .filter(e -> !e.getKey().contains("."))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public Map<String, String> components() {
        return attributes.entrySet().stream()
                .filter(e -> e.getKey().startsWith("component."))
                .collect(
                        Collectors.toMap(
                                e -> e.getKey().substring("component.".length()),
                                Map.Entry::getValue));
    }

    public Map<String, String> preferences() {
        return attributes.entrySet().stream()
                .filter(e -> e.getKey().startsWith("_"))
                .collect(Collectors.toMap(e -> e.getKey().substring(1), Map.Entry::getValue));
    }

    @Override
    public String toString() {
        return "OrderLineSpecification" + attributes;
    }
}
