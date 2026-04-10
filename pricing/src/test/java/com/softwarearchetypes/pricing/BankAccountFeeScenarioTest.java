package com.softwarearchetypes.pricing;

import static java.time.Clock.fixed;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.softwarearchetypes.quantity.money.Money;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Scenario test: Bank account monthly fee based on income tiers
 *
 * <p>Fee structure: - Low activity (< 1000 PLN): 20 PLN fee - Medium activity (1000-4000 PLN): 10
 * PLN fee - High activity (> 4000 PLN): 0 PLN fee (free)
 */
class BankAccountFeeScenarioTest {

    static final Instant NOW =
            LocalDateTime.of(2025, 1, 15, 12, 50).atZone(ZoneId.systemDefault()).toInstant();
    static final Clock clock = fixed(NOW, ZoneId.systemDefault());
    private final PricingFacade facade = PricingConfiguration.inMemory(clock).pricingFacade();
    private CompositeFunctionCalculator accountFeeCalculator;

    @BeforeEach
    void setUp() {
        // Create calculators for each tier
        Calculator feeTier1 =
                facade.addCalculator(
                        "acc-fee-tier-1",
                        CalculatorType.SIMPLE_FIXED,
                        new Parameters(Map.of("amount", Money.pln(new BigDecimal("20.00")))));

        Calculator feeTier2 =
                facade.addCalculator(
                        "acc-fee-tier-2",
                        CalculatorType.SIMPLE_FIXED,
                        new Parameters(Map.of("amount", Money.pln(new BigDecimal("10.00")))));

        Calculator feeTier3 =
                facade.addCalculator(
                        "acc-fee-tier-3",
                        CalculatorType.SIMPLE_FIXED,
                        new Parameters(Map.of("amount", Money.pln(new BigDecimal("0.00")))));

        // Define income ranges
        List<CalculatorRange> ranges =
                List.of(
                        new NumericRange(BigDecimal.ZERO, new BigDecimal("1000"), feeTier1.getId()),
                        new NumericRange(
                                new BigDecimal("1000"), new BigDecimal("4000"), feeTier2.getId()),
                        new NumericRange(
                                new BigDecimal("4000"),
                                new BigDecimal(Integer.MAX_VALUE),
                                feeTier3.getId()));

        // Create composite calculator
        accountFeeCalculator =
                (CompositeFunctionCalculator)
                        facade.addCalculator(
                                "account-fee",
                                CalculatorType.COMPOSITE,
                                new Parameters(
                                        Map.of(
                                                "rangeSelector",
                                                "monthlyIncome",
                                                "ranges",
                                                ranges)));
    }

    @Test
    void shouldCharge20PlnForVeryLowIncome() {
        // given - income of 0 PLN (inactive account)
        Parameters params = new Parameters(Map.of("monthlyIncome", BigDecimal.ZERO));

        // when
        Money fee = facade.calculate("account-fee", params);

        // then - full fee
        assertEquals(0, new BigDecimal("20.00").compareTo(fee.value()));
    }

    @Test
    void shouldCharge20PlnForLowIncome() {
        // given - income of 500 PLN (low activity)
        Parameters params = new Parameters(Map.of("monthlyIncome", new BigDecimal("500")));

        // when
        Money fee = facade.calculate("account-fee", params);

        // then - full fee
        assertEquals(0, new BigDecimal("20.00").compareTo(fee.value()));
    }

    @Test
    void shouldCharge20PlnForIncomeJustBelowThreshold() {
        // given - income of 999.99 PLN (just below medium tier)
        Parameters params = new Parameters(Map.of("monthlyIncome", new BigDecimal("999.99")));

        // when
        Money fee = facade.calculate("account-fee", params);

        // then - still full fee
        assertEquals(0, new BigDecimal("20.00").compareTo(fee.value()));
    }

    @Test
    void shouldCharge10PlnForIncomeAtLowerBoundary() {
        // given - income exactly 1000 PLN (medium tier starts)
        Parameters params = new Parameters(Map.of("monthlyIncome", new BigDecimal("1000")));

        // when
        Money fee = facade.calculate("account-fee", params);

        // then - reduced fee
        assertEquals(0, new BigDecimal("10.00").compareTo(fee.value()));
    }

    @Test
    void shouldCharge10PlnForMediumIncome() {
        // given - income of 2500 PLN (medium activity)
        Parameters params = new Parameters(Map.of("monthlyIncome", new BigDecimal("2500")));

        // when
        Money fee = facade.calculate("account-fee", params);

        // then - reduced fee
        assertEquals(0, new BigDecimal("10.00").compareTo(fee.value()));
    }

    @Test
    void shouldCharge10PlnForIncomeJustBelowHighTier() {
        // given - income of 3999.99 PLN (just below high tier)
        Parameters params = new Parameters(Map.of("monthlyIncome", new BigDecimal("3999.99")));

        // when
        Money fee = facade.calculate("account-fee", params);

        // then - still reduced fee
        assertEquals(0, new BigDecimal("10.00").compareTo(fee.value()));
    }

    @Test
    void shouldChargeNothingForIncomeAtHighTierBoundary() {
        // given - income exactly 4000 PLN (high tier starts)
        Parameters params = new Parameters(Map.of("monthlyIncome", new BigDecimal("4000")));

        // when
        Money fee = facade.calculate("account-fee", params);

        // then - no fee
        assertEquals(0, BigDecimal.ZERO.compareTo(fee.value()));
    }

    @Test
    void shouldChargeNothingForHighIncome() {
        // given - income of 5000 PLN (high activity)
        Parameters params = new Parameters(Map.of("monthlyIncome", new BigDecimal("5000")));

        // when
        Money fee = facade.calculate("account-fee", params);

        // then - no fee
        assertEquals(0, BigDecimal.ZERO.compareTo(fee.value()));
    }

    @Test
    void shouldChargeNothingForVeryHighIncome() {
        // given - income of 50000 PLN (very active account)
        Parameters params = new Parameters(Map.of("monthlyIncome", new BigDecimal("50000")));

        // when
        Money fee = facade.calculate("account-fee", params);

        // then - no fee
        assertEquals(0, BigDecimal.ZERO.compareTo(fee.value()));
    }

    @Test
    void shouldVerifyCalculatorType() {
        // when & then
        assertEquals(CalculatorType.COMPOSITE, accountFeeCalculator.getType());
    }

    @Test
    void shouldProvideCompositeFunctionFormula() {
        // when
        String formula = accountFeeCalculator.formula();

        // then - should show piecewise function with all three ranges
        String expected =
                "f(x) = piecewise function:\n"
                        + "  [0, 1000) → acc-fee-tier-1: f(x) = PLN 20\n"
                        + "  [1000, 4000) → acc-fee-tier-2: f(x) = PLN 10\n"
                        + "  [4000, 2147483647) → acc-fee-tier-3: f(x) = PLN 0";
        assertEquals(expected, formula);
    }
}
