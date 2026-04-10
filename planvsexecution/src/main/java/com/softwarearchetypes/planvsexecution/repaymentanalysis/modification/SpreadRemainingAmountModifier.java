package com.softwarearchetypes.planvsexecution.repaymentanalysis.modification;

import com.softwarearchetypes.planvsexecution.repaymentanalysis.Payment;
import com.softwarearchetypes.planvsexecution.repaymentanalysis.PaymentSchedule;
import com.softwarearchetypes.planvsexecution.repaymentanalysis.delta.DeltaResult;
import com.softwarearchetypes.quantity.money.Money;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class SpreadRemainingAmountModifier implements PaymentScheduleModifier {

    private final int newInstallmentCount;

    public SpreadRemainingAmountModifier(int newInstallmentCount) {
        if (newInstallmentCount <= 0) {
            throw new IllegalArgumentException("newInstallmentCount must be > 0");
        }
        this.newInstallmentCount = newInstallmentCount;
    }

    @Override
    public PaymentSchedule modify(PaymentSchedule current, DeltaResult deltaResult) {
        int paidInstallmentsCount = deltaResult.matched().size();

        if (current.isEmpty() || paidInstallmentsCount >= current.size()) {
            return current;
        }

        PaymentSchedule before = current.take(paidInstallmentsCount);
        PaymentSchedule remaining = current.skip(paidInstallmentsCount);

        Money remainingAmount = remaining.totalAmount();
        Money[] div =
                remainingAmount.divideAndRemainder(
                        java.math.BigDecimal.valueOf(newInstallmentCount));
        Money baseInstallmentAmount = div[0];
        Money remainder = div[1];

        Instant firstRemainingDate = remaining.first().when();
        Instant lastRemainingDate = remaining.last().when();

        Duration intervalBetween;
        if (newInstallmentCount == 1) {
            intervalBetween = Duration.ZERO;
        } else {
            Duration totalDuration = Duration.between(firstRemainingDate, lastRemainingDate);
            intervalBetween = totalDuration.dividedBy(newInstallmentCount - 1);
        }

        List<Payment> newPayments = new ArrayList<>(before.payments());
        for (int i = 0; i < newInstallmentCount; i++) {
            Instant date = firstRemainingDate.plus(intervalBetween.multipliedBy(i));
            Money installmentAmount =
                    (i == newInstallmentCount - 1)
                            ? baseInstallmentAmount.add(remainder)
                            : baseInstallmentAmount;
            newPayments.add(Payment.of(date, installmentAmount));
        }

        return PaymentSchedule.of(newPayments);
    }
}
