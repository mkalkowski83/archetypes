package com.softwarearchetypes.rules.discounting.offer.modifiers.simple;

import com.softwarearchetypes.quantity.money.Money;
import com.softwarearchetypes.quantity.money.Percentage;
import com.softwarearchetypes.rules.discounting.offer.Modification;
import com.softwarearchetypes.rules.discounting.offer.OfferItem;
import com.softwarearchetypes.rules.discounting.offer.modifiers.NamedOfferItemModifier;

public class PercentageOfferItemModifier extends NamedOfferItemModifier {
    private final Percentage percentage;

    public PercentageOfferItemModifier(String name, Percentage percentage) {
        super(name);
        this.percentage = percentage;
    }

    @Override
    public OfferItem modify(OfferItem item) {
        Money modification = item.getBasePrice().multiply(percentage);
        Money newPrice = item.getBasePrice().subtract(modification);
        String description = getName() + " (" + percentage + "%)";

        return item.apply(new Modification(newPrice, description));
    }
}
