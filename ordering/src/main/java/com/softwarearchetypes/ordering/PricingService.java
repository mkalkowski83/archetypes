package com.softwarearchetypes.ordering;

import com.softwarearchetypes.quantity.Quantity;
import com.softwarearchetypes.quantity.money.Money;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

interface PricingService {

    OrderLinePricing calculatePrice(PricingContext context);
}

record PricingContext(
        ProductIdentifier productId,
        Quantity quantity,
        OrderParties parties,
        OrderLineSpecification specification,
        LocalDateTime pricingTime) {

    static PricingContext forOrderLine(OrderLine line, OrderParties effectiveParties) {
        return new PricingContext(
                line.productId(),
                line.quantity(),
                effectiveParties,
                line.specification(),
                LocalDateTime.now());
    }
}

class FixablePricingService implements PricingService {

    private Function<PricingContext, OrderLinePricing> pricingFunction = ctx -> new NotPricedYet();
    private final List<PricingContext> calculateRequests = new ArrayList<>();

    @Override
    public OrderLinePricing calculatePrice(PricingContext context) {
        calculateRequests.add(context);
        return pricingFunction.apply(context);
    }

    void willReturn(OrderLinePricing pricing) {
        this.pricingFunction = ctx -> pricing;
    }

    void willCalculate(Money unitPrice, Money totalPrice) {
        this.pricingFunction = ctx -> new CalculatedPricing(unitPrice, totalPrice);
    }

    void willCalculateWithBreakdown(
            Money unitPrice, Money totalPrice, List<PriceBreakdown> breakdown) {
        this.pricingFunction = ctx -> new CalculatedPricing(unitPrice, totalPrice, breakdown);
    }

    void willEstimate(Money unitPrice, Money totalPrice) {
        this.pricingFunction = ctx -> new EstimatedPricing(unitPrice, totalPrice);
    }

    void willAnswer(Function<PricingContext, OrderLinePricing> pricingFunction) {
        this.pricingFunction = pricingFunction;
    }

    List<PricingContext> calculateRequests() {
        return List.copyOf(calculateRequests);
    }
}
