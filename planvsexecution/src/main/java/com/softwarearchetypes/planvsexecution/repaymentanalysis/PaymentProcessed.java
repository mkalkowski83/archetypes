package com.softwarearchetypes.planvsexecution.repaymentanalysis;

import com.softwarearchetypes.quantity.money.Money;
import java.time.Instant;
import java.util.UUID;

public record PaymentProcessed(UUID eventId, Instant when, Money amount, Instant processedAt) {
    public PaymentProcessed(Instant when, Money amount, Instant processedAt) {
        this(UUID.randomUUID(), when, amount, processedAt);
    }

    public static PaymentProcessed of(Instant when, Money amount, Instant processedAt) {
        return new PaymentProcessed(when, amount, processedAt);
    }
}
