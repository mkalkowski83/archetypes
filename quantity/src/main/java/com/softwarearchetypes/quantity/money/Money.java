package com.softwarearchetypes.quantity.money;

import static java.math.RoundingMode.HALF_UP;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.money.CurrencyUnit;
import org.jetbrains.annotations.NotNull;

public class Money implements Comparable<Money> {

    private final org.javamoney.moneta.Money money;

    // Private constructor
    private Money(org.javamoney.moneta.Money money) {
        this.money = money;
    }

    // Generic factory method
    public static Money of(Number amount, String currencyCode) {
        return new Money(org.javamoney.moneta.Money.of(amount, currencyCode));
    }

    public static Money of(BigDecimal amount, String currencyCode) {
        return new Money(org.javamoney.moneta.Money.of(amount, currencyCode));
    }

    // Factory methods - PLN
    public static Money pln(int amount) {
        return new Money(org.javamoney.moneta.Money.of(amount, "PLN"));
    }

    public static Money pln(BigDecimal amount) {
        return new Money(org.javamoney.moneta.Money.of(amount, "PLN"));
    }

    public static Money pln(Number amount) {
        return new Money(org.javamoney.moneta.Money.of(amount, "PLN"));
    }

    public static Money pln(String amount) {
        return pln(new BigDecimal(amount));
    }

    public static Money zeroPln() {
        return pln(0);
    }

    public static Money onePln() {
        return pln(1);
    }

    // Factory methods - EUR
    public static Money eur(int amount) {
        return new Money(org.javamoney.moneta.Money.of(amount, "EUR"));
    }

    public static Money eur(BigDecimal amount) {
        return new Money(org.javamoney.moneta.Money.of(amount, "EUR"));
    }

    public static Money eur(Number amount) {
        return new Money(org.javamoney.moneta.Money.of(amount, "EUR"));
    }

    public static Money eur(String amount) {
        return eur(new BigDecimal(amount));
    }

    public static Money zeroEur() {
        return eur(0);
    }

    // Factory methods - GBP
    public static Money gbp(int amount) {
        return new Money(org.javamoney.moneta.Money.of(amount, "GBP"));
    }

    public static Money gbp(BigDecimal amount) {
        return new Money(org.javamoney.moneta.Money.of(amount, "GBP"));
    }

    public static Money gbp(Number amount) {
        return new Money(org.javamoney.moneta.Money.of(amount, "GBP"));
    }

    public static Money gbp(String amount) {
        return gbp(new BigDecimal(amount));
    }

    public static Money zeroGbp() {
        return gbp(0);
    }

    // Factory methods - USD
    public static Money usd(int amount) {
        return new Money(org.javamoney.moneta.Money.of(amount, "USD"));
    }

    public static Money usd(BigDecimal amount) {
        return new Money(org.javamoney.moneta.Money.of(amount, "USD"));
    }

    public static Money usd(Number amount) {
        return new Money(org.javamoney.moneta.Money.of(amount, "USD"));
    }

    public static Money usd(String amount) {
        return usd(new BigDecimal(amount));
    }

    public static Money zeroUsd() {
        return usd(0);
    }

    public static Money zero(String currencyCode) {
        return new Money(org.javamoney.moneta.Money.of(0, currencyCode));
    }

    // Static utility methods
    public static Money min(Money one, Money two) {
        return one.compareTo(two) <= 0 ? one : two;
    }

    public static Optional<Money> min(Set<Money> values) {
        return values.stream().reduce(Money::min);
    }

    public static Money max(Money one, Money two) {
        return one.compareTo(two) <= 0 ? two : one;
    }

    public static Money abs(Money from) {
        return from.abs();
    }

    // Arithmetic operations
    public Money add(Money toAdd) {
        return new Money(this.money.add(toAdd.money));
    }

    public Money subtract(Money toSubtract) {
        return new Money(this.money.subtract(toSubtract.money));
    }

    public Money negate() {
        return new Money(this.money.negate());
    }

    public Money abs() {
        return new Money(money.abs());
    }

    public Money multiply(BigDecimal multiplier) {
        return new Money(this.money.multiply(multiplier));
    }

    public Money multiply(Number multiplier) {
        return new Money(this.money.multiply(multiplier));
    }

    public Money divide(BigDecimal divisor) {
        return new Money(this.money.divide(divisor));
    }

    public Money divide(BigDecimal divisor, java.math.RoundingMode roundingMode) {
        BigDecimal result = this.value().divide(divisor, 2, roundingMode);
        return Money.of(result, this.currency());
    }

    public Money[] divideAndRemainder(BigDecimal divider) {
        org.javamoney.moneta.Money[] result = this.money.divideAndRemainder(divider);
        return new Money[] {new Money(result[0]), new Money(result[1])};
    }

    public Money multiply(Percentage percentage) {
        BigDecimal multiplier = percentage.value().divide(new BigDecimal(100), 30, HALF_UP);
        return this.multiply(
                multiplier); // Delegate to multiply(BigDecimal) which preserves currency
    }

    // Comparison operations
    public boolean isZero() {
        return this.money.isZero();
    }

    public boolean isNegative() {
        return this.money.isNegative();
    }

    public boolean isGreaterThan(Money other) {
        return this.money.isGreaterThan(other.money);
    }

    public boolean isGreaterThanOrEqualTo(Money other) {
        return this.money.isGreaterThanOrEqualTo(other.money);
    }

    // Value extraction
    public BigDecimal value() {
        BigDecimal raw = money.getNumber().numberValue(BigDecimal.class);
        // Round to 10 decimal places to eliminate floating point errors, then strip trailing zeros
        BigDecimal stripped = raw.setScale(10, java.math.RoundingMode.HALF_UP).stripTrailingZeros();
        // Ensure scale is not negative to avoid scientific notation (e.g., 1.5E+2)
        return stripped.scale() < 0 ? stripped.setScale(0) : stripped;
    }

    public String currency() {
        return money.getCurrency().getCurrencyCode();
    }

    // Comparable implementation
    @Override
    public int compareTo(@NotNull Money other) {
        return this.money.compareTo(other.money);
    }

    // Object overrides
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Money other = (Money) o;
        return Objects.equals(this.money, other.money);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(money);
    }

    @Override
    public String toString() {
        return money.getCurrency().getCurrencyCode()
                + " "
                + money.getNumberStripped().toPlainString();
    }

    public CurrencyUnit currencyUnit() {
        return money.getCurrency();
    }
}
