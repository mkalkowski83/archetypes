package com.softwarearchetypes.ordering;

import com.softwarearchetypes.quantity.money.Money;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service responsible for processing payments. Different implementations can handle different
 * payment models: - Immediate authorize & capture (e-commerce) - Two-step authorize then capture -
 * Refunds and partial refunds
 */
interface PaymentService {

    /**
     * Authorize and capture payment immediately. Used in e-commerce where payment must succeed
     * before order confirmation.
     */
    PaymentResult authorizeAndCapture(PaymentRequest request);

    /** Refund the entire payment for an order. */
    void refund(OrderId orderId, Money amount, String reason);

    /** Partial refund (e.g., when removing a line from confirmed order). */
    void partialRefund(OrderId orderId, Money amount);

    /** Additional charge (e.g., when adding quantity to confirmed order). */
    void additionalCharge(OrderId orderId, Money amount);
}

enum PaymentStatus {
    CAPTURED,
    FAILED,
    PENDING
}

record PaymentRequest(OrderId orderId, Money amount, String paymentMethod) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private OrderId orderId;
        private Money amount;
        private String paymentMethod;

        public Builder orderId(OrderId orderId) {
            this.orderId = orderId;
            return this;
        }

        public Builder amount(Money amount) {
            this.amount = amount;
            return this;
        }

        public Builder paymentMethod(String paymentMethod) {
            this.paymentMethod = paymentMethod;
            return this;
        }

        public PaymentRequest build() {
            return new PaymentRequest(orderId, amount, paymentMethod);
        }
    }
}

record PaymentResult(PaymentStatus status, String transactionId, String failureReason) {
    public static PaymentResult success(String transactionId) {
        return new PaymentResult(PaymentStatus.CAPTURED, transactionId, null);
    }

    public static PaymentResult failure(String reason) {
        return new PaymentResult(PaymentStatus.FAILED, null, reason);
    }
}

class FixablePaymentService implements PaymentService {

    private PaymentResult paymentResult = PaymentResult.success("txn-" + UUID.randomUUID());
    private final List<PaymentRequest> authorizeRequests = new ArrayList<>();
    private final List<OrderId> refundedOrders = new ArrayList<>();

    public void willReturnOnPayment(PaymentResult result) {
        this.paymentResult = result;
    }

    public void willFailOnPayment(String reason) {
        this.paymentResult = PaymentResult.failure(reason);
    }

    public void reset() {
        this.paymentResult = PaymentResult.success("txn-" + UUID.randomUUID());
        this.authorizeRequests.clear();
        this.refundedOrders.clear();
    }

    public List<PaymentRequest> authorizeRequests() {
        return List.copyOf(authorizeRequests);
    }

    public List<OrderId> refundedOrders() {
        return List.copyOf(refundedOrders);
    }

    @Override
    public PaymentResult authorizeAndCapture(PaymentRequest request) {
        authorizeRequests.add(request);
        return paymentResult;
    }

    @Override
    public void refund(OrderId orderId, Money amount, String reason) {
        refundedOrders.add(orderId);
    }

    @Override
    public void partialRefund(OrderId orderId, Money amount) {}

    @Override
    public void additionalCharge(OrderId orderId, Money amount) {}
}
