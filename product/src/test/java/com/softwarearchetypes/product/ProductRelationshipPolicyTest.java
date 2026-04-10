package com.softwarearchetypes.product;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ProductRelationshipPolicyTest {

    private final InMemoryProductTypeRepository productTypeRepository =
            new InMemoryProductTypeRepository();

    @Test
    void shouldPreventSelfRelationship() {
        NoSelfRelationshipPolicy policy = new NoSelfRelationshipPolicy();
        ProductIdentifier productId = UuidProductIdentifier.random();

        boolean canDefine =
                policy.canDefineFor(productId, productId, ProductRelationshipType.COMPATIBLE_WITH);

        assertFalse(canDefine);
    }

    @Test
    void shouldAllowRelationshipBetweenDifferentProducts() {
        NoSelfRelationshipPolicy policy = new NoSelfRelationshipPolicy();
        ProductIdentifier product1 = UuidProductIdentifier.random();
        ProductIdentifier product2 = UuidProductIdentifier.random();

        boolean canDefine =
                policy.canDefineFor(product1, product2, ProductRelationshipType.COMPATIBLE_WITH);

        assertTrue(canDefine);
    }

    @Test
    void shouldPreventCompatibilityBetweenSeasonalAndNonSeasonal() {
        NoSeasonalCompatibilityPolicy policy =
                new NoSeasonalCompatibilityPolicy(productTypeRepository);

        ProductType pumpkinSpiceLatte = createSeasonalProduct();
        ProductType regularLatte = createNonSeasonalProduct();

        boolean canDefine =
                policy.canDefineFor(
                        pumpkinSpiceLatte.identifier(),
                        regularLatte.identifier(),
                        ProductRelationshipType.COMPATIBLE_WITH);

        assertFalse(canDefine);
    }

    @Test
    void shouldAllowCompatibilityBetweenBothSeasonal() {
        NoSeasonalCompatibilityPolicy policy =
                new NoSeasonalCompatibilityPolicy(productTypeRepository);

        ProductType pumpkinSpiceLatte = createSeasonalProduct();
        ProductType gingerbreadLatte = createSeasonalProduct();

        boolean canDefine =
                policy.canDefineFor(
                        pumpkinSpiceLatte.identifier(),
                        gingerbreadLatte.identifier(),
                        ProductRelationshipType.COMPATIBLE_WITH);

        assertTrue(canDefine);
    }

    @Test
    void shouldAllowCompatibilityBetweenBothNonSeasonal() {
        NoSeasonalCompatibilityPolicy policy =
                new NoSeasonalCompatibilityPolicy(productTypeRepository);

        ProductType regularLatte = createNonSeasonalProduct();
        ProductType cappuccino = createNonSeasonalProduct();

        boolean canDefine =
                policy.canDefineFor(
                        regularLatte.identifier(),
                        cappuccino.identifier(),
                        ProductRelationshipType.COMPATIBLE_WITH);

        assertTrue(canDefine);
    }

    @Test
    void shouldAllowNonCompatibilityRelationshipsBetweenSeasonalAndNonSeasonal() {
        NoSeasonalCompatibilityPolicy policy =
                new NoSeasonalCompatibilityPolicy(productTypeRepository);

        ProductType pumpkinSpiceLatte = createSeasonalProduct();
        ProductType regularLatte = createNonSeasonalProduct();

        // Policy only restricts COMPATIBLE_WITH, not other types
        boolean canDefineUpgrade =
                policy.canDefineFor(
                        pumpkinSpiceLatte.identifier(),
                        regularLatte.identifier(),
                        ProductRelationshipType.UPGRADABLE_TO);

        assertTrue(canDefineUpgrade);
    }

    private ProductType createSeasonalProduct() {
        ProductType product =
                ProductType.builder(
                                UuidProductIdentifier.random(),
                                ProductName.of("Seasonal Product"),
                                ProductDescription.of("Seasonal"),
                                com.softwarearchetypes.quantity.Unit.pieces(),
                                ProductTrackingStrategy.IDENTICAL)
                        .withMetadata("seasonal", "true")
                        .build();
        productTypeRepository.save(product);
        return product;
    }

    private ProductType createNonSeasonalProduct() {
        ProductType product =
                ProductType.builder(
                                UuidProductIdentifier.random(),
                                ProductName.of("Non-Seasonal Product"),
                                ProductDescription.of("Regular"),
                                com.softwarearchetypes.quantity.Unit.pieces(),
                                ProductTrackingStrategy.IDENTICAL)
                        .withMetadata("seasonal", "false")
                        .build();
        productTypeRepository.save(product);
        return product;
    }
}

class NoSelfRelationshipPolicy implements ProductRelationshipDefiningPolicy {
    @Override
    public boolean canDefineFor(
            ProductIdentifier from, ProductIdentifier to, ProductRelationshipType type) {
        return !from.equals(to);
    }
}

class NoSeasonalCompatibilityPolicy implements ProductRelationshipDefiningPolicy {
    private final ProductTypeRepository productRepo;

    NoSeasonalCompatibilityPolicy(ProductTypeRepository productRepo) {
        this.productRepo = productRepo;
    }

    @Override
    public boolean canDefineFor(
            ProductIdentifier from, ProductIdentifier to, ProductRelationshipType type) {
        if (type != ProductRelationshipType.COMPATIBLE_WITH) {
            return true;
        }

        ProductType fromProduct = productRepo.findById(from).orElseThrow();
        ProductType toProduct = productRepo.findById(to).orElseThrow();

        boolean fromSeasonal =
                "true".equals(fromProduct.metadata().getOrDefault("seasonal", "false"));
        boolean toSeasonal = "true".equals(toProduct.metadata().getOrDefault("seasonal", "false"));

        return fromSeasonal == toSeasonal;
    }
}
