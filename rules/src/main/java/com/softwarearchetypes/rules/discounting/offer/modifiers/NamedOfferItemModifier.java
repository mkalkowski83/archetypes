package com.softwarearchetypes.rules.discounting.offer.modifiers;

import com.softwarearchetypes.rules.discounting.OfferItemModifier;

public abstract class NamedOfferItemModifier implements OfferItemModifier {
    private final String name;

    public NamedOfferItemModifier(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
