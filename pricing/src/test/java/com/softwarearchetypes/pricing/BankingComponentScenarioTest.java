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
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Banking pricing scenarios using Component semantic composition.
 *
 * <p>Scenarios: - Loan with insurance depending on principal - Account fees with tiered structure -
 * Portfolio management with performance-based fees
 */
class BankingComponentScenarioTest {

    static final Instant NOW =
            LocalDateTime.of(2025, 1, 15, 12, 50).atZone(ZoneId.systemDefault()).toInstant();
    static final Clock clock = fixed(NOW, ZoneId.systemDefault());
    private final PricingFacade facade = PricingConfiguration.inMemory(clock).pricingFacade();

    @BeforeEach
    void setUp() {
        // Loan calculators
        facade.addCalculator(
                "loan-interest",
                CalculatorType.SIMPLE_INTEREST,
                Parameters.of("annualRate", BigDecimal.valueOf(5.5)));

        facade.addCalculator(
                "insurance-rate",
                CalculatorType.PERCENTAGE,
                Parameters.of("percentageRate", BigDecimal.valueOf(2)));

        facade.addCalculator(
                "processing-fee",
                CalculatorType.SIMPLE_FIXED,
                Parameters.of("amount", Money.pln(BigDecimal.valueOf(500))));

        // Account fees
        facade.addCalculator(
                "monthly-account-fee",
                CalculatorType.SIMPLE_FIXED,
                Parameters.of("amount", Money.pln(BigDecimal.valueOf(15))));

        facade.addCalculator(
                "transaction-fee",
                CalculatorType.STEP_FUNCTION,
                Parameters.of(
                        "basePrice", Money.pln(BigDecimal.ZERO),
                        "stepSize", BigDecimal.ONE,
                        "stepIncrement", BigDecimal.valueOf(0.50)));

        // Portfolio management
        facade.addCalculator(
                "management-fee",
                CalculatorType.PERCENTAGE,
                Parameters.of("percentageRate", BigDecimal.valueOf(1.5)));

        facade.addCalculator(
                "performance-fee",
                CalculatorType.PERCENTAGE,
                Parameters.of("percentageRate", BigDecimal.valueOf(20)));
    }

    @Test
    void shouldCalculateLoanCostWithInsuranceDependency() {
        // given: loan components
        facade.createSimpleComponent("principal-interest", "loan-interest");
        facade.createSimpleComponent("loan-insurance", "insurance-rate");
        facade.createSimpleComponent("processing", "processing-fee");

        // insurance depends on principal + interest
        facade.createCompositeComponent("loan-base", Map.of(), "principal-interest", "processing");

        facade.createCompositeComponent(
                "total-loan-cost",
                Map.of("loan-insurance", Map.of("baseAmount", new ValueOf("loan-base"))),
                "loan-base",
                "loan-insurance");

        // when: loan of 100,000 PLN for 1 year
        Parameters loanParams =
                Parameters.of(
                        "base", Money.pln(BigDecimal.valueOf(100000)), "unit", ChronoUnit.YEARS);

        Money result = facade.calculateComponent("total-loan-cost", loanParams);

        // then:
        // interest = 5500 PLN (5.5% of 100k)
        // processing = 500 PLN
        // loan base = 6000 PLN
        // insurance = 120 PLN (2% of 6000)
        // total = 6120 PLN
        Money expectedBase = Money.pln(BigDecimal.valueOf(6000));
        Money expectedInsurance = Money.pln(BigDecimal.valueOf(120));
        Money expectedTotal = Money.pln(BigDecimal.valueOf(6120));

        assertEquals(expectedTotal, result);

        ComponentBreakdown breakdown =
                facade.calculateComponentBreakdown("total-loan-cost", loanParams);
        assertThat(breakdown)
                .hasName("total-loan-cost")
                .hasTotal(expectedTotal)
                .hasChildrenCount(2);

        assertThat(breakdown).child("loan-base").hasTotal(expectedBase).hasChildrenCount(2);

        assertThat(breakdown).child("loan-insurance").hasTotal(expectedInsurance).hasNoChildren();
    }

    @Test
    void shouldCalculateAccountFeesWithTransactions() {
        // given: account fee components
        facade.createSimpleComponent("monthly-fee", "monthly-account-fee");
        facade.createSimpleComponent("transaction-fees", "transaction-fee");

        facade.createCompositeComponent(
                "total-account-fees", Map.of(), "monthly-fee", "transaction-fees");

        // when: customer made 50 transactions above free limit
        Parameters accountParams = Parameters.of("quantity", BigDecimal.valueOf(50));
        Money result = facade.calculateComponent("total-account-fees", accountParams);

        // then: 15 PLN monthly + 25 PLN transactions (50 * 0.50)
        Money expected = Money.pln(BigDecimal.valueOf(40));
        assertEquals(expected, result);

        ComponentBreakdown breakdown =
                facade.calculateComponentBreakdown("total-account-fees", accountParams);
        assertThat(breakdown).hasName("total-account-fees").hasTotal(expected).hasChildrenCount(2);
    }

    @Test
    void shouldCalculatePortfolioFeesWithPerformanceBonus() {
        // given: portfolio management components
        facade.createSimpleComponent("base-management", "management-fee");
        facade.createSimpleComponent("performance-bonus", "performance-fee");

        // base management fee on portfolio value
        // performance fee on gains above benchmark
        facade.createCompositeComponent(
                "total-management-fees",
                Map.of("performance-bonus", Map.of("baseAmount", new ValueOf("base-management"))),
                "base-management",
                "performance-bonus");

        // when: portfolio worth 1,000,000 PLN
        Parameters portfolioParams =
                Parameters.of("baseAmount", Money.pln(BigDecimal.valueOf(1000000)));

        Money result = facade.calculateComponent("total-management-fees", portfolioParams);

        // then:
        // base management = 15,000 PLN (1.5% of 1M)
        // performance bonus = 3,000 PLN (20% of 15k base fee)
        // total = 18,000 PLN
        Money expectedBase = Money.pln(BigDecimal.valueOf(15000));
        Money expectedPerformance = Money.pln(BigDecimal.valueOf(3000));
        Money expectedTotal = Money.pln(BigDecimal.valueOf(18000));

        assertEquals(expectedTotal, result);

        ComponentBreakdown breakdown =
                facade.calculateComponentBreakdown("total-management-fees", portfolioParams);
        assertThat(breakdown)
                .hasName("total-management-fees")
                .hasTotal(expectedTotal)
                .hasChildrenCount(2)
                .child("base-management")
                .hasTotal(expectedBase);

        assertThat(breakdown).child("performance-bonus").hasTotal(expectedPerformance);
    }

    @Test
    void shouldCalculateComplexLoanWithMultipleDependencies() {
        // given: complex loan structure
        facade.createSimpleComponent("interest", "loan-interest");
        facade.createSimpleComponent("processing", "processing-fee");
        facade.createSimpleComponent("insurance", "insurance-rate");

        // insurance depends on sum of interest + processing
        facade.createCompositeComponent(
                "financing-costs",
                Map.of("insurance", Map.of("baseAmount", new SumOf("interest", "processing"))),
                "interest",
                "processing",
                "insurance");

        // when: loan parameters
        Parameters loanParams =
                Parameters.of(
                        "base", Money.pln(BigDecimal.valueOf(200000)), "unit", ChronoUnit.YEARS);

        Money result = facade.calculateComponent("financing-costs", loanParams);

        // then:
        // interest = 11,000 PLN (5.5% of 200k)
        // processing = 500 PLN
        // insurance = 230 PLN (2% of 11,500)
        // total = 11,730 PLN
        Money expectedInterest = Money.pln(BigDecimal.valueOf(11000));
        Money expectedProcessing = Money.pln(BigDecimal.valueOf(500));
        Money expectedInsurance = Money.pln(BigDecimal.valueOf(230));
        Money expectedTotal = Money.pln(BigDecimal.valueOf(11730));

        assertEquals(expectedTotal, result);

        ComponentBreakdown breakdown =
                facade.calculateComponentBreakdown("financing-costs", loanParams);
        assertThat(breakdown)
                .hasName("financing-costs")
                .hasTotal(expectedTotal)
                .hasChildrenCount(3);

        assertThat(breakdown).child("interest").hasTotal(expectedInterest);

        assertThat(breakdown).child("processing").hasTotal(expectedProcessing);

        assertThat(breakdown).child("insurance").hasTotal(expectedInsurance);
    }
}
