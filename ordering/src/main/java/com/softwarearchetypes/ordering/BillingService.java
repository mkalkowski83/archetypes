package com.softwarearchetypes.ordering;

import com.softwarearchetypes.quantity.money.Money;
import java.time.LocalDate;

/**
 * Service responsible for deferred billing. Used in B2B scenarios where payment happens later
 * (e.g., net 30).
 */
interface BillingService {

    /** Record a charge that will be billed later. */
    void recordCharge(BillingRecord record);
}

record BillingRecord(
        OrderId orderId,
        PartyId customerId,
        Money amount,
        LocalDate dueDate,
        boolean invoiceRequired) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private OrderId orderId;
        private PartyId customerId;
        private Money amount;
        private LocalDate dueDate;
        private boolean invoiceRequired;

        public Builder orderId(OrderId orderId) {
            this.orderId = orderId;
            return this;
        }

        public Builder customerId(PartyId customerId) {
            this.customerId = customerId;
            return this;
        }

        public Builder amount(Money amount) {
            this.amount = amount;
            return this;
        }

        public Builder dueDate(LocalDate dueDate) {
            this.dueDate = dueDate;
            return this;
        }

        public Builder invoiceRequired(boolean invoiceRequired) {
            this.invoiceRequired = invoiceRequired;
            return this;
        }

        public BillingRecord build() {
            return new BillingRecord(orderId, customerId, amount, dueDate, invoiceRequired);
        }
    }
}

class FixableBillingService implements BillingService {

    @Override
    public void recordCharge(BillingRecord record) {}
}
