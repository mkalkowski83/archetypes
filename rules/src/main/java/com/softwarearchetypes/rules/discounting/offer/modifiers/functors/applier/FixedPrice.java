package com.softwarearchetypes.rules.discounting.offer.modifiers.functors.applier;

import com.softwarearchetypes.quantity.money.Money;
import com.softwarearchetypes.rules.discounting.offer.OfferItem;
import java.util.function.Function;

public class FixedPrice implements Function<OfferItem, Money> {
    private final Money amount;

    public FixedPrice(Money amount) {
        this.amount = amount;
    }

    @Override
    public Money apply(OfferItem offerItem) {
        return amount;
    }

    public Money getAmount() {
        return amount;
    }
}
