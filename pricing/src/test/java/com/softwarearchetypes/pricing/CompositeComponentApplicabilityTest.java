package com.softwarearchetypes.pricing;

import static com.softwarearchetypes.pricing.ApplicabilityConstraint.*;
import static com.softwarearchetypes.pricing.ComponentBreakdownAssert.assertThat;
import static java.time.Clock.fixed;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.softwarearchetypes.quantity.money.Money;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for ApplicabilityConstraint on CompositeComponent — tested through PricingFacade.
 *
 * <p>Key behaviours verified: - CompositeComponent with unsatisfied constraint returns 0 (not its
 * children's sum) - CompositeComponent with satisfied constraint calculates normally - Sibling
 * composites with different constraints are independent - Nested composites: outer constraint gates
 * the entire subtree - Children's own constraints still apply when outer composite is active -
 * Temporal validity and applicability constraint are both required
 */
class CompositeComponentApplicabilityTest {

    static final Instant NOW =
            LocalDateTime.of(2025, 6, 1, 12, 0).atZone(ZoneId.systemDefault()).toInstant();
    static final Clock clock = fixed(NOW, ZoneId.systemDefault());

    private PricingFacade facade = PricingConfiguration.inMemory(clock).pricingFacade();

    @BeforeEach
    void setUp() {
        facade.addCalculator(
                "fixed-100",
                CalculatorType.SIMPLE_FIXED,
                Parameters.of("amount", Money.pln(BigDecimal.valueOf(100))));
        facade.addCalculator(
                "fixed-50",
                CalculatorType.SIMPLE_FIXED,
                Parameters.of("amount", Money.pln(BigDecimal.valueOf(50))));
        facade.addCalculator(
                "fixed-30",
                CalculatorType.SIMPLE_FIXED,
                Parameters.of("amount", Money.pln(BigDecimal.valueOf(30))));
        facade.addCalculator(
                "fixed-20",
                CalculatorType.SIMPLE_FIXED,
                Parameters.of("amount", Money.pln(BigDecimal.valueOf(20))));
        facade.addCalculator(
                "pct-10",
                CalculatorType.PERCENTAGE,
                Parameters.of("percentageRate", BigDecimal.valueOf(10)));
    }

    // ================================================================
    // Scenario 1: premium bundle only for premium customers
    //
    //   total
    //   ├── base-fee            (always: 100 PLN)
    //   └── premium-bundle      (constraint: customer-type = "premium")
    //       ├── premium-feature (50 PLN)
    //       └── loyalty-bonus   (30 PLN)
    //
    //   premium customer  → 100 + (50 + 30) = 180 PLN
    //   standard customer → 100 + 0         = 100 PLN
    // ================================================================

    @Test
    void shouldReturnZeroForCompositeWhenConstraintNotSatisfied() {
        facade.createSimpleComponent("base-fee", "fixed-100");
        facade.createSimpleComponent("premium-feature", "fixed-50");
        facade.createSimpleComponent("loyalty-bonus", "fixed-30");

        facade.createCompositeComponent(
                "premium-bundle",
                Map.of(),
                equalsTo("customer-type", "premium"),
                "premium-feature",
                "loyalty-bonus");

        facade.createCompositeComponent("total", Map.of(), "base-fee", "premium-bundle");

        Parameters standard = Parameters.of("customer-type", "standard");
        Parameters premium = Parameters.of("customer-type", "premium");

        assertEquals(
                Money.pln(BigDecimal.valueOf(100)), facade.calculateComponent("total", standard));
        assertEquals(
                Money.pln(BigDecimal.valueOf(180)), facade.calculateComponent("total", premium));
    }

    @Test
    void shouldIncludeCompositeInBreakdownWithZeroWhenNotApplicable() {
        facade.createSimpleComponent("base-fee", "fixed-100");
        facade.createSimpleComponent("premium-feature", "fixed-50");
        facade.createSimpleComponent("loyalty-bonus", "fixed-30");

        facade.createCompositeComponent(
                "premium-bundle",
                Map.of(),
                equalsTo("customer-type", "premium"),
                "premium-feature",
                "loyalty-bonus");

        facade.createCompositeComponent("total", Map.of(), "base-fee", "premium-bundle");

        Parameters standard = Parameters.of("customer-type", "standard");
        ComponentBreakdown breakdown = facade.calculateComponentBreakdown("total", standard);

        assertThat(breakdown).hasTotal(Money.pln(BigDecimal.valueOf(100)));
        assertThat(breakdown).child("base-fee").hasTotal(Money.pln(BigDecimal.valueOf(100)));
        assertThat(breakdown).child("premium-bundle").hasTotal(Money.pln(BigDecimal.ZERO));
    }

    // ================================================================
    // Scenario 2: mutually exclusive composites by weight range
    //
    //   delivery-cost
    //   ├── light-delivery   (constraint: weight < 5)  → 20 PLN fixed
    //   └── heavy-delivery   (constraint: weight >= 5) → 50 PLN fixed
    //
    //   weight=3  → light applies → 20 PLN
    //   weight=10 → heavy applies → 50 PLN
    // ================================================================

    @Test
    void shouldSelectCorrectCompositeBasedOnNumericConstraint() {
        facade.createSimpleComponent("light-fee", "fixed-20");
        facade.createSimpleComponent("heavy-fee", "fixed-50");

        facade.createCompositeComponent(
                "light-delivery", Map.of(), lessThan("weight", 5), "light-fee");

        facade.createCompositeComponent(
                "heavy-delivery", Map.of(), greaterThanOrEqualTo("weight", 5), "heavy-fee");

        facade.createCompositeComponent(
                "delivery-cost", Map.of(), "light-delivery", "heavy-delivery");

        Parameters light = Parameters.of("weight", BigDecimal.valueOf(3));
        Parameters heavy = Parameters.of("weight", BigDecimal.valueOf(10));

        assertEquals(
                Money.pln(BigDecimal.valueOf(20)),
                facade.calculateComponent("delivery-cost", light));
        assertEquals(
                Money.pln(BigDecimal.valueOf(50)),
                facade.calculateComponent("delivery-cost", heavy));
    }

    @Test
    void shouldReturnZeroForBothCompositesWhenWeightAtNoBoundary() {
        // weight exactly 5 is handled by heavy-delivery (>=5), light-delivery (<5) returns 0
        facade.createSimpleComponent("light-fee", "fixed-20");
        facade.createSimpleComponent("heavy-fee", "fixed-50");

        facade.createCompositeComponent(
                "light-delivery", Map.of(), lessThan("weight", 5), "light-fee");
        facade.createCompositeComponent(
                "heavy-delivery", Map.of(), greaterThanOrEqualTo("weight", 5), "heavy-fee");
        facade.createCompositeComponent(
                "delivery-cost", Map.of(), "light-delivery", "heavy-delivery");

        Parameters boundary = Parameters.of("weight", BigDecimal.valueOf(5));
        assertEquals(
                Money.pln(BigDecimal.valueOf(50)),
                facade.calculateComponent("delivery-cost", boundary));
    }

    // ================================================================
    // Scenario 3: nested composites — outer constraint gates entire subtree
    //
    //   contract
    //   └── hazmat-package    (constraint: cargo = "hazmat")
    //       ├── handling      (always: 100 PLN)
    //       └── inspection    (constraint: zone = "restricted" → 50 PLN, else 0)
    //
    //   cargo=hazmat, zone=restricted → 100 + 50 = 150
    //   cargo=hazmat, zone=standard   → 100 + 0  = 100
    //   cargo=standard (any zone)     → 0
    // ================================================================

    @Test
    void shouldGateEntireSubtreeWithOuterCompositeConstraint() {
        facade.createSimpleComponent("handling-fee", "fixed-100");
        facade.createSimpleComponent("inspection-fee", "fixed-50", equalsTo("zone", "restricted"));

        facade.createCompositeComponent(
                "hazmat-package",
                Map.of(),
                equalsTo("cargo", "hazmat"),
                "handling-fee",
                "inspection-fee");

        facade.createCompositeComponent("contract", Map.of(), "hazmat-package");

        Parameters hazmatRestricted = Parameters.of("cargo", "hazmat", "zone", "restricted");
        Parameters hazmatStandard = Parameters.of("cargo", "hazmat", "zone", "standard");
        Parameters normalCargo = Parameters.of("cargo", "standard", "zone", "restricted");

        assertEquals(
                Money.pln(BigDecimal.valueOf(150)),
                facade.calculateComponent("contract", hazmatRestricted));
        assertEquals(
                Money.pln(BigDecimal.valueOf(100)),
                facade.calculateComponent("contract", hazmatStandard));
        assertEquals(
                Money.pln(BigDecimal.ZERO), facade.calculateComponent("contract", normalCargo));
    }

    // ================================================================
    // Scenario 4: composite constraint combined with temporal validity
    //
    //   total
    //   └── promo-bundle   (constraint: member = "gold",
    //                       validity: between 2025-01-01 and 2025-07-01)
    //       ├── promo-fee  (50 PLN)
    //       └── bonus-fee  (30 PLN)
    //
    //   Before period  + gold member  → 0 (validity fails)
    //   Within period  + gold member  → 80 PLN (both pass)
    //   Within period  + silver member → 0 (constraint fails)
    // ================================================================

    @Test
    void shouldRequireBothValidityAndConstraintForComposite() {
        // promo-bundle valid Jan–Jun 2025, but only for gold members.
        // Outside validity there is no version at all — the component throws (temporal versioning
        // behaviour, tested separately in CompositeComponentVersioningTest).
        // Here we verify that within the validity period the constraint still gates the result.
        facade.createSimpleComponent("promo-fee", "fixed-50");
        facade.createSimpleComponent("bonus-fee", "fixed-30");

        facade.createCompositeComponent(
                "promo-bundle",
                Map.of(),
                equalsTo("member", "gold"),
                Validity.between(
                        LocalDateTime.of(2025, 1, 1, 0, 0), LocalDateTime.of(2025, 7, 1, 0, 0)),
                "promo-fee",
                "bonus-fee");

        facade.createCompositeComponent("total", Map.of(), "promo-bundle");

        // within validity + gold member — both conditions satisfied
        Parameters withinGold =
                Parameters.of("member", "gold")
                        .with("timestamp", LocalDateTime.of(2025, 6, 15, 10, 0));
        assertEquals(
                Money.pln(BigDecimal.valueOf(80)), facade.calculateComponent("total", withinGold));

        // within validity + silver member — validity ok, constraint fails
        Parameters withinSilver =
                Parameters.of("member", "silver")
                        .with("timestamp", LocalDateTime.of(2025, 6, 15, 10, 0));
        assertEquals(Money.pln(BigDecimal.ZERO), facade.calculateComponent("total", withinSilver));
    }

    // ================================================================
    // Scenario 5: composite with dependency — constraint guards baseAmount propagation
    //
    //   service-cost
    //   ├── base-service          (always: 100 PLN)
    //   └── surcharge-bundle      (constraint: tier = "enterprise")
    //       └── surcharge         (10% of base-service)
    //
    //   enterprise: 100 + 10 = 110 PLN
    //   standard:   100 + 0  = 100 PLN
    // ================================================================

    @Test
    void shouldNotComputeChildrenWhenCompositeConstraintNotSatisfied() {
        // surcharge-bundle has NO internal dependency — baseAmount flows from service-cost's
        // enrichment (ValueOf("base-service")) into the params passed to surcharge-bundle.
        // surcharge (pct-10) reads baseAmount from those params directly.
        //
        // ValueOf("base-service") lives in service-cost's dependency map, where base-service
        // IS a sibling child — so it is resolvable from service-cost's componentResults.
        facade.createSimpleComponent("base-service", "fixed-100");
        facade.createSimpleComponent("surcharge", "pct-10");

        facade.createCompositeComponent(
                "surcharge-bundle",
                Map.of(), // no internal deps — baseAmount comes from outer params
                equalsTo("tier", "enterprise"),
                "surcharge");

        facade.createCompositeComponent(
                "service-cost",
                Map.of("surcharge-bundle", Map.of("baseAmount", new ValueOf("base-service"))),
                "base-service",
                "surcharge-bundle"); // base-service computed first

        Parameters enterprise = Parameters.of("tier", "enterprise");
        Parameters standard = Parameters.of("tier", "standard");

        // enterprise: base 100 + surcharge 10% of 100 = 110
        assertEquals(
                Money.pln(BigDecimal.valueOf(110)),
                facade.calculateComponent("service-cost", enterprise));
        // standard:   base 100 + surcharge-bundle returns 0 (constraint not satisfied)
        assertEquals(
                Money.pln(BigDecimal.valueOf(100)),
                facade.calculateComponent("service-cost", standard));
    }
}
