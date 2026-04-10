package com.softwarearchetypes.planvsexecution.repaymentanalysis.modification;

import com.softwarearchetypes.planvsexecution.repaymentanalysis.Payment;
import com.softwarearchetypes.planvsexecution.repaymentanalysis.PaymentSchedule;
import com.softwarearchetypes.planvsexecution.repaymentanalysis.delta.DeltaResult;
import java.util.ArrayList;
import java.util.List;

public class RemoveInstallmentModifier implements PaymentScheduleModifier {

    private final int installmentIndex;

    public RemoveInstallmentModifier(int installmentIndex) {
        if (installmentIndex < 0) {
            throw new IllegalArgumentException("installmentIndex must be >= 0");
        }
        this.installmentIndex = installmentIndex;
    }

    @Override
    public PaymentSchedule modify(PaymentSchedule current, DeltaResult deltaResult) {
        if (current.isEmpty() || installmentIndex >= current.size()) {
            return current;
        }

        List<Payment> newPayments = new ArrayList<>();
        for (int i = 0; i < current.payments().size(); i++) {
            if (i != installmentIndex) {
                newPayments.add(current.payments().get(i));
            }
        }

        return PaymentSchedule.of(newPayments);
    }
}
