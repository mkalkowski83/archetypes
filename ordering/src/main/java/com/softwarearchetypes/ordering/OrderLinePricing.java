package com.softwarearchetypes.ordering;

import com.softwarearchetypes.quantity.money.Money;
import java.util.List;

sealed interface OrderLinePricing
        permits CalculatedPricing, ArbitraryPricing, EstimatedPricing, NotPricedYet {

    Money unitPrice();

    Money totalPrice();

    List<PriceBreakdown> breakdown();

    boolean isDefinitive();
}

record CalculatedPricing(Money unitPrice, Money totalPrice, List<PriceBreakdown> breakdown)
        implements OrderLinePricing {

    CalculatedPricing {
        breakdown = List.copyOf(breakdown);
    }

    CalculatedPricing(Money unitPrice, Money totalPrice) {
        this(unitPrice, totalPrice, List.of());
    }

    @Override
    public boolean isDefinitive() {
        return true;
    }
}

record EstimatedPricing(Money unitPrice, Money totalPrice, List<PriceBreakdown> breakdown)
        implements OrderLinePricing {

    EstimatedPricing {
        breakdown = List.copyOf(breakdown);
    }

    EstimatedPricing(Money unitPrice, Money totalPrice) {
        this(unitPrice, totalPrice, List.of());
    }

    @Override
    public boolean isDefinitive() {
        return false;
    }
}

record ArbitraryPricing(Money unitPrice, Money totalPrice, String reason)
        implements OrderLinePricing {

    @Override
    public List<PriceBreakdown> breakdown() {
        return List.of();
    }

    @Override
    public boolean isDefinitive() {
        return true;
    }
}

record NotPricedYet() implements OrderLinePricing {

    @Override
    public Money unitPrice() {
        throw new UnsupportedOperationException("Order line has not been priced yet");
    }

    @Override
    public Money totalPrice() {
        throw new UnsupportedOperationException("Order line has not been priced yet");
    }

    @Override
    public List<PriceBreakdown> breakdown() {
        return List.of();
    }

    @Override
    public boolean isDefinitive() {
        return false;
    }
}
