package com.softwarearchetypes.quantity.money;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void shouldCreateMoneyFromIntegerAmount() {
        // given
        int amount = 100;

        // when
        Money money = Money.pln(amount);

        // then
        assertEquals(new BigDecimal("100"), money.value());
    }

    @Test
    void shouldCreateMoneyFromBigDecimalAmount() {
        // given
        BigDecimal amount = new BigDecimal("99.99");

        // when
        Money money = Money.pln(amount);

        // then
        assertEquals(amount, money.value());
    }

    @Test
    void shouldCreateMoneyFromNumberAmount() {
        // given
        Number amount = 50.5;

        // when
        Money money = Money.pln(amount);

        // then
        assertEquals(new BigDecimal("50.5"), money.value());
    }

    @Test
    void shouldCreateZeroPlnMoney() {
        // when
        Money money = Money.zeroPln();

        // then
        assertEquals(BigDecimal.ZERO, money.value());
        assertTrue(money.isZero());
    }

    @Test
    void shouldCreateOnePlnMoney() {
        // when
        Money money = Money.onePln();

        // then
        assertEquals(BigDecimal.ONE, money.value());
    }

    @Test
    void shouldAddTwoMoneyAmounts() {
        // given
        Money first = Money.pln(100);
        Money second = Money.pln(50);

        // when
        Money result = first.add(second);

        // then
        assertEquals(Money.pln(150), result);
    }

    @Test
    void shouldSubtractTwoMoneyAmounts() {
        // given
        Money first = Money.pln(100);
        Money second = Money.pln(30);

        // when
        Money result = first.subtract(second);

        // then
        assertEquals(Money.pln(70), result);
    }

    @Test
    void shouldNegateMoneyAmount() {
        // given
        Money money = Money.pln(50);

        // when
        Money result = money.negate();

        // then
        assertEquals(Money.pln(-50), result);
        assertTrue(result.isNegative());
    }

    @Test
    void shouldReturnAbsoluteValueOfNegativeMoney() {
        // given
        Money negativeMoney = Money.pln(-100);

        // when
        Money result = negativeMoney.abs();

        // then
        assertEquals(new BigDecimal("100"), result.value());
        assertFalse(result.isNegative());
    }

    @Test
    void shouldReturnAbsoluteValueUsingStaticMethod() {
        // given
        Money negativeMoney = Money.pln(-75);

        // when
        Money result = Money.abs(negativeMoney);

        // then
        assertEquals(new BigDecimal("75"), result.value());
    }

    @Test
    void shouldDivideAndReturnQuotientAndRemainder() {
        // given
        Money money = Money.pln(100);
        BigDecimal divisor = new BigDecimal("3");

        // when
        Money[] result = money.divideAndRemainder(divisor);

        // then
        assertEquals(2, result.length);
        assertEquals(new BigDecimal("33"), result[0].value());
        assertEquals(BigDecimal.ONE, result[1].value());
    }

    @Test
    void shouldReturnTrueWhenMoneyIsZero() {
        // given
        Money money = Money.zeroPln();

        // when & then
        assertTrue(money.isZero());
    }

    @Test
    void shouldReturnFalseWhenMoneyIsNotZero() {
        // given
        Money money = Money.pln(1);

        // when & then
        assertFalse(money.isZero());
    }

    @Test
    void shouldReturnTrueWhenMoneyIsNegative() {
        // given
        Money money = Money.pln(-10);

        // when & then
        assertTrue(money.isNegative());
    }

    @Test
    void shouldReturnFalseWhenMoneyIsPositive() {
        // given
        Money money = Money.pln(10);

        // when & then
        assertFalse(money.isNegative());
    }

    @Test
    void shouldReturnTrueWhenFirstMoneyIsGreaterThanSecond() {
        // given
        Money greater = Money.pln(100);
        Money lesser = Money.pln(50);

        // when & then
        assertTrue(greater.isGreaterThan(lesser));
    }

    @Test
    void shouldReturnFalseWhenFirstMoneyIsNotGreaterThanSecond() {
        // given
        Money lesser = Money.pln(50);
        Money greater = Money.pln(100);

        // when & then
        assertFalse(lesser.isGreaterThan(greater));
    }

    @Test
    void shouldReturnTrueWhenFirstMoneyIsGreaterThanOrEqualToSecond() {
        // given
        Money first = Money.pln(100);
        Money second = Money.pln(100);

        // when & then
        assertTrue(first.isGreaterThanOrEqualTo(second));
    }

    @Test
    void shouldReturnTrueWhenFirstMoneyIsGreaterInGreaterThanOrEqualComparison() {
        // given
        Money greater = Money.pln(150);
        Money lesser = Money.pln(100);

        // when & then
        assertTrue(greater.isGreaterThanOrEqualTo(lesser));
    }

    @Test
    void shouldReturnMinimumOfTwoMoneyAmounts() {
        // given
        Money first = Money.pln(100);
        Money second = Money.pln(50);

        // when
        Money result = Money.min(first, second);

        // then
        assertEquals(second, result);
    }

    @Test
    void shouldReturnMinimumFromSetOfMoneyAmounts() {
        // given
        Set<Money> amounts = Set.of(Money.pln(100), Money.pln(25), Money.pln(50), Money.pln(75));

        // when
        Optional<Money> result = Money.min(amounts);

        // then
        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("25"), result.get().value());
    }

    @Test
    void shouldReturnEmptyOptionalWhenMinCalledOnEmptySet() {
        // given
        Set<Money> emptySet = Set.of();

        // when
        Optional<Money> result = Money.min(emptySet);

        // then
        assertFalse(result.isPresent());
    }

    @Test
    void shouldReturnMaximumOfTwoMoneyAmounts() {
        // given
        Money first = Money.pln(100);
        Money second = Money.pln(50);

        // when
        Money result = Money.max(first, second);

        // then
        assertEquals(first, result);
    }

    @Test
    void shouldCompareMoneyAmountsCorrectly() {
        // given
        Money smaller = Money.pln(50);
        Money larger = Money.pln(100);
        Money equal = Money.pln(50);

        // when & then
        assertTrue(smaller.compareTo(larger) < 0);
        assertTrue(larger.compareTo(smaller) > 0);
        assertEquals(0, smaller.compareTo(equal));
    }

    @Test
    void shouldBeEqualWhenMoneyHasSameAmountAndCurrency() {
        // given
        Money first = Money.pln(100);
        Money second = Money.pln(100);

        // when & then
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenMoneyHasDifferentAmount() {
        // given
        Money first = Money.pln(100);
        Money second = Money.pln(50);

        // when & then
        assertNotEquals(first, second);
    }

    @Test
    void shouldNotBeEqualToNull() {
        // given
        Money money = Money.pln(100);

        // when & then
        assertNotEquals(null, money);
    }

    @Test
    void shouldBeEqualToItself() {
        // given
        Money money = Money.pln(100);

        // when & then
        assertEquals(money, money);
    }

    @Test
    void shouldHaveProperStringRepresentation() {
        // given
        Money money = Money.pln(new BigDecimal("123.45"));

        // when
        String result = money.toString();

        // then
        assertEquals("PLN 123.45", result);
    }

    @Test
    void shouldReturnCorrectValueAsBigDecimal() {
        // given
        BigDecimal expectedValue = new BigDecimal("99.99");
        Money money = Money.pln(expectedValue);

        // when
        BigDecimal result = money.value();

        // then
        assertEquals(expectedValue, result);
    }

    @Test
    void shouldHandleZeroInArithmeticOperations() {
        // given
        Money money = Money.pln(100);
        Money zero = Money.zeroPln();

        // when
        Money addResult = money.add(zero);
        Money subtractResult = money.subtract(zero);

        // then
        assertEquals(money, addResult);
        assertEquals(money, subtractResult);
    }

    @Test
    void shouldHandleNegativeAmountsInComparisons() {
        // given
        Money negative = Money.pln(-50);
        Money positive = Money.pln(50);

        // when & then
        assertTrue(negative.isNegative());
        assertFalse(positive.isNegative());
        assertTrue(positive.isGreaterThan(negative));
        assertFalse(negative.isGreaterThan(positive));
    }

    @Test
    void shouldMultiplyMoneyByPercentage() {
        // given
        Money money = Money.pln(1000);
        Percentage percentage = Percentage.of(20); // 20%

        // when
        Money result = money.multiply(percentage);

        // then - 1000 * 20% = 200
        assertEquals(0, new BigDecimal("200.00").compareTo(result.value()));
    }

    @Test
    void shouldMultiplyMoneyByFiftyPercentage() {
        // given
        Money money = Money.pln(200);
        Percentage percentage = Percentage.of(50); // 50%

        // when
        Money result = money.multiply(percentage);

        // then - 200 * 50% = 100
        assertEquals(0, new BigDecimal("100.00").compareTo(result.value()));
    }

    @Test
    void shouldMultiplyMoneyByOneHundredPercentage() {
        // given
        Money money = Money.pln(150);
        Percentage percentage = Percentage.oneHundred(); // 100%

        // when
        Money result = money.multiply(percentage);

        // then - 150 * 100% = 150
        assertEquals(0, new BigDecimal("150.00").compareTo(result.value()));
    }

    @Test
    void shouldMultiplyMoneyByZeroPercentage() {
        // given
        Money money = Money.pln(500);
        Percentage percentage = Percentage.zero(); // 0%

        // when
        Money result = money.multiply(percentage);

        // then - 500 * 0% = 0
        assertEquals(0, BigDecimal.ZERO.compareTo(result.value()));
    }

    @Test
    void shouldMultiplyMoneyByDecimalPercentage() {
        // given
        Money money = Money.pln(1000);
        Percentage percentage = Percentage.of(new BigDecimal("12.5")); // 12.5%

        // when
        Money result = money.multiply(percentage);

        // then - 1000 * 12.5% = 125
        assertEquals(0, new BigDecimal("125.00").compareTo(result.value()));
    }

    @Test
    void shouldMultiplyMoneyByVerySmallPercentage() {
        // given
        Money money = Money.pln(10000);
        Percentage percentage = Percentage.of(new BigDecimal("0.5")); // 0.5%

        // when
        Money result = money.multiply(percentage);

        // then - 10000 * 0.5% = 50
        assertEquals(0, new BigDecimal("50.00").compareTo(result.value()));
    }

    @Test
    void shouldMultiplyMoneyByPercentageGreaterThanHundred() {
        // given
        Money money = Money.pln(100);
        Percentage percentage = Percentage.of(150); // 150%

        // when
        Money result = money.multiply(percentage);

        // then - 100 * 150% = 150
        assertEquals(0, new BigDecimal("150.00").compareTo(result.value()));
    }

    @Test
    void shouldRoundResultToTwoDecimalPlaces() {
        // given
        Money money = Money.pln(100);
        Percentage percentage = Percentage.of(new BigDecimal("33.33")); // 33.33%

        // when
        Money result = money.multiply(percentage);

        // then - 100 * 33.33% = 33.33 rounded to 33.33
        assertEquals(0, new BigDecimal("33.33").compareTo(result.value()));
    }

    @Test
    void shouldMultiplyEurMoneyByPercentage() {
        // given
        Money money = Money.eur(1000);
        Percentage percentage = Percentage.of(25); // 25%

        // when
        Money result = money.multiply(percentage);

        // then - 1000 EUR * 25% = 250 EUR
        assertEquals(0, new BigDecimal("250.00").compareTo(result.value()));
        // NOTE: Currently this will fail because multiply(Percentage) hardcodes PLN!
        // This is a BUG that needs to be fixed
        assertTrue(
                result.toString().contains("EUR"),
                "Currency should be preserved as EUR, not hardcoded to PLN");
    }

    @Test
    void shouldDivideMoneyWithDefaultRounding() {
        // given
        Money money = Money.pln(100);
        BigDecimal divisor = new BigDecimal("3");

        // when
        Money result = money.divide(divisor);

        // then - 100 / 3 = 33.33... (with default rounding)
        assertEquals("PLN", result.currency());
        // Result should be approximately 33.33
        assertTrue(result.value().compareTo(new BigDecimal("33")) > 0);
        assertTrue(result.value().compareTo(new BigDecimal("34")) < 0);
    }

    @Test
    void shouldDivideMoneyWithSpecifiedRounding() {
        // given
        Money money = Money.pln(105);
        BigDecimal divisor = new BigDecimal("15");

        // when
        Money result = money.divide(divisor, java.math.RoundingMode.HALF_UP);

        // then - 105 / 15 = 7.00
        assertEquals(new BigDecimal("7"), result.value());
        assertEquals("PLN", result.currency());
    }

    @Test
    void shouldDivideMoneyPreservingCurrency() {
        // given
        Money money = Money.eur(150);
        BigDecimal divisor = new BigDecimal("10");

        // when
        Money result = money.divide(divisor);

        // then
        assertEquals("EUR", result.currency());
        assertTrue(result.value().compareTo(new BigDecimal("14")) > 0);
        assertTrue(result.value().compareTo(new BigDecimal("16")) < 0);
    }

    @Test
    void shouldDivideMoneyWithDifferentRoundingModes() {
        // given
        Money money = Money.pln(100);
        BigDecimal divisor = new BigDecimal("3");

        // when
        Money roundUp = money.divide(divisor, java.math.RoundingMode.UP);
        Money roundDown = money.divide(divisor, java.math.RoundingMode.DOWN);
        Money roundHalfUp = money.divide(divisor, java.math.RoundingMode.HALF_UP);

        // then
        assertEquals(new BigDecimal("33.34"), roundUp.value());
        assertEquals(new BigDecimal("33.33"), roundDown.value());
        assertEquals(new BigDecimal("33.33"), roundHalfUp.value());
    }

    @Test
    void shouldReturnCurrencyCode() {
        // given
        Money pln = Money.pln(100);
        Money eur = Money.eur(200);
        Money usd = Money.usd(300);
        Money gbp = Money.gbp(400);

        // when & then
        assertEquals("PLN", pln.currency());
        assertEquals("EUR", eur.currency());
        assertEquals("USD", usd.currency());
        assertEquals("GBP", gbp.currency());
    }

    @Test
    void shouldCreateZeroMoneyInSpecifiedCurrency() {
        // when
        Money zeroPln = Money.zero("PLN");
        Money zeroEur = Money.zero("EUR");
        Money zeroUsd = Money.zero("USD");

        // then
        assertTrue(zeroPln.isZero());
        assertEquals("PLN", zeroPln.currency());
        assertEquals(BigDecimal.ZERO, zeroPln.value());

        assertTrue(zeroEur.isZero());
        assertEquals("EUR", zeroEur.currency());

        assertTrue(zeroUsd.isZero());
        assertEquals("USD", zeroUsd.currency());
    }

    @Test
    void shouldCreateZeroMoneyEqualToSpecificZeroFactories() {
        // when
        Money zeroPln = Money.zero("PLN");
        Money zeroPlnFactory = Money.zeroPln();

        Money zeroEur = Money.zero("EUR");
        Money zeroEurFactory = Money.zeroEur();

        Money zeroUsd = Money.zero("USD");
        Money zeroUsdFactory = Money.zeroUsd();

        // then
        assertEquals(zeroPlnFactory, zeroPln);
        assertEquals(zeroEurFactory, zeroEur);
        assertEquals(zeroUsdFactory, zeroUsd);
    }

    @Test
    void shouldDivideAndPreserveCurrencyInArithmeticChain() {
        // given
        Money money = Money.eur(100);

        // when
        Money result =
                money.divide(new BigDecimal("2"))
                        .multiply(new BigDecimal("3"))
                        .divide(new BigDecimal("5"));

        // then - (100 / 2) * 3 / 5 = 50 * 3 / 5 = 150 / 5 = 30
        assertEquals("EUR", result.currency());
        assertTrue(result.value().compareTo(new BigDecimal("29")) > 0);
        assertTrue(result.value().compareTo(new BigDecimal("31")) < 0);
    }
}
