package com.softwarearchetypes.rules.discounting.offer.modifiers.functors.applier;

import com.softwarearchetypes.quantity.money.Money;
import com.softwarearchetypes.quantity.money.Percentage;
import com.softwarearchetypes.rules.discounting.offer.OfferItem;
import java.util.function.Function;

public class PercentageFromBase implements Function<OfferItem, Money> {
    private final Percentage percentage;

    public PercentageFromBase(Percentage percentage) {
        this.percentage = percentage;
    }

    @Override
    public Money apply(OfferItem offerItem) {
        Money mod = offerItem.getBasePrice().multiply(percentage);
        return offerItem.getFinalPrice().subtract(mod);
    }

    public Percentage getPercentage() {
        return percentage;
    }
}
