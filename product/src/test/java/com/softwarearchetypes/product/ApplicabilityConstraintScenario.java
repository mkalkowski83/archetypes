package com.softwarearchetypes.product;

import static com.softwarearchetypes.product.ApplicabilityConstraint.*;
import static org.junit.jupiter.api.Assertions.*;

import com.softwarearchetypes.quantity.Unit;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Scenario from lesson: ApplicabilityConstraint examples
 *
 * <p>Demonstrates products with business rules that restrict their availability based on context
 * (channel, country, age, etc.)
 *
 * <p>Examples: - Mobile-only products (only available on mobile channel) - Pediatric products (only
 * for children under 16 in PL/UK) - Age-restricted products (teenagers only) - Multi-dimensional
 * constraints (country AND channel AND age)
 */
class ApplicabilityConstraintScenario {

    @Test
    void shouldRestrictProductToMobileChannel() {
        // Product only available on mobile channel
        ProductType mobileOnlyProduct =
                Product.builder(
                                UuidProductIdentifier.random(),
                                ProductName.of("Mobile App Premium"),
                                ProductDescription.of("Premium feature available only on mobile"))
                        .asProductType(Unit.pieces(), ProductTrackingStrategy.IDENTICAL)
                        .withApplicabilityConstraint(equalsTo("channel", "mobile"))
                        .build();

        // Valid: mobile context
        ApplicabilityContext mobileContext = ApplicabilityContext.of(Map.of("channel", "mobile"));
        assertTrue(
                mobileOnlyProduct.isApplicableFor(mobileContext),
                "Product should be available on mobile");

        // Invalid: web context
        ApplicabilityContext webContext = ApplicabilityContext.of(Map.of("channel", "web"));
        assertFalse(
                mobileOnlyProduct.isApplicableFor(webContext),
                "Product should NOT be available on web");

        // Invalid: desktop context
        ApplicabilityContext desktopContext = ApplicabilityContext.of(Map.of("channel", "desktop"));
        assertFalse(
                mobileOnlyProduct.isApplicableFor(desktopContext),
                "Product should NOT be available on desktop");
    }

    @Test
    void shouldRestrictPediatricServiceByMultipleDimensions() {
        // Product only for children under 16 in PL or UK, on mobile/web
        ProductType pediatricProduct =
                Product.builder(
                                UuidProductIdentifier.random(),
                                ProductName.of("Pediatric Service"),
                                ProductDescription.of("Service for children"))
                        .asProductType(Unit.pieces(), ProductTrackingStrategy.IDENTICAL)
                        .withApplicabilityConstraint(
                                and(
                                        or(equalsTo("country", "PL"), equalsTo("country", "UK")),
                                        or(
                                                equalsTo("channel", "mobile"),
                                                equalsTo("channel", "web")),
                                        lessThan("age", 16)))
                        .build();

        // Valid: PL, mobile, age 10
        ApplicabilityContext validContext =
                ApplicabilityContext.of(
                        Map.of(
                                "country", "PL",
                                "channel", "mobile",
                                "age", "10"));
        assertTrue(
                pediatricProduct.isApplicableFor(validContext),
                "Should be available for children in PL on mobile");

        // Invalid: age too high (18)
        ApplicabilityContext invalidAge =
                ApplicabilityContext.of(
                        Map.of(
                                "country", "PL",
                                "channel", "mobile",
                                "age", "18"));
        assertFalse(
                pediatricProduct.isApplicableFor(invalidAge), "Should NOT be available for adults");

        // Invalid: wrong country (DE)
        ApplicabilityContext invalidCountry =
                ApplicabilityContext.of(
                        Map.of(
                                "country", "DE",
                                "channel", "mobile",
                                "age", "10"));
        assertFalse(
                pediatricProduct.isApplicableFor(invalidCountry),
                "Should NOT be available in Germany");

        // Invalid: wrong channel (desktop)
        ApplicabilityContext invalidChannel =
                ApplicabilityContext.of(
                        Map.of(
                                "country", "PL",
                                "channel", "desktop",
                                "age", "10"));
        assertFalse(
                pediatricProduct.isApplicableFor(invalidChannel),
                "Should NOT be available on desktop");

        // Valid: UK, web, age 15
        ApplicabilityContext validUK =
                ApplicabilityContext.of(
                        Map.of(
                                "country", "UK",
                                "channel", "web",
                                "age", "15"));
        assertTrue(
                pediatricProduct.isApplicableFor(validUK),
                "Should be available for children in UK on web");
    }

    @Test
    void shouldRestrictProductByAgeRange() {
        // Product for teenagers (13-19 years old)
        ProductType teenProduct =
                Product.builder(
                                UuidProductIdentifier.random(),
                                ProductName.of("Teen Product"),
                                ProductDescription.of("For teenagers only"))
                        .asProductType(Unit.pieces(), ProductTrackingStrategy.IDENTICAL)
                        .withApplicabilityConstraint(between("age", 13, 19))
                        .build();

        // Valid: age 15
        ApplicabilityContext validAge = ApplicabilityContext.of(Map.of("age", "15"));
        assertTrue(teenProduct.isApplicableFor(validAge), "Should be available for 15-year-old");

        // Valid: boundary - age 13
        ApplicabilityContext minAge = ApplicabilityContext.of(Map.of("age", "13"));
        assertTrue(
                teenProduct.isApplicableFor(minAge),
                "Should be available for 13-year-old (min boundary)");

        // Valid: boundary - age 19
        ApplicabilityContext maxAge = ApplicabilityContext.of(Map.of("age", "19"));
        assertTrue(
                teenProduct.isApplicableFor(maxAge),
                "Should be available for 19-year-old (max boundary)");

        // Invalid: too young
        ApplicabilityContext tooYoung = ApplicabilityContext.of(Map.of("age", "10"));
        assertFalse(
                teenProduct.isApplicableFor(tooYoung), "Should NOT be available for 10-year-old");

        // Invalid: too old
        ApplicabilityContext tooOld = ApplicabilityContext.of(Map.of("age", "25"));
        assertFalse(teenProduct.isApplicableFor(tooOld), "Should NOT be available for 25-year-old");
    }

    @Test
    void shouldAllowUniversalProductInAnyContext() {
        // Product without restrictions (default: alwaysTrue)
        ProductType universalProduct =
                ProductType.identical(
                        UuidProductIdentifier.random(),
                        ProductName.of("Universal Product"),
                        ProductDescription.of("No restrictions"),
                        Unit.pieces());

        // Should work in any context
        ApplicabilityContext emptyContext = ApplicabilityContext.empty();
        assertTrue(
                universalProduct.isApplicableFor(emptyContext),
                "Should be available without context");

        ApplicabilityContext someContext = ApplicabilityContext.of(Map.of("country", "PL"));
        assertTrue(
                universalProduct.isApplicableFor(someContext),
                "Should be available with any context");

        ApplicabilityContext richContext =
                ApplicabilityContext.of(
                        Map.of(
                                "channel", "mobile",
                                "age", "99",
                                "country", "ZZ"));
        assertTrue(
                universalProduct.isApplicableFor(richContext),
                "Should be available with any context");
    }

    @Test
    void shouldCombineConstraintsWithInOperator() {
        // Product available only in PL/UK/DE on mobile/web
        ProductType regionalProduct =
                Product.builder(
                                UuidProductIdentifier.random(),
                                ProductName.of("Regional Product"),
                                ProductDescription.of(
                                        "Available in selected countries and channels"))
                        .asProductType(Unit.pieces(), ProductTrackingStrategy.IDENTICAL)
                        .withApplicabilityConstraint(
                                and(
                                        in("country", "PL", "UK", "DE"),
                                        in("channel", "mobile", "web")))
                        .build();

        // Valid: DE + web
        ApplicabilityContext validContext =
                ApplicabilityContext.of(
                        Map.of(
                                "country", "DE",
                                "channel", "web"));
        assertTrue(
                regionalProduct.isApplicableFor(validContext), "Should be available in DE on web");

        // Invalid: France (not in country list)
        ApplicabilityContext invalidCountry =
                ApplicabilityContext.of(
                        Map.of(
                                "country", "FR",
                                "channel", "web"));
        assertFalse(
                regionalProduct.isApplicableFor(invalidCountry),
                "Should NOT be available in France");

        // Invalid: desktop (not in channel list)
        ApplicabilityContext invalidChannel =
                ApplicabilityContext.of(
                        Map.of(
                                "country", "DE",
                                "channel", "desktop"));
        assertFalse(
                regionalProduct.isApplicableFor(invalidChannel),
                "Should NOT be available on desktop");
    }

    @Test
    void shouldSupportNotConstraint() {
        // Product available everywhere EXCEPT on mobile
        ProductType nonMobileProduct =
                Product.builder(
                                UuidProductIdentifier.random(),
                                ProductName.of("Desktop-Only Product"),
                                ProductDescription.of("Not available on mobile"))
                        .asProductType(Unit.pieces(), ProductTrackingStrategy.IDENTICAL)
                        .withApplicabilityConstraint(not(equalsTo("channel", "mobile")))
                        .build();

        // Valid: web
        ApplicabilityContext webContext = ApplicabilityContext.of(Map.of("channel", "web"));
        assertTrue(nonMobileProduct.isApplicableFor(webContext), "Should be available on web");

        // Valid: desktop
        ApplicabilityContext desktopContext = ApplicabilityContext.of(Map.of("channel", "desktop"));
        assertTrue(
                nonMobileProduct.isApplicableFor(desktopContext), "Should be available on desktop");

        // Invalid: mobile
        ApplicabilityContext mobileContext = ApplicabilityContext.of(Map.of("channel", "mobile"));
        assertFalse(
                nonMobileProduct.isApplicableFor(mobileContext),
                "Should NOT be available on mobile");
    }

    @Test
    void shouldSupportComplexOrConstraints() {
        // Product for either premium customers OR in promotional countries
        ProductType specialProduct =
                Product.builder(
                                UuidProductIdentifier.random(),
                                ProductName.of("Special Offer"),
                                ProductDescription.of(
                                        "For premium customers or promotional countries"))
                        .asProductType(Unit.pieces(), ProductTrackingStrategy.IDENTICAL)
                        .withApplicabilityConstraint(
                                or(
                                        equalsTo("customerTier", "premium"),
                                        in("country", "PL", "CZ", "SK")))
                        .build();

        // Valid: premium customer in any country
        ApplicabilityContext premiumCustomer =
                ApplicabilityContext.of(
                        Map.of(
                                "customerTier", "premium",
                                "country", "US"));
        assertTrue(
                specialProduct.isApplicableFor(premiumCustomer),
                "Should be available for premium customer");

        // Valid: regular customer in promotional country
        ApplicabilityContext promoCountry =
                ApplicabilityContext.of(
                        Map.of(
                                "customerTier", "regular",
                                "country", "PL"));
        assertTrue(
                specialProduct.isApplicableFor(promoCountry),
                "Should be available in promotional country");

        // Invalid: regular customer in non-promotional country
        ApplicabilityContext regularCustomer =
                ApplicabilityContext.of(
                        Map.of(
                                "customerTier", "regular",
                                "country", "DE"));
        assertFalse(
                specialProduct.isApplicableFor(regularCustomer),
                "Should NOT be available for regular customer outside promo countries");
    }
}
