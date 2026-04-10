package com.softwarearchetypes.product;

import static com.softwarearchetypes.product.ApplicabilityConstraint.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ApplicabilityConstraintTest {

    @Test
    void shouldSatisfyEqualsToConstraint() {
        ApplicabilityConstraint constraint = equalsTo("country", "PL");
        ApplicabilityContext context = ApplicabilityContext.of(Map.of("country", "PL"));

        assertTrue(constraint.isSatisfiedBy(context));
    }

    @Test
    void shouldNotSatisfyEqualsToConstraintWhenValueDifferent() {
        ApplicabilityConstraint constraint = equalsTo("country", "PL");
        ApplicabilityContext context = ApplicabilityContext.of(Map.of("country", "UK"));

        assertFalse(constraint.isSatisfiedBy(context));
    }

    @Test
    void shouldNotSatisfyEqualsToConstraintWhenParameterMissing() {
        ApplicabilityConstraint constraint = equalsTo("country", "PL");
        ApplicabilityContext context = ApplicabilityContext.empty();

        assertFalse(constraint.isSatisfiedBy(context));
    }

    @Test
    void shouldSatisfyInConstraintWhenValueInSet() {
        ApplicabilityConstraint constraint = in("channel", "mobile", "web", "tablet");
        ApplicabilityContext context = ApplicabilityContext.of(Map.of("channel", "mobile"));

        assertTrue(constraint.isSatisfiedBy(context));
    }

    @Test
    void shouldNotSatisfyInConstraintWhenValueNotInSet() {
        ApplicabilityConstraint constraint = in("channel", "mobile", "web");
        ApplicabilityContext context = ApplicabilityContext.of(Map.of("channel", "desktop"));

        assertFalse(constraint.isSatisfiedBy(context));
    }

    @Test
    void shouldSatisfyGreaterThanConstraint() {
        ApplicabilityConstraint constraint = greaterThan("age", 18);
        ApplicabilityContext context = ApplicabilityContext.of(Map.of("age", "25"));

        assertTrue(constraint.isSatisfiedBy(context));
    }

    @Test
    void shouldNotSatisfyGreaterThanConstraintWhenEqual() {
        ApplicabilityConstraint constraint = greaterThan("age", 18);
        ApplicabilityContext context = ApplicabilityContext.of(Map.of("age", "18"));

        assertFalse(constraint.isSatisfiedBy(context));
    }

    @Test
    void shouldNotSatisfyGreaterThanConstraintWhenLess() {
        ApplicabilityConstraint constraint = greaterThan("age", 18);
        ApplicabilityContext context = ApplicabilityContext.of(Map.of("age", "15"));

        assertFalse(constraint.isSatisfiedBy(context));
    }

    @Test
    void shouldNotSatisfyGreaterThanConstraintWhenNotNumeric() {
        ApplicabilityConstraint constraint = greaterThan("age", 18);
        ApplicabilityContext context = ApplicabilityContext.of(Map.of("age", "adult"));

        assertFalse(constraint.isSatisfiedBy(context));
    }

    @Test
    void shouldSatisfyLessThanConstraint() {
        ApplicabilityConstraint constraint = lessThan("age", 16);
        ApplicabilityContext context = ApplicabilityContext.of(Map.of("age", "12"));

        assertTrue(constraint.isSatisfiedBy(context));
    }

    @Test
    void shouldNotSatisfyLessThanConstraintWhenGreater() {
        ApplicabilityConstraint constraint = lessThan("age", 16);
        ApplicabilityContext context = ApplicabilityContext.of(Map.of("age", "20"));

        assertFalse(constraint.isSatisfiedBy(context));
    }

    @Test
    void shouldSatisfyBetweenConstraint() {
        ApplicabilityConstraint constraint = between("age", 18, 65);
        ApplicabilityContext context = ApplicabilityContext.of(Map.of("age", "30"));

        assertTrue(constraint.isSatisfiedBy(context));
    }

    @Test
    void shouldSatisfyBetweenConstraintAtMinBoundary() {
        ApplicabilityConstraint constraint = between("age", 18, 65);
        ApplicabilityContext context = ApplicabilityContext.of(Map.of("age", "18"));

        assertTrue(constraint.isSatisfiedBy(context));
    }

    @Test
    void shouldSatisfyBetweenConstraintAtMaxBoundary() {
        ApplicabilityConstraint constraint = between("age", 18, 65);
        ApplicabilityContext context = ApplicabilityContext.of(Map.of("age", "65"));

        assertTrue(constraint.isSatisfiedBy(context));
    }

    @Test
    void shouldNotSatisfyBetweenConstraintWhenOutsideRange() {
        ApplicabilityConstraint constraint = between("age", 18, 65);
        ApplicabilityContext context = ApplicabilityContext.of(Map.of("age", "70"));

        assertFalse(constraint.isSatisfiedBy(context));
    }

    @Test
    void shouldSatisfyAndConstraintWhenAllConstraintsMet() {
        ApplicabilityConstraint constraint =
                and(equalsTo("country", "PL"), equalsTo("channel", "mobile"));
        ApplicabilityContext context =
                ApplicabilityContext.of(
                        Map.of(
                                "country", "PL",
                                "channel", "mobile"));

        assertTrue(constraint.isSatisfiedBy(context));
    }

    @Test
    void shouldNotSatisfyAndConstraintWhenOneConstraintNotMet() {
        ApplicabilityConstraint constraint =
                and(equalsTo("country", "PL"), equalsTo("channel", "mobile"));
        ApplicabilityContext context =
                ApplicabilityContext.of(
                        Map.of(
                                "country", "PL",
                                "channel", "web"));

        assertFalse(constraint.isSatisfiedBy(context));
    }

    @Test
    void shouldSatisfyOrConstraintWhenAnyConstraintMet() {
        ApplicabilityConstraint constraint =
                or(equalsTo("country", "PL"), equalsTo("country", "UK"));
        ApplicabilityContext context = ApplicabilityContext.of(Map.of("country", "UK"));

        assertTrue(constraint.isSatisfiedBy(context));
    }

    @Test
    void shouldNotSatisfyOrConstraintWhenNoConstraintMet() {
        ApplicabilityConstraint constraint =
                or(equalsTo("country", "PL"), equalsTo("country", "UK"));
        ApplicabilityContext context = ApplicabilityContext.of(Map.of("country", "DE"));

        assertFalse(constraint.isSatisfiedBy(context));
    }

    @Test
    void shouldSatisfyNotConstraint() {
        ApplicabilityConstraint constraint = not(equalsTo("country", "PL"));
        ApplicabilityContext context = ApplicabilityContext.of(Map.of("country", "UK"));

        assertTrue(constraint.isSatisfiedBy(context));
    }

    @Test
    void shouldNotSatisfyNotConstraint() {
        ApplicabilityConstraint constraint = not(equalsTo("country", "PL"));
        ApplicabilityContext context = ApplicabilityContext.of(Map.of("country", "PL"));

        assertFalse(constraint.isSatisfiedBy(context));
    }

    @Test
    void shouldSatisfyComplexNestedConstraint() {
        // (country = PL OR country = UK) AND (channel = mobile OR channel = web) AND age < 16
        ApplicabilityConstraint constraint =
                and(
                        or(equalsTo("country", "PL"), equalsTo("country", "UK")),
                        or(equalsTo("channel", "mobile"), equalsTo("channel", "web")),
                        lessThan("age", 16));

        ApplicabilityContext context =
                ApplicabilityContext.of(
                        Map.of(
                                "country", "UK",
                                "channel", "mobile",
                                "age", "12"));

        assertTrue(constraint.isSatisfiedBy(context));
    }

    @Test
    void shouldNotSatisfyComplexNestedConstraintWhenOnePartFails() {
        // Same constraint as above
        ApplicabilityConstraint constraint =
                and(
                        or(equalsTo("country", "PL"), equalsTo("country", "UK")),
                        or(equalsTo("channel", "mobile"), equalsTo("channel", "web")),
                        lessThan("age", 16));

        // Age is too high
        ApplicabilityContext context =
                ApplicabilityContext.of(
                        Map.of(
                                "country", "UK",
                                "channel", "mobile",
                                "age", "18"));

        assertFalse(constraint.isSatisfiedBy(context));
    }

    @Test
    void shouldSatisfyAlwaysTrueConstraint() {
        ApplicabilityConstraint constraint = alwaysTrue();
        ApplicabilityContext context = ApplicabilityContext.empty();

        assertTrue(constraint.isSatisfiedBy(context));
    }

    @Test
    void shouldSatisfyAlwaysTrueConstraintWithAnyContext() {
        ApplicabilityConstraint constraint = alwaysTrue();
        ApplicabilityContext context =
                ApplicabilityContext.of(
                        Map.of(
                                "country", "PL",
                                "channel", "mobile"));

        assertTrue(constraint.isSatisfiedBy(context));
    }

    @Test
    void shouldUseApplicabilityConstraintInProductType() {
        ProductType mobileOnlyProduct =
                ProductType.builder(
                                UuidProductIdentifier.random(),
                                ProductName.of("Mobile App Premium"),
                                ProductDescription.of("Premium feature available only on mobile"),
                                com.softwarearchetypes.quantity.Unit.pieces(),
                                ProductTrackingStrategy.IDENTICAL)
                        .withApplicabilityConstraint(equalsTo("channel", "mobile"))
                        .build();

        ApplicabilityContext mobileContext = ApplicabilityContext.of(Map.of("channel", "mobile"));
        ApplicabilityContext webContext = ApplicabilityContext.of(Map.of("channel", "web"));

        assertTrue(mobileOnlyProduct.isApplicableFor(mobileContext));
        assertFalse(mobileOnlyProduct.isApplicableFor(webContext));
    }

    @Test
    void shouldUseComplexApplicabilityConstraintInProductType() {
        // Product only for PL/UK, on mobile/web, for users under 16
        ProductType pediatricProduct =
                ProductType.builder(
                                UuidProductIdentifier.random(),
                                ProductName.of("Pediatric Service"),
                                ProductDescription.of("Service for children"),
                                com.softwarearchetypes.quantity.Unit.pieces(),
                                ProductTrackingStrategy.IDENTICAL)
                        .withApplicabilityConstraint(
                                and(
                                        or(equalsTo("country", "PL"), equalsTo("country", "UK")),
                                        or(
                                                equalsTo("channel", "mobile"),
                                                equalsTo("channel", "web")),
                                        lessThan("age", 16)))
                        .build();

        ApplicabilityContext validContext =
                ApplicabilityContext.of(
                        Map.of(
                                "country", "PL",
                                "channel", "mobile",
                                "age", "10"));

        ApplicabilityContext invalidContextAge =
                ApplicabilityContext.of(
                        Map.of(
                                "country", "PL",
                                "channel", "mobile",
                                "age", "18"));

        ApplicabilityContext invalidContextCountry =
                ApplicabilityContext.of(
                        Map.of(
                                "country", "DE",
                                "channel", "mobile",
                                "age", "10"));

        assertTrue(pediatricProduct.isApplicableFor(validContext));
        assertFalse(pediatricProduct.isApplicableFor(invalidContextAge));
        assertFalse(pediatricProduct.isApplicableFor(invalidContextCountry));
    }

    @Test
    void shouldUseDefaultAlwaysTrueConstraintWhenNotSpecified() {
        ProductType product =
                ProductType.identical(
                        UuidProductIdentifier.random(),
                        ProductName.of("Universal Product"),
                        ProductDescription.of("No restrictions"),
                        com.softwarearchetypes.quantity.Unit.pieces());

        ApplicabilityContext anyContext1 = ApplicabilityContext.empty();
        ApplicabilityContext anyContext2 = ApplicabilityContext.of(Map.of("country", "PL"));
        ApplicabilityContext anyContext3 =
                ApplicabilityContext.of(Map.of("channel", "mobile", "age", "99"));

        assertTrue(product.isApplicableFor(anyContext1));
        assertTrue(product.isApplicableFor(anyContext2));
        assertTrue(product.isApplicableFor(anyContext3));
    }

    @Test
    void shouldCombineConstraintsWithInOperator() {
        ApplicabilityConstraint constraint =
                and(in("country", "PL", "UK", "DE"), in("channel", "mobile", "web"));

        ApplicabilityContext validContext =
                ApplicabilityContext.of(
                        Map.of(
                                "country", "DE",
                                "channel", "web"));

        ApplicabilityContext invalidContext =
                ApplicabilityContext.of(
                        Map.of(
                                "country", "FR",
                                "channel", "web"));

        assertTrue(constraint.isSatisfiedBy(validContext));
        assertFalse(constraint.isSatisfiedBy(invalidContext));
    }

    @Test
    void shouldUseBetweenForRangeConstraints() {
        ProductType ageRestrictedProduct =
                ProductType.builder(
                                UuidProductIdentifier.random(),
                                ProductName.of("Teen Product"),
                                ProductDescription.of("For teenagers only"),
                                com.softwarearchetypes.quantity.Unit.pieces(),
                                ProductTrackingStrategy.IDENTICAL)
                        .withApplicabilityConstraint(between("age", 13, 19))
                        .build();

        ApplicabilityContext validContext = ApplicabilityContext.of(Map.of("age", "15"));
        ApplicabilityContext tooYoung = ApplicabilityContext.of(Map.of("age", "10"));
        ApplicabilityContext tooOld = ApplicabilityContext.of(Map.of("age", "25"));

        assertTrue(ageRestrictedProduct.isApplicableFor(validContext));
        assertFalse(ageRestrictedProduct.isApplicableFor(tooYoung));
        assertFalse(ageRestrictedProduct.isApplicableFor(tooOld));
    }
}
