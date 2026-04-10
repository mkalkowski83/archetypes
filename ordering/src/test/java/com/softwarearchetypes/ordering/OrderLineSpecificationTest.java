package com.softwarearchetypes.ordering;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;

class OrderLineSpecificationTest {

    @Test
    void featuresShouldReturnOnlyPlainAttributes() {
        // given
        OrderLineSpecification spec =
                OrderLineSpecification.of(
                        Map.of(
                                "color", "black",
                                "size", "XL",
                                "component.cpu", "i7",
                                "_warehouse", "warsaw"));

        // when
        Map<String, String> features = spec.features();

        // then
        assertEquals(2, features.size());
        assertEquals("black", features.get("color"));
        assertEquals("XL", features.get("size"));
    }

    @Test
    void componentsShouldReturnComponentEntriesWithPrefixStripped() {
        // given
        OrderLineSpecification spec =
                OrderLineSpecification.of(
                        Map.of(
                                "component.laptop", "Dell-5540",
                                "component.mouse", "Logitech-MX3",
                                "color", "black"));

        // when
        Map<String, String> components = spec.components();

        // then
        assertEquals(2, components.size());
        assertEquals("Dell-5540", components.get("laptop"));
        assertEquals("Logitech-MX3", components.get("mouse"));
    }

    @Test
    void preferencesShouldReturnUnderscorePrefixedEntriesWithPrefixStripped() {
        // given
        OrderLineSpecification spec =
                OrderLineSpecification.of(
                        Map.of(
                                "_warehouse", "warsaw-central",
                                "_deliveryDate", "2025-01-16",
                                "color", "blue"));

        // when
        Map<String, String> preferences = spec.preferences();

        // then
        assertEquals(2, preferences.size());
        assertEquals("warsaw-central", preferences.get("warehouse"));
        assertEquals("2025-01-16", preferences.get("deliveryDate"));
    }

    @Test
    void mixedAttributesShouldBeCorrectlySeparated() {
        // given
        OrderLineSpecification spec =
                OrderLineSpecification.of(
                        Map.of(
                                "color", "black",
                                "component.cpu", "i7",
                                "_warehouse", "warsaw"));

        // then
        assertEquals(1, spec.features().size());
        assertEquals(1, spec.components().size());
        assertEquals(1, spec.preferences().size());
    }

    @Test
    void componentFeaturesShouldBeExcludedFromPlainFeatures() {
        // given
        OrderLineSpecification spec =
                OrderLineSpecification.of(
                        Map.of(
                                "laptop.color", "black",
                                "ram", "16GB"));

        // when
        Map<String, String> features = spec.features();

        // then
        assertEquals(1, features.size());
        assertEquals("16GB", features.get("ram"));
        assertFalse(features.containsKey("laptop.color"));
    }

    @Test
    void withShouldCreateNewSpecWithAddedAttribute() {
        // given
        OrderLineSpecification original = OrderLineSpecification.of("color", "black");

        // when
        OrderLineSpecification updated = original.with("size", "XL");

        // then
        assertEquals("black", updated.get("color").orElseThrow());
        assertEquals("XL", updated.get("size").orElseThrow());
        assertFalse(original.has("size"));
    }

    @Test
    void getShouldReturnEmptyOptionalForMissingKey() {
        // given
        OrderLineSpecification spec = OrderLineSpecification.of("color", "black");

        // then
        assertTrue(spec.get("color").isPresent());
        assertTrue(spec.get("missing").isEmpty());
    }

    @Test
    void hasShouldCheckKeyPresence() {
        // given
        OrderLineSpecification spec = OrderLineSpecification.of("color", "black");

        // then
        assertTrue(spec.has("color"));
        assertFalse(spec.has("missing"));
    }

    @Test
    void emptyShouldHaveNoAttributes() {
        // when
        OrderLineSpecification spec = OrderLineSpecification.empty();

        // then
        assertTrue(spec.attributes().isEmpty());
        assertTrue(spec.features().isEmpty());
        assertTrue(spec.components().isEmpty());
        assertTrue(spec.preferences().isEmpty());
    }
}
