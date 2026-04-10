package com.softwarearchetypes.product;

import static org.junit.jupiter.api.Assertions.*;

import com.softwarearchetypes.common.Result;
import com.softwarearchetypes.quantity.Unit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Scenario: Product Relationships in real business contexts
 *
 * <p>Demonstrates various relationship types between products: - UPGRADABLE_TO: Premium versions,
 * higher tiers - SUBSTITUTED_BY: Alternative products, replacements - REPLACED_BY: Discontinued
 * products replaced by new versions - COMPLEMENTED_BY: Products that work together -
 * COMPATIBLE_WITH: Technical compatibility - INCOMPATIBLE_WITH: Products that cannot be used
 * together
 */
class ProductRelationshipsScenario {

    private ProductTypeRepository productRepository;
    private ProductRelationshipsFacade relationshipsFacade;

    // Mobile plans
    private ProductType basicPlan;
    private ProductType standardPlan;
    private ProductType premiumPlan;

    // Phones
    private ProductType iphone15;
    private ProductType iphone15Pro;
    private ProductType samsungS24;
    private ProductType pixelPhone;

    // Accessories
    private ProductType usbCCharger;
    private ProductType lightningCharger;
    private ProductType wirelessCharger;

    // Software
    private ProductType cloudStorage100GB;
    private ProductType cloudStorage1TB;

    @BeforeEach
    void setUp() {
        ProductConfiguration configuration = ProductConfiguration.inMemory();
        productRepository = configuration.productTypeRepository();
        relationshipsFacade = configuration.productRelationshipsFacade();

        // Create products
        basicPlan =
                ProductType.identical(
                        UuidProductIdentifier.random(),
                        ProductName.of("Basic Plan"),
                        ProductDescription.of("5GB data, unlimited calls"),
                        Unit.pieces());

        standardPlan =
                ProductType.identical(
                        UuidProductIdentifier.random(),
                        ProductName.of("Standard Plan"),
                        ProductDescription.of("20GB data, unlimited calls, EU roaming"),
                        Unit.pieces());

        premiumPlan =
                ProductType.identical(
                        UuidProductIdentifier.random(),
                        ProductName.of("Premium Plan"),
                        ProductDescription.of("Unlimited data, calls, worldwide roaming"),
                        Unit.pieces());

        iphone15 =
                ProductType.individuallyTracked(
                        UuidProductIdentifier.random(),
                        ProductName.of("iPhone 15"),
                        ProductDescription.of("128GB"),
                        Unit.pieces());

        iphone15Pro =
                ProductType.individuallyTracked(
                        UuidProductIdentifier.random(),
                        ProductName.of("iPhone 15 Pro"),
                        ProductDescription.of("256GB with ProMotion display"),
                        Unit.pieces());

        samsungS24 =
                ProductType.individuallyTracked(
                        UuidProductIdentifier.random(),
                        ProductName.of("Samsung Galaxy S24"),
                        ProductDescription.of("256GB"),
                        Unit.pieces());

        pixelPhone =
                ProductType.individuallyTracked(
                        UuidProductIdentifier.random(),
                        ProductName.of("Google Pixel 8"),
                        ProductDescription.of("128GB"),
                        Unit.pieces());

        usbCCharger =
                ProductType.identical(
                        UuidProductIdentifier.random(),
                        ProductName.of("USB-C Fast Charger"),
                        ProductDescription.of("65W USB-C charger"),
                        Unit.pieces());

        lightningCharger =
                ProductType.identical(
                        UuidProductIdentifier.random(),
                        ProductName.of("Lightning Charger"),
                        ProductDescription.of("20W Lightning charger"),
                        Unit.pieces());

        wirelessCharger =
                ProductType.identical(
                        UuidProductIdentifier.random(),
                        ProductName.of("Wireless Charger"),
                        ProductDescription.of("15W MagSafe compatible"),
                        Unit.pieces());

        cloudStorage100GB =
                ProductType.identical(
                        UuidProductIdentifier.random(),
                        ProductName.of("Cloud Storage 100GB"),
                        ProductDescription.of("100GB cloud storage"),
                        Unit.pieces());

        cloudStorage1TB =
                ProductType.identical(
                        UuidProductIdentifier.random(),
                        ProductName.of("Cloud Storage 1TB"),
                        ProductDescription.of("1TB cloud storage"),
                        Unit.pieces());

        // Save to repository
        productRepository.save(basicPlan);
        productRepository.save(standardPlan);
        productRepository.save(premiumPlan);
        productRepository.save(iphone15);
        productRepository.save(iphone15Pro);
        productRepository.save(samsungS24);
        productRepository.save(pixelPhone);
        productRepository.save(usbCCharger);
        productRepository.save(lightningCharger);
        productRepository.save(wirelessCharger);
        productRepository.save(cloudStorage100GB);
        productRepository.save(cloudStorage1TB);
    }

    @Test
    void shouldDefineUpgradePath() {
        // Business scenario: Mobile plan upgrade path
        // Basic → Standard → Premium

        Result<String, ProductRelationshipId> basicToStandard =
                relationshipsFacade.handle(
                        new ProductRelationshipCommands.DefineRelationship(
                                basicPlan.id().toString(),
                                standardPlan.id().toString(),
                                "UPGRADABLE_TO"));

        assertTrue(basicToStandard.success(), "Should create upgrade relationship");

        Result<String, ProductRelationshipId> standardToPremium =
                relationshipsFacade.handle(
                        new ProductRelationshipCommands.DefineRelationship(
                                standardPlan.id().toString(),
                                premiumPlan.id().toString(),
                                "UPGRADABLE_TO"));

        assertTrue(standardToPremium.success(), "Should create upgrade relationship");

        // Customer on Basic plan can see Standard as upgrade option
        assertNotNull(basicToStandard.getSuccess());
        assertNotNull(standardToPremium.getSuccess());
    }

    @Test
    void shouldDefineSubstitutes() {
        // Business scenario: Alternative phones in similar price range
        // iPhone 15 ↔ Samsung S24 ↔ Pixel 8

        Result<String, ProductRelationshipId> iphoneToSamsung =
                relationshipsFacade.handle(
                        new ProductRelationshipCommands.DefineRelationship(
                                iphone15.id().toString(),
                                samsungS24.id().toString(),
                                "SUBSTITUTED_BY"));

        Result<String, ProductRelationshipId> iphoneToPixel =
                relationshipsFacade.handle(
                        new ProductRelationshipCommands.DefineRelationship(
                                iphone15.id().toString(),
                                pixelPhone.id().toString(),
                                "SUBSTITUTED_BY"));

        assertTrue(iphoneToSamsung.success(), "Should create substitute relationship");
        assertTrue(iphoneToPixel.success(), "Should create substitute relationship");

        // If iPhone 15 is out of stock, suggest Samsung S24 or Pixel 8
    }

    @Test
    void shouldDefineReplacement() {
        // Business scenario: iPhone 15 replaced by iPhone 15 Pro (discontinued model)

        Result<String, ProductRelationshipId> replacement =
                relationshipsFacade.handle(
                        new ProductRelationshipCommands.DefineRelationship(
                                iphone15.id().toString(),
                                iphone15Pro.id().toString(),
                                "REPLACED_BY"));

        assertTrue(replacement.success(), "Should create replacement relationship");

        // When customer asks for iPhone 15 (discontinued), suggest iPhone 15 Pro
    }

    @Test
    void shouldDefineComplementaryProducts() {
        // Business scenario: Phone + Cloud storage = better experience

        Result<String, ProductRelationshipId> iphoneWithStorage =
                relationshipsFacade.handle(
                        new ProductRelationshipCommands.DefineRelationship(
                                iphone15.id().toString(),
                                cloudStorage100GB.id().toString(),
                                "COMPLEMENTED_BY"));

        Result<String, ProductRelationshipId> iphoneProWithStorage =
                relationshipsFacade.handle(
                        new ProductRelationshipCommands.DefineRelationship(
                                iphone15Pro.id().toString(),
                                cloudStorage1TB.id().toString(),
                                "COMPLEMENTED_BY"));

        assertTrue(iphoneWithStorage.success(), "Should create complement relationship");
        assertTrue(iphoneProWithStorage.success(), "Should create complement relationship");

        // When buying iPhone, suggest cloud storage as complement
    }

    @Test
    void shouldDefineCompatibility() {
        // Business scenario: USB-C charger compatible with Samsung & Pixel

        Result<String, ProductRelationshipId> usbCWithSamsung =
                relationshipsFacade.handle(
                        new ProductRelationshipCommands.DefineRelationship(
                                usbCCharger.id().toString(),
                                samsungS24.id().toString(),
                                "COMPATIBLE_WITH"));

        Result<String, ProductRelationshipId> usbCWithPixel =
                relationshipsFacade.handle(
                        new ProductRelationshipCommands.DefineRelationship(
                                usbCCharger.id().toString(),
                                pixelPhone.id().toString(),
                                "COMPATIBLE_WITH"));

        assertTrue(usbCWithSamsung.success(), "Should create compatibility relationship");
        assertTrue(usbCWithPixel.success(), "Should create compatibility relationship");

        // Charger listing shows: "Compatible with Samsung S24, Google Pixel 8"
    }

    @Test
    void shouldDefineIncompatibility() {
        // Business scenario: Lightning charger incompatible with USB-C phones

        Result<String, ProductRelationshipId> lightningWithSamsung =
                relationshipsFacade.handle(
                        new ProductRelationshipCommands.DefineRelationship(
                                lightningCharger.id().toString(),
                                samsungS24.id().toString(),
                                "INCOMPATIBLE_WITH"));

        Result<String, ProductRelationshipId> lightningWithPixel =
                relationshipsFacade.handle(
                        new ProductRelationshipCommands.DefineRelationship(
                                lightningCharger.id().toString(),
                                pixelPhone.id().toString(),
                                "INCOMPATIBLE_WITH"));

        assertTrue(lightningWithSamsung.success(), "Should create incompatibility relationship");
        assertTrue(lightningWithPixel.success(), "Should create incompatibility relationship");

        // Prevent adding Lightning charger to cart with Samsung/Pixel phone
    }

    @Test
    void shouldRemoveRelationship() {
        // Business scenario: Remove outdated relationship

        Result<String, ProductRelationshipId> relationship =
                relationshipsFacade.handle(
                        new ProductRelationshipCommands.DefineRelationship(
                                iphone15.id().toString(),
                                samsungS24.id().toString(),
                                "SUBSTITUTED_BY"));

        assertTrue(relationship.success());
        ProductRelationshipId relationshipId = relationship.getSuccess();

        // Later: relationship no longer relevant
        Result<String, ProductRelationshipId> removal =
                relationshipsFacade.handle(
                        new ProductRelationshipCommands.RemoveRelationship(relationshipId.value()));

        assertTrue(removal.success(), "Should remove relationship");
    }

    @Test
    void shouldRejectRelationshipForNonExistentProduct() {
        String fakeProductId = UuidProductIdentifier.random().toString();

        Result<String, ProductRelationshipId> result =
                relationshipsFacade.handle(
                        new ProductRelationshipCommands.DefineRelationship(
                                fakeProductId, iphone15.id().toString(), "UPGRADABLE_TO"));

        assertTrue(result.failure(), "Should reject relationship with non-existent product");
        assertTrue(result.getFailure().contains("PRODUCT_NOT_FOUND"));
    }

    @Test
    void shouldSupportComplexRelationshipGraph() {
        // Business scenario: Complete product ecosystem
        // 1. Basic plan → Standard plan (upgrade)
        // 2. Standard plan → Premium plan (upgrade)
        // 3. iPhone 15 → iPhone 15 Pro (upgrade)
        // 4. iPhone 15 ↔ Samsung S24 (substitute)
        // 5. iPhone 15 + Cloud storage (complement)
        // 6. USB-C charger ↔ Samsung S24 (compatible)
        // 7. Lightning charger ⊗ Samsung S24 (incompatible)

        relationshipsFacade.handle(
                new ProductRelationshipCommands.DefineRelationship(
                        basicPlan.id().toString(), standardPlan.id().toString(), "UPGRADABLE_TO"));

        relationshipsFacade.handle(
                new ProductRelationshipCommands.DefineRelationship(
                        standardPlan.id().toString(),
                        premiumPlan.id().toString(),
                        "UPGRADABLE_TO"));

        relationshipsFacade.handle(
                new ProductRelationshipCommands.DefineRelationship(
                        iphone15.id().toString(), iphone15Pro.id().toString(), "UPGRADABLE_TO"));

        relationshipsFacade.handle(
                new ProductRelationshipCommands.DefineRelationship(
                        iphone15.id().toString(), samsungS24.id().toString(), "SUBSTITUTED_BY"));

        relationshipsFacade.handle(
                new ProductRelationshipCommands.DefineRelationship(
                        iphone15.id().toString(),
                        cloudStorage100GB.id().toString(),
                        "COMPLEMENTED_BY"));

        relationshipsFacade.handle(
                new ProductRelationshipCommands.DefineRelationship(
                        usbCCharger.id().toString(),
                        samsungS24.id().toString(),
                        "COMPATIBLE_WITH"));

        Result<String, ProductRelationshipId> incompatible =
                relationshipsFacade.handle(
                        new ProductRelationshipCommands.DefineRelationship(
                                lightningCharger.id().toString(),
                                samsungS24.id().toString(),
                                "INCOMPATIBLE_WITH"));

        assertTrue(incompatible.success(), "Should build complex relationship graph");
    }

    @Test
    void shouldSupportBidirectionalRelationships() {
        // Business scenario: Wireless charger compatible with both iPhones

        relationshipsFacade.handle(
                new ProductRelationshipCommands.DefineRelationship(
                        wirelessCharger.id().toString(),
                        iphone15.id().toString(),
                        "COMPATIBLE_WITH"));

        Result<String, ProductRelationshipId> reverse =
                relationshipsFacade.handle(
                        new ProductRelationshipCommands.DefineRelationship(
                                iphone15.id().toString(),
                                wirelessCharger.id().toString(),
                                "COMPATIBLE_WITH"));

        assertTrue(reverse.success(), "Should support bidirectional relationships");

        // Both products can reference each other
        // Charger page: "Compatible with iPhone 15"
        // iPhone page: "Works with Wireless Charger"
    }
}
