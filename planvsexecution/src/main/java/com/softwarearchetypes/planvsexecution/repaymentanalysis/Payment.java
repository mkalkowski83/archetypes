package com.softwarearchetypes.planvsexecution.repaymentanalysis;

import com.softwarearchetypes.quantity.money.Money;
import java.time.Instant;

public record Payment(Instant when, Money amount) implements Comparable<Payment> {

    public Payment {
        if (when == null) {
            throw new IllegalArgumentException("Payment date cannot be null");
        }
        if (amount == null) {
            throw new IllegalArgumentException("Payment amount cannot be null");
        }
    }

    @Override
    public int compareTo(Payment other) {
        return this.when.compareTo(other.when);
    }

    public static Payment of(Instant when, Money amount) {
        return new Payment(when, amount);
    }
}
