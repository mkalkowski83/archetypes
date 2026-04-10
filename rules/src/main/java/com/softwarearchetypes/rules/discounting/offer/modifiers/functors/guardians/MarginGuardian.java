package com.softwarearchetypes.rules.discounting.offer.modifiers.functors.guardians;

import com.softwarearchetypes.quantity.money.Money;
import com.softwarearchetypes.quantity.money.Percentage;
import com.softwarearchetypes.rules.discounting.offer.OfferItem;
import java.util.function.Predicate;

public class MarginGuardian implements Predicate<OfferItem> {
    private final Percentage minMargin;

    public MarginGuardian(Percentage minMargin) {
        this.minMargin = minMargin;
    }

    @Override
    public boolean test(OfferItem offerItem) {
        Money threshold = offerItem.getBasePrice().multiply(minMargin);
        return offerItem.getFinalPrice().isGreaterThanOrEqualTo(threshold);
    }

    public Percentage getMinMargin() {
        return minMargin;
    }
}
