package com.softwarearchetypes.product;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softwarearchetypes.quantity.Unit;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Scenario from lesson: Logistics Transport Premium Package
 *
 * <p>A logistics company offers Transport Premium - a comprehensive service including: - Transport
 * (domestic or international) - Insurance (standard, extended, or partner) - Add-ons (cold chain,
 * fragile handling, pickup service) - Tracking Package (system tracking or active monitoring) -
 * Notification Package (SMS, email, webhook notifications)
 *
 * <p>Business rule: Standard insurance cannot be selected for international transport
 */
class LogisticsTransportScenario {

    // Simple tracking products
    private ProductType systemTracking;
    private ProductType activeMonitoring;

    // Notification products
    private ProductType smsNotification;
    private ProductType emailNotification;
    private ProductType webhookNotification;

    // Transport products
    private ProductType domesticExpress;
    private ProductType internationalExpress;

    // Insurance products
    private ProductType standardCargo;
    private ProductType extendedCargo;
    private ProductType partnerInsurance;

    // Add-on products
    private ProductType coldChain;
    private ProductType fragileHandling;
    private ProductType pickupService;

    // Composite packages
    private PackageType trackingPackage;
    private PackageType notificationPackage;
    private PackageType transportPremium;

    @BeforeEach
    void setUp() {
        // Simple tracking options
        systemTracking =
                ProductType.identical(
                        UuidProductIdentifier.random(),
                        ProductName.of("System Tracking"),
                        ProductDescription.of(
                                "Status-level tracking (dispatched, in transit, delivered)"),
                        Unit.pieces());

        activeMonitoring =
                ProductType.identical(
                        UuidProductIdentifier.random(),
                        ProductName.of("Active Monitoring"),
                        ProductDescription.of("Full GPS monitoring with temperature sensors"),
                        Unit.pieces());

        // Notification options
        smsNotification =
                ProductType.identical(
                        UuidProductIdentifier.random(),
                        ProductName.of("SMS Notification"),
                        ProductDescription.of("SMS alerts for shipment updates"),
                        Unit.pieces());

        emailNotification =
                ProductType.identical(
                        UuidProductIdentifier.random(),
                        ProductName.of("Email Notification"),
                        ProductDescription.of("Email alerts for shipment updates"),
                        Unit.pieces());

        webhookNotification =
                ProductType.identical(
                        UuidProductIdentifier.random(),
                        ProductName.of("Webhook Notification"),
                        ProductDescription.of("Webhook integration for ERP systems"),
                        Unit.pieces());

        // Transport options
        domesticExpress =
                ProductType.identical(
                        UuidProductIdentifier.random(),
                        ProductName.of("Domestic Express"),
                        ProductDescription.of("Fast delivery within country"),
                        Unit.pieces());

        internationalExpress =
                ProductType.identical(
                        UuidProductIdentifier.random(),
                        ProductName.of("International Express"),
                        ProductDescription.of("International delivery with customs"),
                        Unit.pieces());

        // Insurance options
        standardCargo =
                ProductType.identical(
                        UuidProductIdentifier.random(),
                        ProductName.of("Standard Cargo Insurance"),
                        ProductDescription.of("Basic coverage for domestic transport"),
                        Unit.pieces());

        extendedCargo =
                ProductType.identical(
                        UuidProductIdentifier.random(),
                        ProductName.of("Extended Cargo Insurance"),
                        ProductDescription.of("Extended coverage for valuable items"),
                        Unit.pieces());

        partnerInsurance =
                ProductType.identical(
                        UuidProductIdentifier.random(),
                        ProductName.of("Partner Insurance"),
                        ProductDescription.of("Insurance by external provider"),
                        Unit.pieces());

        // Add-on options
        coldChain =
                ProductType.identical(
                        UuidProductIdentifier.random(),
                        ProductName.of("Cold Chain"),
                        ProductDescription.of("Temperature-controlled transport"),
                        Unit.pieces());

        fragileHandling =
                ProductType.identical(
                        UuidProductIdentifier.random(),
                        ProductName.of("Fragile Handling"),
                        ProductDescription.of("Special handling for fragile items"),
                        Unit.pieces());

        pickupService =
                ProductType.identical(
                        UuidProductIdentifier.random(),
                        ProductName.of("Pickup Service"),
                        ProductDescription.of("Pickup from customer location"),
                        Unit.pieces());

        // Tracking Package - choose exactly one tracking option
        trackingPackage =
                Product.builder(
                                UuidProductIdentifier.random(),
                                ProductName.of("Tracking Package"),
                                ProductDescription.of("Shipment tracking and monitoring options"))
                        .asPackageType()
                        .withSingleChoice(
                                "TrackingOption", systemTracking.id(), activeMonitoring.id())
                        .build();

        // Notification Package - optionally choose one notification channel
        notificationPackage =
                Product.builder(
                                UuidProductIdentifier.random(),
                                ProductName.of("Notification Package"),
                                ProductDescription.of("Notification options for shipment updates"))
                        .asPackageType()
                        .withOptionalChoice(
                                "NotificationChannel",
                                smsNotification.id(),
                                emailNotification.id(),
                                webhookNotification.id())
                        .build();

        // Transport Premium Package - the complete offering
        transportPremium =
                Product.builder(
                                UuidProductIdentifier.random(),
                                ProductName.of("Transport Premium Package"),
                                ProductDescription.of(
                                        "Comprehensive transport with insurance and monitoring"))
                        .asPackageType()
                        .withSingleChoice(
                                "Transport", domesticExpress.id(), internationalExpress.id())
                        .withSingleChoice(
                                "Insurance",
                                standardCargo.id(),
                                extendedCargo.id(),
                                partnerInsurance.id())
                        .withChoice(
                                "AddOns",
                                0,
                                2,
                                coldChain.id(),
                                fragileHandling.id(),
                                pickupService.id())
                        // Business rule: standard insurance cannot be selected for international
                        // transport
                        .withProductSet("International", internationalExpress.id())
                        .withProductSet("StandardCargo", standardCargo.id())
                        .withRule(
                                SelectionRule.ifThen(
                                        SelectionRule.single(
                                                new ProductSet(
                                                        "International",
                                                        Set.of(internationalExpress.id()))),
                                        SelectionRule.not(
                                                SelectionRule.single(
                                                        new ProductSet(
                                                                "StandardCargo",
                                                                Set.of(standardCargo.id()))))))
                        // Include smaller packages
                        .withSingleChoice("Tracking", trackingPackage.id())
                        .withOptionalChoice("Notifications", notificationPackage.id())
                        .build();
    }

    @Test
    void shouldAllowDomesticTransportWithStandardInsurance() {
        // Valid combination: domestic + standard insurance
        List<SelectedProduct> selection =
                List.of(
                        new SelectedProduct(domesticExpress.id(), 1),
                        new SelectedProduct(standardCargo.id(), 1),
                        new SelectedProduct(trackingPackage.id(), 1));

        PackageValidationResult result = transportPremium.validateSelection(selection);
        assertTrue(result.isValid(), "Domestic transport with standard insurance should be valid");
    }

    @Test
    void shouldRejectInternationalTransportWithStandardInsurance() {
        // Invalid combination: international + standard insurance
        List<SelectedProduct> selection =
                List.of(
                        new SelectedProduct(internationalExpress.id(), 1),
                        new SelectedProduct(standardCargo.id(), 1),
                        new SelectedProduct(trackingPackage.id(), 1));

        PackageValidationResult result = transportPremium.validateSelection(selection);
        assertFalse(
                result.isValid(),
                "International transport with standard insurance should be rejected");
    }

    @Test
    void shouldAllowInternationalTransportWithExtendedInsurance() {
        // Valid combination: international + extended insurance
        List<SelectedProduct> selection =
                List.of(
                        new SelectedProduct(internationalExpress.id(), 1),
                        new SelectedProduct(extendedCargo.id(), 1),
                        new SelectedProduct(trackingPackage.id(), 1));

        PackageValidationResult result = transportPremium.validateSelection(selection);
        assertTrue(
                result.isValid(),
                "International transport with extended insurance should be valid");
    }

    @Test
    void shouldAllowUpToTwoAddOns() {
        // Valid: 2 add-ons
        List<SelectedProduct> selection =
                List.of(
                        new SelectedProduct(domesticExpress.id(), 1),
                        new SelectedProduct(standardCargo.id(), 1),
                        new SelectedProduct(coldChain.id(), 1),
                        new SelectedProduct(fragileHandling.id(), 1),
                        new SelectedProduct(trackingPackage.id(), 1));

        PackageValidationResult result = transportPremium.validateSelection(selection);
        assertTrue(result.isValid(), "Should allow up to 2 add-ons");
    }

    @Test
    void shouldRejectMoreThanTwoAddOns() {
        // Invalid: 3 add-ons
        List<SelectedProduct> selection =
                List.of(
                        new SelectedProduct(domesticExpress.id(), 1),
                        new SelectedProduct(standardCargo.id(), 1),
                        new SelectedProduct(coldChain.id(), 1),
                        new SelectedProduct(fragileHandling.id(), 1),
                        new SelectedProduct(pickupService.id(), 1),
                        new SelectedProduct(trackingPackage.id(), 1));

        PackageValidationResult result = transportPremium.validateSelection(selection);
        assertFalse(result.isValid(), "Should reject more than 2 add-ons");
    }

    @Test
    void shouldRequireTrackingPackage() {
        // Invalid: missing mandatory tracking package
        List<SelectedProduct> selection =
                List.of(
                        new SelectedProduct(domesticExpress.id(), 1),
                        new SelectedProduct(standardCargo.id(), 1));

        PackageValidationResult result = transportPremium.validateSelection(selection);
        assertFalse(result.isValid(), "Should require tracking package");
    }

    @Test
    void shouldAllowOptionalNotificationPackage() {
        // Valid: with notification package
        List<SelectedProduct> withNotification =
                List.of(
                        new SelectedProduct(domesticExpress.id(), 1),
                        new SelectedProduct(standardCargo.id(), 1),
                        new SelectedProduct(trackingPackage.id(), 1),
                        new SelectedProduct(notificationPackage.id(), 1));

        PackageValidationResult result1 = transportPremium.validateSelection(withNotification);
        assertTrue(result1.isValid(), "Should allow notification package");

        // Valid: without notification package
        List<SelectedProduct> withoutNotification =
                List.of(
                        new SelectedProduct(domesticExpress.id(), 1),
                        new SelectedProduct(standardCargo.id(), 1),
                        new SelectedProduct(trackingPackage.id(), 1));

        PackageValidationResult result2 = transportPremium.validateSelection(withoutNotification);
        assertTrue(result2.isValid(), "Should work without notification package");
    }

    @Test
    void shouldValidateTrackingPackageStructure() {
        // Tracking package should require exactly one tracking option
        List<SelectedProduct> validTracking = List.of(new SelectedProduct(systemTracking.id(), 1));

        PackageValidationResult result1 = trackingPackage.validateSelection(validTracking);
        assertTrue(result1.isValid(), "Should accept one tracking option");

        // Invalid: no tracking option
        PackageValidationResult result2 = trackingPackage.validateSelection(List.of());
        assertFalse(result2.isValid(), "Should reject empty tracking selection");
    }

    @Test
    void shouldValidateNotificationPackageStructure() {
        // Notification package should allow zero or one notification
        List<SelectedProduct> withNotification =
                List.of(new SelectedProduct(emailNotification.id(), 1));

        PackageValidationResult result1 = notificationPackage.validateSelection(withNotification);
        assertTrue(result1.isValid(), "Should accept one notification");

        // Valid: no notification
        PackageValidationResult result2 = notificationPackage.validateSelection(List.of());
        assertTrue(result2.isValid(), "Should accept empty notification selection");
    }
}
