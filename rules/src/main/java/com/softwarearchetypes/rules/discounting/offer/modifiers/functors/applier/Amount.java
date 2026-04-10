package com.softwarearchetypes.rules.discounting.offer.modifiers.functors.applier;

import com.softwarearchetypes.quantity.money.Money;
import com.softwarearchetypes.rules.discounting.offer.OfferItem;
import java.util.function.Function;

public class Amount implements Function<OfferItem, Money> {
    private final Money amount;

    public Amount(Money amount) {
        this.amount = amount;
    }

    @Override
    public Money apply(OfferItem offerItem) {
        return offerItem.getFinalPrice().subtract(amount);
    }

    public Money getAmount() {
        return amount;
    }
}
