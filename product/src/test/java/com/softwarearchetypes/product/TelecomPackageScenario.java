package com.softwarearchetypes.product;

import static com.softwarearchetypes.product.ProductTrackingStrategy.IDENTICAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softwarearchetypes.quantity.Quantity;
import com.softwarearchetypes.quantity.Unit;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Scenario: Telecom packages with phones and plans
 *
 * <p>Real-world telecom offering: - Mobile plans (Basic, Standard, Premium) - Phones (budget,
 * mid-range, flagship) - SIM cards and accessories - Business rules: Premium phones require Premium
 * plans - Nested packages: Starter packs with SIM + accessories
 */
class TelecomPackageScenario {

    // Mobile plans
    private ProductType basicPlan;
    private ProductType standardPlan;
    private ProductType premiumPlan;

    // Phones
    private ProductType budgetPhone;
    private ProductType midRangePhone;
    private ProductType flagshipPhone;

    // Accessories
    private ProductType simCard;
    private ProductType phoneCase;
    private ProductType screenProtector;
    private ProductType charger;

    // Packages
    private PackageType starterPack;
    private PackageType phoneBundle;

    @BeforeEach
    void setUp() {
        // Mobile plans
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

        // Phones
        budgetPhone =
                ProductType.individuallyTracked(
                        UuidProductIdentifier.random(),
                        ProductName.of("Samsung Galaxy A15"),
                        ProductDescription.of("Budget smartphone"),
                        Unit.pieces());

        midRangePhone =
                ProductType.individuallyTracked(
                        UuidProductIdentifier.random(),
                        ProductName.of("Google Pixel 8"),
                        ProductDescription.of("Mid-range smartphone"),
                        Unit.pieces());

        flagshipPhone =
                ProductType.individuallyTracked(
                        UuidProductIdentifier.random(),
                        ProductName.of("iPhone 15 Pro Max"),
                        ProductDescription.of("Flagship smartphone"),
                        Unit.pieces());

        // Accessories
        simCard =
                ProductType.individuallyTracked(
                        UuidProductIdentifier.random(),
                        ProductName.of("5G SIM Card"),
                        ProductDescription.of("Nano SIM with eSIM support"),
                        Unit.pieces());

        phoneCase =
                ProductType.identical(
                        UuidProductIdentifier.random(),
                        ProductName.of("Universal Phone Case"),
                        ProductDescription.of("Protective case"),
                        Unit.pieces());

        screenProtector =
                ProductType.identical(
                        UuidProductIdentifier.random(),
                        ProductName.of("Tempered Glass Screen Protector"),
                        ProductDescription.of("9H hardness"),
                        Unit.pieces());

        charger =
                ProductType.identical(
                        UuidProductIdentifier.random(),
                        ProductName.of("Fast Charger 65W"),
                        ProductDescription.of("USB-C fast charging"),
                        Unit.pieces());

        // Starter Pack: SIM + optional accessories
        starterPack =
                Product.builder(
                                UuidProductIdentifier.random(),
                                ProductName.of("5G Starter Pack"),
                                ProductDescription.of("SIM card with optional accessories"))
                        .asPackageType()
                        .withTrackingStrategy(IDENTICAL)
                        .withRequiredChoice("SIM", simCard.id())
                        .withChoice(
                                "Accessories",
                                0,
                                2,
                                phoneCase.id(),
                                screenProtector.id(),
                                charger.id())
                        .build();

        // Phone Bundle: Plan + Phone with business rule
        // Business rule: Flagship phone requires Premium plan
        phoneBundle =
                Product.builder(
                                UuidProductIdentifier.random(),
                                ProductName.of("Phone Bundle"),
                                ProductDescription.of(
                                        "Complete mobile package with phone and plan"))
                        .asPackageType()
                        .withSingleChoice(
                                "Plan", basicPlan.id(), standardPlan.id(), premiumPlan.id())
                        .withSingleChoice(
                                "Phone", budgetPhone.id(), midRangePhone.id(), flagshipPhone.id())
                        // Business rule: flagship phone requires premium plan
                        .withProductSet("Flagship", flagshipPhone.id())
                        .withProductSet("Premium", premiumPlan.id())
                        .withRule(
                                SelectionRule.ifThen(
                                        SelectionRule.single(
                                                new ProductSet(
                                                        "Flagship", Set.of(flagshipPhone.id()))),
                                        SelectionRule.single(
                                                new ProductSet(
                                                        "Premium", Set.of(premiumPlan.id())))))
                        // Include starter pack
                        .withSingleChoice("Starter", starterPack.id())
                        .build();
    }

    @Test
    void shouldValidateStarterPackStructure() {
        // Starter pack: mandatory SIM + up to 2 accessories
        List<SelectedProduct> withAccessories =
                List.of(
                        new SelectedProduct(simCard.id(), 1),
                        new SelectedProduct(phoneCase.id(), 1),
                        new SelectedProduct(screenProtector.id(), 1));

        PackageValidationResult result1 = starterPack.validateSelection(withAccessories);
        assertTrue(result1.isValid(), "Should accept SIM with 2 accessories");

        // Without accessories
        List<SelectedProduct> withoutAccessories = List.of(new SelectedProduct(simCard.id(), 1));

        PackageValidationResult result2 = starterPack.validateSelection(withoutAccessories);
        assertTrue(result2.isValid(), "Should accept SIM without accessories");

        // Too many accessories
        List<SelectedProduct> tooMany =
                List.of(
                        new SelectedProduct(simCard.id(), 1),
                        new SelectedProduct(phoneCase.id(), 1),
                        new SelectedProduct(screenProtector.id(), 1),
                        new SelectedProduct(charger.id(), 1));

        PackageValidationResult result3 = starterPack.validateSelection(tooMany);
        assertFalse(result3.isValid(), "Should reject more than 2 accessories");
    }

    @Test
    void shouldAllowBudgetPhoneWithAnyPlan() {
        // Budget phone works with any plan
        List<SelectedProduct> withBasicPlan =
                List.of(
                        new SelectedProduct(basicPlan.id(), 1),
                        new SelectedProduct(budgetPhone.id(), 1),
                        new SelectedProduct(starterPack.id(), 1));

        PackageValidationResult result = phoneBundle.validateSelection(withBasicPlan);
        assertTrue(result.isValid(), "Budget phone should work with Basic plan");
    }

    @Test
    void shouldAllowMidRangePhoneWithAnyPlan() {
        // Mid-range phone works with any plan
        List<SelectedProduct> withStandardPlan =
                List.of(
                        new SelectedProduct(standardPlan.id(), 1),
                        new SelectedProduct(midRangePhone.id(), 1),
                        new SelectedProduct(starterPack.id(), 1));

        PackageValidationResult result = phoneBundle.validateSelection(withStandardPlan);
        assertTrue(result.isValid(), "Mid-range phone should work with Standard plan");
    }

    @Test
    void shouldEnforcePremiumPlanForFlagshipPhone() {
        // Flagship phone requires premium plan
        List<SelectedProduct> withPremiumPlan =
                List.of(
                        new SelectedProduct(premiumPlan.id(), 1),
                        new SelectedProduct(flagshipPhone.id(), 1),
                        new SelectedProduct(starterPack.id(), 1));

        PackageValidationResult result1 = phoneBundle.validateSelection(withPremiumPlan);
        assertTrue(result1.isValid(), "Flagship phone should work with Premium plan");

        // Should fail: flagship with basic plan
        List<SelectedProduct> withBasicPlan =
                List.of(
                        new SelectedProduct(basicPlan.id(), 1),
                        new SelectedProduct(flagshipPhone.id(), 1),
                        new SelectedProduct(starterPack.id(), 1));

        PackageValidationResult result2 = phoneBundle.validateSelection(withBasicPlan);
        assertFalse(result2.isValid(), "Flagship phone should require Premium plan");

        // Should fail: flagship with standard plan
        List<SelectedProduct> withStandardPlan =
                List.of(
                        new SelectedProduct(standardPlan.id(), 1),
                        new SelectedProduct(flagshipPhone.id(), 1),
                        new SelectedProduct(starterPack.id(), 1));

        PackageValidationResult result3 = phoneBundle.validateSelection(withStandardPlan);
        assertFalse(result3.isValid(), "Flagship phone should require Premium plan");
    }

    @Test
    void shouldCreatePhoneBundleInstance() {
        // Customer chooses: Premium plan + iPhone + Starter pack with SIM + case

        // First, create product instances
        ProductInstance iPhoneInstance =
                new InstanceBuilder(InstanceId.newOne())
                        .withSerial(SerialNumber.of("IPHONE-123456"))
                        .asProductInstance(flagshipPhone)
                        .withQuantity(Quantity.of(1, Unit.pieces()))
                        .build();

        ProductInstance simInstance =
                new InstanceBuilder(InstanceId.newOne())
                        .withSerial(SerialNumber.of("SIM-789012"))
                        .asProductInstance(simCard)
                        .withQuantity(Quantity.of(1, Unit.pieces()))
                        .build();

        ProductInstance caseInstance =
                new InstanceBuilder(InstanceId.newOne())
                        .asProductInstance(phoneCase)
                        .withQuantity(Quantity.of(1, Unit.pieces()))
                        .build();

        ProductInstance planInstance =
                new InstanceBuilder(InstanceId.newOne())
                        .asProductInstance(premiumPlan)
                        .withQuantity(Quantity.of(1, Unit.pieces()))
                        .build();

        // Create starter pack instance
        PackageInstance starterPackInstance =
                new InstanceBuilder(InstanceId.newOne())
                        .withBatch(BatchId.newOne())
                        .asPackageInstance(starterPack)
                        .withSelection(
                                List.of(
                                        new SelectedInstance(simInstance, 1),
                                        new SelectedInstance(caseInstance, 1)))
                        .build();

        // Create phone bundle instance
        PackageInstance phoneBundleInstance =
                new InstanceBuilder(InstanceId.newOne())
                        .withSerial(SerialNumber.of("BUNDLE-2025-001"))
                        .asPackageInstance(phoneBundle)
                        .withSelection(
                                List.of(
                                        new SelectedInstance(planInstance, 1),
                                        new SelectedInstance(iPhoneInstance, 1),
                                        new SelectedInstance(starterPackInstance, 1)))
                        .build();

        assertNotNull(phoneBundleInstance);
        assertEquals(3, phoneBundleInstance.selection().size());
        assertTrue(phoneBundleInstance.serialNumber().isPresent());
    }

    @Test
    void shouldRejectInvalidPhoneBundleInstance() {
        // Invalid: flagship phone with basic plan
        ProductInstance iPhoneInstance =
                new InstanceBuilder(InstanceId.newOne())
                        .withSerial(SerialNumber.of("IPHONE-123456"))
                        .asProductInstance(flagshipPhone)
                        .withQuantity(Quantity.of(1, Unit.pieces()))
                        .build();

        ProductInstance basicPlanInstance =
                new InstanceBuilder(InstanceId.newOne())
                        .asProductInstance(basicPlan)
                        .withQuantity(Quantity.of(1, Unit.pieces()))
                        .build();

        ProductInstance simInstance =
                new InstanceBuilder(InstanceId.newOne())
                        .withSerial(SerialNumber.of("SIM-789012"))
                        .asProductInstance(simCard)
                        .withQuantity(Quantity.of(1, Unit.pieces()))
                        .build();

        PackageInstance starterPackInstance =
                new InstanceBuilder(InstanceId.newOne())
                        .withBatch(BatchId.newOne())
                        .asPackageInstance(starterPack)
                        .withSelection(List.of(new SelectedInstance(simInstance, 1)))
                        .build();

        // Should fail during package instance creation due to business rule
        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    new InstanceBuilder(InstanceId.newOne())
                            .withSerial(SerialNumber.of("BUNDLE-INVALID"))
                            .asPackageInstance(phoneBundle)
                            .withSelection(
                                    List.of(
                                            new SelectedInstance(basicPlanInstance, 1),
                                            new SelectedInstance(iPhoneInstance, 1),
                                            new SelectedInstance(starterPackInstance, 1)))
                            .build();
                },
                "Should reject flagship phone with basic plan");
    }

    @Test
    void shouldSupportNestedPackages() {
        // Starter pack is nested inside phone bundle
        List<SelectedProduct> selection =
                List.of(
                        new SelectedProduct(premiumPlan.id(), 1),
                        new SelectedProduct(flagshipPhone.id(), 1),
                        new SelectedProduct(starterPack.id(), 1));

        PackageValidationResult result = phoneBundle.validateSelection(selection);
        assertTrue(result.isValid(), "Should support nested packages");
    }
}
