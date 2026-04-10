package com.softwarearchetypes.pricing;

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
 * E-Mobility charging session scenario using Component semantic composition.
 *
 * <p>Scenario: Customer charged EV with: - 12 kWh of energy - Session duration: 40 minutes
 *
 * <p>Price breakdown: - Energy cost (wholesale + grid) - CPO markup (session fee + per kWh + per
 * minute) - EMSP markup (per kWh + per minute) - VAT 23% on net amount
 */
class EMobilityComponentScenarioTest {

    static final Instant NOW =
            LocalDateTime.of(2025, 1, 15, 12, 50).atZone(ZoneId.systemDefault()).toInstant();
    static final Clock clock = fixed(NOW, ZoneId.systemDefault());
    private final PricingFacade facade = PricingConfiguration.inMemory(clock).pricingFacade();

    @BeforeEach
    void setUp() {
        // ============================================================
        // ENERGY CALCULATORS
        // ============================================================

        // Energy wholesale - progressive step pricing
        // Base price: 0.60 PLN for first tier (0-5 kWh)
        // Each 5 kWh tier increases by 0.10 PLN
        facade.addCalculator(
                "energy-wholesale",
                CalculatorType.STEP_FUNCTION,
                Parameters.of(
                        "basePrice", Money.pln(BigDecimal.valueOf(0.60)),
                        "stepSize", BigDecimal.valueOf(5),
                        "stepIncrement", BigDecimal.valueOf(0.10),
                        "stepBoundary", StepBoundary.INCLUSIVE,
                        "interpretation", Interpretation.MARGINAL));

        // Energy grid fee - flat 0.15 PLN per kWh
        facade.addCalculator(
                "energy-grid",
                CalculatorType.SIMPLE_FIXED,
                Parameters.of(
                        "amount",
                        Money.pln(BigDecimal.valueOf(0.15)),
                        "interpretation",
                        Interpretation.UNIT));

        // ============================================================
        // CPO (Charging Point Operator) CALCULATORS
        // ============================================================

        // CPO session fee - flat 1.50 PLN per session
        facade.addCalculator(
                "cpo-session-fee",
                CalculatorType.SIMPLE_FIXED,
                Parameters.of(
                        "amount",
                        Money.pln(new BigDecimal("1.50")),
                        "interpretation",
                        Interpretation.TOTAL));

        // CPO per kWh markup - 0.25 PLN per kWh
        facade.addCalculator(
                "cpo-per-kwh",
                CalculatorType.SIMPLE_FIXED,
                Parameters.of(
                        "amount",
                        Money.pln(BigDecimal.valueOf(0.25)),
                        "interpretation",
                        Interpretation.UNIT));

        // CPO per minute markup - 0.10 PLN per minute
        facade.addCalculator(
                "cpo-per-minute",
                CalculatorType.SIMPLE_FIXED,
                Parameters.of(
                        "amount",
                        Money.pln(BigDecimal.valueOf(0.10)),
                        "interpretation",
                        Interpretation.UNIT));

        // ============================================================
        // EMSP (E-Mobility Service Provider) CALCULATORS
        // ============================================================

        // EMSP per kWh markup - 0.10 PLN per kWh
        facade.addCalculator(
                "emsp-per-kwh",
                CalculatorType.SIMPLE_FIXED,
                Parameters.of(
                        "amount",
                        Money.pln(BigDecimal.valueOf(0.10)),
                        "interpretation",
                        Interpretation.UNIT));

        // EMSP per minute markup - 0.05 PLN per minute
        facade.addCalculator(
                "emsp-per-minute",
                CalculatorType.SIMPLE_FIXED,
                Parameters.of(
                        "amount",
                        Money.pln(BigDecimal.valueOf(0.05)),
                        "interpretation",
                        Interpretation.UNIT));

        // ============================================================
        // VAT CALCULATOR
        // ============================================================

        // VAT 23% - percentage calculator
        facade.addCalculator(
                "vat-rate",
                CalculatorType.PERCENTAGE,
                Parameters.of(
                        "percentageRate",
                        BigDecimal.valueOf(23),
                        "interpretation",
                        Interpretation.TOTAL));
    }

    @Test
    void shouldCalculateCompleteChargingSessionWithBreakdown() {
        // ============================================================
        // STEP 1: Create energy components
        // ============================================================

        // Energy wholesale - progressive step pricing
        facade.createSimpleComponent("energy-wholesale-component", "energy-wholesale");

        // Grid fee
        facade.createSimpleComponent("energy-grid-component", "energy-grid");

        // Energy net = wholesale + grid
        facade.createCompositeComponent(
                "energy-net", Map.of(), "energy-wholesale-component", "energy-grid-component");

        // ============================================================
        // STEP 2: Create CPO markup components
        // ============================================================
        facade.createSimpleComponent("cpo-session-component", "cpo-session-fee");
        facade.createSimpleComponent("cpo-kwh-component", "cpo-per-kwh");
        facade.createSimpleComponent(
                "cpo-time-component", "cpo-per-minute", Map.of("time", "quantity"));

        facade.createCompositeComponent(
                "cpo-markup",
                Map.of(),
                "cpo-session-component",
                "cpo-kwh-component",
                "cpo-time-component");

        // ============================================================
        // STEP 3: Create EMSP markup components
        // ============================================================
        facade.createSimpleComponent("emsp-kwh-component", "emsp-per-kwh");
        facade.createSimpleComponent(
                "emsp-time-component", "emsp-per-minute", Map.of("time", "quantity"));

        facade.createCompositeComponent(
                "emsp-markup", Map.of(), "emsp-kwh-component", "emsp-time-component");

        // ============================================================
        // STEP 4: Create net amount (before VAT)
        // ============================================================
        facade.createCompositeComponent(
                "netto", Map.of(), "energy-net", "cpo-markup", "emsp-markup");

        // ============================================================
        // STEP 5: Create VAT component with dependency on netto
        // ============================================================
        facade.createSimpleComponent("vat-component", "vat-rate");

        facade.createCompositeComponent(
                "total-session-cost",
                Map.of("vat-component", Map.of("baseAmount", new ValueOf("netto"))),
                "netto",
                "vat-component");

        // ============================================================
        // STEP 6: Calculate for 12 kWh and 40 minutes
        // ============================================================

        Parameters sessionParams =
                Parameters.of(
                        "quantity", BigDecimal.valueOf(12), // 12 kWh
                        "time", BigDecimal.valueOf(40) // 40 minutes
                        );

        Money result = facade.calculateComponent("total-session-cost", sessionParams);

        // Expected breakdown:
        // energy-wholesale (MARGINAL, INCLUSIVE):
        //   1-5 kWh @ 0.60 PLN = 3.00 PLN
        //   6-10 kWh @ 0.70 PLN = 3.50 PLN
        //   11-12 kWh @ 0.80 PLN = 1.60 PLN
        //   total = 8.10 PLN
        // energy-grid: 12 kWh × 0.15 = 1.80 PLN
        // energy-net: 8.10 + 1.80 = 9.90 PLN
        //
        // cpo-session: 1.50 PLN
        // cpo-kwh: 12 × 0.25 = 3.00 PLN
        // cpo-time: 40 × 0.10 = 4.00 PLN
        // cpo-markup: 1.50 + 3.00 + 4.00 = 8.50 PLN
        //
        // emsp-kwh: 12 × 0.10 = 1.20 PLN
        // emsp-time: 40 × 0.05 = 2.00 PLN
        // emsp-markup: 1.20 + 2.00 = 3.20 PLN
        //
        // netto: 9.90 + 8.50 + 3.20 = 21.60 PLN
        // vat: 21.60 × 23% = 4.97 PLN
        // total: 21.60 + 4.97 = 26.57 PLN

        Money expectedTotal = Money.pln(new BigDecimal("26.57"));
        assertEquals(expectedTotal, result);

        // ============================================================
        // STEP 7: Verify detailed breakdown
        // ============================================================
        ComponentBreakdown breakdown =
                facade.calculateComponentBreakdown("total-session-cost", sessionParams);

        assertThat(breakdown)
                .hasName("total-session-cost")
                .hasTotal(Money.pln(new BigDecimal("26.57")))
                .hasChildrenCount(2);

        // Verify netto
        assertThat(breakdown)
                .child("netto")
                .hasTotal(Money.pln(new BigDecimal("21.60")))
                .hasChildrenCount(3);

        // Verify energy-net
        assertThat(breakdown)
                .child("netto")
                .child("energy-net")
                .hasTotal(Money.pln(new BigDecimal("9.90")))
                .hasChildrenCount(2);

        // Verify energy-wholesale
        assertThat(breakdown)
                .child("netto")
                .child("energy-net")
                .child("energy-wholesale-component")
                .hasTotal(Money.pln(new BigDecimal("8.10")))
                .hasNoChildren(); // simple component, no children

        assertThat(breakdown)
                .child("netto")
                .child("energy-net")
                .child("energy-grid-component")
                .hasTotal(Money.pln(new BigDecimal("1.80")));

        // Verify CPO markup
        assertThat(breakdown)
                .child("netto")
                .child("cpo-markup")
                .hasTotal(Money.pln(new BigDecimal("8.50")))
                .hasChildrenCount(3);

        // Verify individual CPO components
        assertThat(breakdown)
                .child("netto")
                .child("cpo-markup")
                .child("cpo-session-component")
                .hasTotal(Money.pln(new BigDecimal("1.50")))
                .hasNoChildren();

        assertThat(breakdown)
                .child("netto")
                .child("cpo-markup")
                .child("cpo-kwh-component")
                .hasTotal(Money.pln(new BigDecimal("3.00")))
                .hasNoChildren();

        assertThat(breakdown)
                .child("netto")
                .child("cpo-markup")
                .child("cpo-time-component")
                .hasTotal(Money.pln(new BigDecimal("4.00")))
                .hasNoChildren();

        // Verify EMSP markup
        assertThat(breakdown)
                .child("netto")
                .child("emsp-markup")
                .hasTotal(Money.pln(new BigDecimal("3.20")))
                .hasChildrenCount(2);

        // Verify individual EMSP components
        assertThat(breakdown)
                .child("netto")
                .child("emsp-markup")
                .child("emsp-kwh-component")
                .hasTotal(Money.pln(new BigDecimal("1.20")))
                .hasNoChildren();

        assertThat(breakdown)
                .child("netto")
                .child("emsp-markup")
                .child("emsp-time-component")
                .hasTotal(Money.pln(new BigDecimal("2.00")))
                .hasNoChildren();

        // Verify VAT
        assertThat(breakdown).child("vat-component").hasTotal(Money.pln(new BigDecimal("4.97")));
    }

    @Test
    void shouldShowFormattedBreakdownForSession() {
        // given: complete component structure (same as previous test)
        facade.createSimpleComponent("energy-wholesale-component", "energy-wholesale");
        facade.createSimpleComponent("energy-grid-component", "energy-grid");
        facade.createCompositeComponent(
                "energy-net", Map.of(), "energy-wholesale-component", "energy-grid-component");

        facade.createSimpleComponent("cpo-session-component", "cpo-session-fee");
        facade.createSimpleComponent("cpo-kwh-component", "cpo-per-kwh");
        facade.createSimpleComponent(
                "cpo-time-component", "cpo-per-minute", Map.of("time", "quantity"));
        facade.createCompositeComponent(
                "cpo-markup",
                Map.of(),
                "cpo-session-component",
                "cpo-kwh-component",
                "cpo-time-component");

        facade.createSimpleComponent("emsp-kwh-component", "emsp-per-kwh");
        facade.createSimpleComponent(
                "emsp-time-component", "emsp-per-minute", Map.of("time", "quantity"));
        facade.createCompositeComponent(
                "emsp-markup", Map.of(), "emsp-kwh-component", "emsp-time-component");

        facade.createCompositeComponent(
                "netto", Map.of(), "energy-net", "cpo-markup", "emsp-markup");

        facade.createSimpleComponent("vat-component", "vat-rate");
        facade.createCompositeComponent(
                "total-session-cost",
                Map.of("vat-component", Map.of("baseAmount", new ValueOf("netto"))),
                "netto",
                "vat-component");

        // when: calculate with formatting
        Parameters sessionParams =
                Parameters.of(
                        "quantity", BigDecimal.valueOf(12),
                        "time", BigDecimal.valueOf(40));

        ComponentBreakdown breakdown =
                facade.calculateComponentBreakdown("total-session-cost", sessionParams);

        // then: print formatted breakdown
        System.out.println("=".repeat(60));
        System.out.println("E-MOBILITY CHARGING SESSION BREAKDOWN");
        System.out.println("Session: 12 kWh, 40 minutes");
        System.out.println("=".repeat(60));
        System.out.println(breakdown.format());
        System.out.println("=".repeat(60));

        assertEquals(Money.pln(new BigDecimal("26.57")), breakdown.total());
    }
}
