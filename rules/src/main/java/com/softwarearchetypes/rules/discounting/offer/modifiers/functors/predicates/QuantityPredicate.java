package com.softwarearchetypes.rules.discounting.offer.modifiers.functors.predicates;

import com.softwarearchetypes.quantity.Quantity;
import com.softwarearchetypes.rules.discounting.offer.OfferItem;
import com.softwarearchetypes.rules.predicates.RichLogicalPredicate;

public class QuantityPredicate implements RichLogicalPredicate<OfferItem> {
    private final Quantity quantity;

    public QuantityPredicate(Quantity quantity) {
        this.quantity = quantity;
    }

    @Override
    public boolean test(OfferItem offerItem) {
        return false; // TODO comaprator with units
    }

    public Quantity getQuantity() {
        return quantity;
    }
}
