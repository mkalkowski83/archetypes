package com.softwarearchetypes.pricing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.softwarearchetypes.quantity.money.Money;

/** Fluent assertion helper for ComponentBreakdown. */
public class ComponentBreakdownAssert {

    private final ComponentBreakdown actual;

    private ComponentBreakdownAssert(ComponentBreakdown actual) {
        this.actual = actual;
    }

    public static ComponentBreakdownAssert assertThat(ComponentBreakdown actual) {
        assertNotNull(actual, "ComponentBreakdown should not be null");
        return new ComponentBreakdownAssert(actual);
    }

    public ComponentBreakdownAssert hasName(String expectedName) {
        assertEquals(expectedName, actual.name(), "Component name");
        return this;
    }

    public ComponentBreakdownAssert hasTotal(Money expectedTotal) {
        assertEquals(expectedTotal, actual.total(), "Component total");
        return this;
    }

    public ComponentBreakdownAssert hasChildrenCount(int expectedCount) {
        assertEquals(expectedCount, actual.children().size(), "Number of children");
        return this;
    }

    public ComponentBreakdownAssert hasNoChildren() {
        return hasChildrenCount(0);
    }

    public ComponentBreakdownAssert child(int index) {
        if (index >= actual.children().size()) {
            throw new AssertionError(
                    "Child at index %d does not exist. Total children: %d"
                            .formatted(index, actual.children().size()));
        }
        return new ComponentBreakdownAssert(actual.children().get(index));
    }

    public ComponentBreakdownAssert child(String childName) {
        ComponentBreakdown child =
                actual.children().stream()
                        .filter(c -> c.name().equals(childName))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new AssertionError(
                                                "Child with name '%s' not found"
                                                        .formatted(childName)));
        return new ComponentBreakdownAssert(child);
    }

    public ComponentBreakdown get() {
        return actual;
    }
}
