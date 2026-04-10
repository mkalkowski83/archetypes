package com.softwarearchetypes.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softwarearchetypes.common.Result;
import com.softwarearchetypes.product.ProductCommands.AllowedValuesConfig;
import com.softwarearchetypes.product.ProductCommands.DateRangeConfig;
import com.softwarearchetypes.product.ProductCommands.DecimalRangeConfig;
import com.softwarearchetypes.product.ProductCommands.DefineProductType;
import com.softwarearchetypes.product.ProductCommands.MandatoryFeature;
import com.softwarearchetypes.product.ProductCommands.NumericRangeConfig;
import com.softwarearchetypes.product.ProductCommands.OptionalFeature;
import com.softwarearchetypes.product.ProductCommands.RegexConfig;
import com.softwarearchetypes.product.ProductCommands.UnconstrainedConfig;
import com.softwarearchetypes.product.ProductQueries.FindByTrackingStrategyCriteria;
import com.softwarearchetypes.product.ProductQueries.FindProductTypeCriteria;
import com.softwarearchetypes.product.ProductViews.FeatureTypeView;
import com.softwarearchetypes.product.ProductViews.ProductTypeView;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for ProductFacade - main API for managing ProductTypes. */
class ProductFacadeTest {

    private ProductConfiguration configuration;
    private ProductFacade facade;

    @BeforeEach
    void setUp() {
        configuration = ProductConfiguration.inMemory();
        facade = configuration.productFacade();
    }

    // ===========================================
    // Defining ProductType
    // ===========================================

    @Test
    void shouldDefineSimpleProductTypeAndFindIt() {
        // given
        String productId = UUID.randomUUID().toString();

        // when
        Result<String, ProductIdentifier> result =
                facade.handle(
                        new DefineProductType(
                                "UUID",
                                productId,
                                "Simple Product",
                                "A simple product without features",
                                "pcs",
                                "IDENTICAL",
                                Set.of(),
                                Set.of(),
                                Map.of()));

        // then
        assertTrue(result.success());
        ProductTypeView found = facade.findBy(new FindProductTypeCriteria(productId)).orElseThrow();
        assertEquals("Simple Product", found.name());
        assertEquals("A simple product without features", found.description());
        assertEquals("IDENTICAL", found.trackingStrategy());
    }

    @Test
    void shouldDefineProductTypeWithMandatoryFeaturesAndFindIt() {
        // given
        String productId = UUID.randomUUID().toString();

        // when
        Result<String, ProductIdentifier> result =
                facade.handle(
                        new DefineProductType(
                                "UUID",
                                productId,
                                "Laptop",
                                "Business laptop with configurable features",
                                "pcs",
                                "INDIVIDUALLY_TRACKED",
                                Set.of(
                                        new MandatoryFeature(
                                                "color",
                                                new AllowedValuesConfig(
                                                        Set.of("Black", "Silver", "Gold"))),
                                        new MandatoryFeature(
                                                "storage",
                                                new AllowedValuesConfig(
                                                        Set.of("256GB", "512GB", "1TB")))),
                                Set.of(),
                                Map.of("category", "electronics")));

        // then
        assertTrue(result.success());
        ProductTypeView found = facade.findBy(new FindProductTypeCriteria(productId)).orElseThrow();
        assertEquals("Laptop", found.name());
        assertEquals("INDIVIDUALLY_TRACKED", found.trackingStrategy());
        assertEquals(2, found.mandatoryFeatures().size());
    }

    @Test
    void shouldDefineProductTypeWithOptionalFeaturesAndFindIt() {
        // given
        String productId = UUID.randomUUID().toString();

        // when
        Result<String, ProductIdentifier> result =
                facade.handle(
                        new DefineProductType(
                                "UUID",
                                productId,
                                "Smartphone",
                                "Smartphone with optional features",
                                "pcs",
                                "INDIVIDUALLY_TRACKED",
                                Set.of(),
                                Set.of(
                                        new OptionalFeature(
                                                "engraving", new UnconstrainedConfig("TEXT")),
                                        new OptionalFeature(
                                                "warranty_years", new NumericRangeConfig(1, 5))),
                                Map.of()));

        // then
        assertTrue(result.success());
        ProductTypeView found = facade.findBy(new FindProductTypeCriteria(productId)).orElseThrow();
        assertEquals(2, found.optionalFeatures().size());
    }

    @Test
    void shouldDefineProductTypeWithAllConstraintTypes() {
        // given
        String productId = UUID.randomUUID().toString();

        // when
        Result<String, ProductIdentifier> result =
                facade.handle(
                        new DefineProductType(
                                "UUID",
                                productId,
                                "Complex Product",
                                "Product with all constraint types",
                                "pcs",
                                "IDENTICAL",
                                Set.of(
                                        new MandatoryFeature(
                                                "color",
                                                new AllowedValuesConfig(
                                                        Set.of("Red", "Blue", "Green"))),
                                        new MandatoryFeature(
                                                "year", new NumericRangeConfig(2020, 2025)),
                                        new MandatoryFeature(
                                                "weight", new DecimalRangeConfig("0.1", "100.0")),
                                        new MandatoryFeature(
                                                "code", new RegexConfig("^[A-Z]{2}-\\d{4}$")),
                                        new MandatoryFeature(
                                                "expiry",
                                                new DateRangeConfig("2024-01-01", "2025-12-31")),
                                        new MandatoryFeature(
                                                "notes", new UnconstrainedConfig("TEXT"))),
                                Set.of(),
                                Map.of()));

        // then
        assertTrue(result.success());
        ProductTypeView found = facade.findBy(new FindProductTypeCriteria(productId)).orElseThrow();
        assertEquals(6, found.mandatoryFeatures().size());
    }

    @Test
    void shouldFailForInvalidIdentifierType() {
        // when
        Result<String, ProductIdentifier> result =
                facade.handle(
                        new DefineProductType(
                                "INVALID_TYPE",
                                "some-id",
                                "Product",
                                "Description",
                                "pcs",
                                "IDENTICAL",
                                Set.of(),
                                Set.of(),
                                Map.of()));

        // then
        assertTrue(result.failure());
        assertTrue(result.getFailure().contains("Unknown product identifier type"));
    }

    // ===========================================
    // Finding by tracking strategy
    // ===========================================

    @Test
    void shouldFindProductTypesByTrackingStrategy() {
        // given
        thereIsProductType("Identical Product 1", "IDENTICAL");
        thereIsProductType("Tracked Product", "INDIVIDUALLY_TRACKED");
        thereIsProductType("Identical Product 2", "IDENTICAL");

        // when
        Set<ProductTypeView> identicalProducts =
                facade.findBy(new FindByTrackingStrategyCriteria("IDENTICAL"));
        Set<ProductTypeView> trackedProducts =
                facade.findBy(new FindByTrackingStrategyCriteria("INDIVIDUALLY_TRACKED"));

        // then
        assertEquals(2, identicalProducts.size());
        assertEquals(1, trackedProducts.size());
    }

    @Test
    void shouldReturnEmptyForNonExistentProduct() {
        // when
        boolean found =
                facade.findBy(new FindProductTypeCriteria(UUID.randomUUID().toString()))
                        .isPresent();

        // then
        assertFalse(found);
    }

    // ===========================================
    // Feature type views
    // ===========================================

    @Test
    void shouldReturnCorrectFeatureTypeViews() {
        // given
        String productId = UUID.randomUUID().toString();
        facade.handle(
                new DefineProductType(
                        "UUID",
                        productId,
                        "Product with Features",
                        "Description",
                        "pcs",
                        "IDENTICAL",
                        Set.of(
                                new MandatoryFeature(
                                        "color", new AllowedValuesConfig(Set.of("Red", "Blue")))),
                        Set.of(new OptionalFeature("size", new NumericRangeConfig(1, 10))),
                        Map.of()));

        // when
        ProductTypeView view = facade.findBy(new FindProductTypeCriteria(productId)).orElseThrow();

        // then
        assertEquals(1, view.mandatoryFeatures().size());
        assertEquals(1, view.optionalFeatures().size());

        FeatureTypeView colorFeature =
                view.mandatoryFeatures().stream()
                        .filter(f -> f.name().equals("color"))
                        .findFirst()
                        .orElseThrow();
        assertEquals("TEXT", colorFeature.valueType());
        assertEquals("ALLOWED_VALUES", colorFeature.constraintType());
        assertNotNull(colorFeature.constraintConfig());
        assertTrue(colorFeature.constraintConfig().containsKey("allowedValues"));

        FeatureTypeView sizeFeature =
                view.optionalFeatures().stream()
                        .filter(f -> f.name().equals("size"))
                        .findFirst()
                        .orElseThrow();
        assertEquals("INTEGER", sizeFeature.valueType());
        assertEquals("NUMERIC_RANGE", sizeFeature.constraintType());
        assertEquals(1, sizeFeature.constraintConfig().get("min"));
        assertEquals(10, sizeFeature.constraintConfig().get("max"));
    }

    // ===========================================
    // Different identifier types
    // ===========================================

    @Test
    void shouldSupportIsbnIdentifier() {
        // when
        Result<String, ProductIdentifier> result =
                facade.handle(
                        new DefineProductType(
                                "ISBN",
                                "0-201-77060-1",
                                "Book",
                                "A sample book",
                                "pcs",
                                "IDENTICAL",
                                Set.of(),
                                Set.of(),
                                Map.of()));

        // then
        assertTrue(result.success());
    }

    @Test
    void shouldSupportGtinIdentifier() {
        // when
        Result<String, ProductIdentifier> result =
                facade.handle(
                        new DefineProductType(
                                "GTIN",
                                "96385074",
                                "Retail Product",
                                "A product with GTIN",
                                "pcs",
                                "IDENTICAL",
                                Set.of(),
                                Set.of(),
                                Map.of()));

        // then
        assertTrue(result.success());
    }

    // ===========================================
    // Command validation
    // ===========================================

    @Test
    void shouldRejectNullProductIdType() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new DefineProductType(
                                null,
                                "id",
                                "Name",
                                "Description",
                                "pcs",
                                "IDENTICAL",
                                Set.of(),
                                Set.of(),
                                Map.of()));
    }

    @Test
    void shouldRejectBlankProductId() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new DefineProductType(
                                "UUID",
                                "   ",
                                "Name",
                                "Description",
                                "pcs",
                                "IDENTICAL",
                                Set.of(),
                                Set.of(),
                                Map.of()));
    }

    @Test
    void shouldRejectBlankName() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new DefineProductType(
                                "UUID",
                                UUID.randomUUID().toString(),
                                "",
                                "Description",
                                "pcs",
                                "IDENTICAL",
                                Set.of(),
                                Set.of(),
                                Map.of()));
    }

    // ===========================================
    // Helper methods
    // ===========================================

    private void thereIsProductType(String name, String trackingStrategy) {
        facade.handle(
                new DefineProductType(
                        "UUID",
                        UUID.randomUUID().toString(),
                        name,
                        "Description of " + name,
                        "pcs",
                        trackingStrategy,
                        Set.of(),
                        Set.of(),
                        Map.of()));
    }
}
