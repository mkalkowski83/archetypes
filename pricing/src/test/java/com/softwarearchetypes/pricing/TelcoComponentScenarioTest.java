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
 * Telecom billing scenario using Component semantic composition.
 *
 * <p>Scenario: Monthly mobile subscription bill with: - Base fee: 45 PLN = 25 PLN network
 * maintenance + 20 PLN commission - Data overage: linear charging above 5GB data limit - Roaming
 * overage: 1.5 PLN per minute above 30 minutes limit
 */
class TelcoComponentScenarioTest {

    static final Instant NOW =
            LocalDateTime.of(2025, 1, 15, 12, 50).atZone(ZoneId.systemDefault()).toInstant();
    static final Clock clock = fixed(NOW, ZoneId.systemDefault());
    private PricingFacade facade = PricingConfiguration.inMemory(clock).pricingFacade();

    @BeforeEach
    void setUp() {
        // Setup calculators (price lists) with specific values
        facade.addCalculator(
                "network-maintenance",
                CalculatorType.SIMPLE_FIXED,
                Parameters.of("amount", Money.pln(BigDecimal.valueOf(25))));

        facade.addCalculator(
                "commission",
                CalculatorType.SIMPLE_FIXED,
                Parameters.of("amount", Money.pln(BigDecimal.valueOf(20))));

        facade.addCalculator(
                "data-overage",
                CalculatorType.STEP_FUNCTION,
                Parameters.of(
                        "basePrice", Money.pln(BigDecimal.ZERO),
                        "stepSize", BigDecimal.ONE,
                        "stepIncrement", BigDecimal.valueOf(2)));

        facade.addCalculator(
                "roaming-overage",
                CalculatorType.STEP_FUNCTION,
                Parameters.of(
                        "basePrice", Money.pln(BigDecimal.ZERO),
                        "stepSize", BigDecimal.ONE,
                        "stepIncrement", BigDecimal.valueOf(1.5)));

        facade.addCalculator(
                "percentage-rate",
                CalculatorType.PERCENTAGE,
                Parameters.of("percentageRate", BigDecimal.valueOf(23)));
    }

    @Test
    void shouldCalculateMonthlyBillWithBaseFeesOnly() {
        // given: components for base fee breakdown
        facade.createSimpleComponent("network-maintenance-component", "network-maintenance");
        facade.createSimpleComponent("commission-component", "commission");

        facade.createCompositeComponent(
                "base-fee", Map.of(), "network-maintenance-component", "commission-component");

        // when: calculate base fee
        Money result = facade.calculateComponent("base-fee", Parameters.empty());

        // then: base fee should be 45 PLN (25 + 20)
        assertEquals(Money.pln(BigDecimal.valueOf(45)), result);

        ComponentBreakdown breakdown =
                facade.calculateComponentBreakdown("base-fee", Parameters.empty());
        assertThat(breakdown)
                .hasName("base-fee")
                .hasTotal(Money.pln(BigDecimal.valueOf(45)))
                .hasChildrenCount(2);
    }

    @Test
    void shouldCalculateMonthlyBillWithDataOverage() {
        // given: components
        facade.createSimpleComponent("network-maintenance-component", "network-maintenance");
        facade.createSimpleComponent("commission-component", "commission");
        facade.createSimpleComponent("data-overage-component", "data-overage");

        facade.createCompositeComponent(
                "base-fee", Map.of(), "network-maintenance-component", "commission-component");

        facade.createCompositeComponent(
                "monthly-bill", Map.of(), "base-fee", "data-overage-component");

        // when: customer used 8 GB (3 GB above 5GB limit)
        Parameters usageParams = Parameters.of("quantity", BigDecimal.valueOf(3));
        Money result = facade.calculateComponent("monthly-bill", usageParams);

        // then: 45 PLN base + 6 PLN data overage (3 GB * 2 PLN)
        assertEquals(Money.pln(BigDecimal.valueOf(51)), result);

        ComponentBreakdown breakdown =
                facade.calculateComponentBreakdown("monthly-bill", usageParams);
        assertThat(breakdown)
                .hasName("monthly-bill")
                .hasTotal(Money.pln(BigDecimal.valueOf(51)))
                .hasChildrenCount(2);

        assertThat(breakdown)
                .child("base-fee")
                .hasTotal(Money.pln(BigDecimal.valueOf(45)))
                .hasChildrenCount(2);

        assertThat(breakdown)
                .child("data-overage-component")
                .hasTotal(Money.pln(BigDecimal.valueOf(6)))
                .hasNoChildren();
    }

    @Test
    void shouldCalculateMonthlyBillWithRoamingOverage() {
        // given: components
        facade.createSimpleComponent("network-maintenance-component", "network-maintenance");
        facade.createSimpleComponent("commission-component", "commission");
        facade.createSimpleComponent("roaming-overage-component", "roaming-overage");

        facade.createCompositeComponent(
                "base-fee", Map.of(), "network-maintenance-component", "commission-component");

        facade.createCompositeComponent(
                "monthly-bill", Map.of(), "base-fee", "roaming-overage-component");

        // when: customer used 50 roaming minutes (20 minutes above 30-minute limit)
        Parameters usageParams = Parameters.of("quantity", BigDecimal.valueOf(20));
        Money result = facade.calculateComponent("monthly-bill", usageParams);

        // then: 45 PLN base + 30 PLN roaming overage (20 min * 1.5 PLN)
        assertEquals(Money.pln(BigDecimal.valueOf(75)), result);
    }

    @Test
    void shouldCalculateBillWithVATDependingOnNetAmount() {
        // given: components for net amount
        facade.createSimpleComponent("network-maintenance-component", "network-maintenance");
        facade.createSimpleComponent("commission-component", "commission");
        facade.createSimpleComponent("data-overage-component", "data-overage");

        facade.createCompositeComponent(
                "base-fee", Map.of(), "network-maintenance-component", "commission-component");

        facade.createCompositeComponent(
                "net-amount", Map.of(), "base-fee", "data-overage-component");

        // and: VAT component that depends on net amount
        facade.createSimpleComponent("vat-component", "percentage-rate");

        // and: total bill with VAT depending on net amount
        facade.createCompositeComponent(
                "total-bill",
                Map.of("vat-component", Map.of("baseAmount", new ValueOf("net-amount"))),
                "net-amount",
                "vat-component");

        // when: customer used 3 GB overage
        Parameters usageParams = Parameters.of("quantity", BigDecimal.valueOf(3));
        Money result = facade.calculateComponent("total-bill", usageParams);

        // then: net amount = 51 PLN (45 base + 6 overage)
        //       VAT = 11.73 PLN (23% of 51)
        //       total = 62.73 PLN
        Money expectedNet = Money.pln(BigDecimal.valueOf(51));
        Money expectedVAT = Money.pln(new BigDecimal("11.73"));
        Money expectedTotal = Money.pln(new BigDecimal("62.73"));

        assertEquals(expectedTotal, result);

        // and: breakdown shows dependency
        ComponentBreakdown breakdown =
                facade.calculateComponentBreakdown("total-bill", usageParams);
        assertThat(breakdown).hasName("total-bill").hasTotal(expectedTotal).hasChildrenCount(2);

        assertThat(breakdown).child("net-amount").hasTotal(expectedNet).hasChildrenCount(2);

        assertThat(breakdown).child("vat-component").hasTotal(expectedVAT).hasNoChildren();
    }

    @Test
    void shouldShowDetailedBreakdownHierarchy() {
        // given: complete setup
        facade.createSimpleComponent("network-maintenance-component", "network-maintenance");
        facade.createSimpleComponent("commission-component", "commission");
        facade.createSimpleComponent("data-overage-component", "data-overage");

        facade.createCompositeComponent(
                "base-fee", Map.of(), "network-maintenance-component", "commission-component");

        facade.createCompositeComponent(
                "monthly-bill", Map.of(), "base-fee", "data-overage-component");

        // when: get detailed breakdown for 3 GB overage
        Parameters usageParams = Parameters.of("quantity", BigDecimal.valueOf(3));
        ComponentBreakdown breakdown =
                facade.calculateComponentBreakdown("monthly-bill", usageParams);

        // then: verify complete hierarchy with fluent assertions
        assertThat(breakdown)
                .hasName("monthly-bill")
                .hasTotal(Money.pln(BigDecimal.valueOf(51)))
                .hasChildrenCount(2);

        assertThat(breakdown)
                .child("base-fee")
                .hasName("base-fee")
                .hasTotal(Money.pln(BigDecimal.valueOf(45)))
                .hasChildrenCount(2)
                .child("network-maintenance-component")
                .hasTotal(Money.pln(BigDecimal.valueOf(25)))
                .hasNoChildren();

        assertThat(breakdown)
                .child("base-fee")
                .child("commission-component")
                .hasTotal(Money.pln(BigDecimal.valueOf(20)))
                .hasNoChildren();

        assertThat(breakdown)
                .child("data-overage-component")
                .hasTotal(Money.pln(BigDecimal.valueOf(6)))
                .hasNoChildren();
    }
}
