package com.softwarearchetypes.quantity.money;

import static java.math.BigDecimal.ZERO;
import static java.math.BigDecimal.valueOf;
import static java.math.RoundingMode.HALF_UP;

import java.math.BigDecimal;

public record Percentage(BigDecimal value) {

    public static Percentage of(BigDecimal percentage) {
        return new Percentage(percentage.setScale(5, HALF_UP));
    }

    public static Percentage of(int percentage) {
        return of(valueOf(percentage));
    }

    public static Percentage ofFraction(double v) {
        return of(valueOf(v * 100));
    }

    public static Percentage oneHundred() {
        return of(100);
    }

    public Percentage {
        if (value.compareTo(ZERO) < 0) {
            throw new IllegalArgumentException("Percentage can't be negative");
        }
    }

    public static Percentage zero() {
        return of(0);
    }

    public Percentage add(Percentage other) {
        return of(this.value.add(other.value));
    }

    public Percentage subtract(Percentage other) {
        return of(this.value.subtract(other.value));
    }

    public Percentage multiply(Percentage other) {
        return of(this.value.multiply(other.value()).divide(new BigDecimal(100)));
    }

    @Override
    public String toString() {
        return value.setScale(2, HALF_UP).stripTrailingZeros().toPlainString() + "%";
    }
}
