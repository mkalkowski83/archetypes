package com.softwarearchetypes.rules.discounting.offer.modifiers;

import com.softwarearchetypes.rules.discounting.OfferItemModifier;
import com.softwarearchetypes.rules.discounting.offer.OfferItem;
import java.util.LinkedList;
import java.util.List;

public class ChainOfferItemModifier implements OfferItemModifier {

    private final List<OfferItemModifier> modifiers = new LinkedList<>();

    @Override
    public OfferItem modify(OfferItem item) {
        for (OfferItemModifier modifier : modifiers) {
            item = modifier.modify(item);
        }
        return item;
    }

    public ChainOfferItemModifier add(OfferItemModifier modifier) {
        modifiers.add(modifier);
        return this;
    }
}
