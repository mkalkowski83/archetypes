package com.softwarearchetypes.planvsexecution.repaymentanalysis;

import com.softwarearchetypes.quantity.money.Money;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public record PaymentSchedule(List<Payment> payments) {

    public PaymentSchedule {
        List<Instant> originalDates = payments.stream().map(Payment::when).toList();
        List<Instant> sortedDates = originalDates.stream().sorted().toList();
        if (!originalDates.equals(sortedDates)) {
            throw new IllegalStateException("Payments must be in ascending chronological order");
        }
    }

    public static PaymentSchedule empty() {
        return new PaymentSchedule(List.of());
    }

    public static PaymentSchedule of(List<Payment> payments) {
        List<Payment> sorted = new ArrayList<>(payments);
        sorted.sort(Comparator.comparing(Payment::when));
        return new PaymentSchedule(sorted);
    }

    public static PaymentSchedule fromEvents(List<PaymentProcessed> events) {
        return PaymentSchedule.of(
                events.stream().map(e -> Payment.of(e.when(), e.amount())).toList());
    }

    public Money totalAmount() {
        return payments.stream().map(Payment::amount).reduce(Money.zeroPln(), Money::add);
    }

    public int size() {
        return payments.size();
    }

    public boolean isEmpty() {
        return payments.isEmpty();
    }

    public PaymentSchedule skip(int count) {
        if (count >= payments.size()) {
            return empty();
        }
        return new PaymentSchedule(payments.subList(count, payments.size()));
    }

    public PaymentSchedule take(int count) {
        if (count >= payments.size()) {
            return this;
        }
        return new PaymentSchedule(payments.subList(0, count));
    }

    public Payment first() {
        if (payments.isEmpty()) {
            throw new IllegalStateException("Schedule is empty");
        }
        return payments.get(0);
    }

    public Payment last() {
        if (payments.isEmpty()) {
            throw new IllegalStateException("Schedule is empty");
        }
        return payments.get(payments.size() - 1);
    }
}
