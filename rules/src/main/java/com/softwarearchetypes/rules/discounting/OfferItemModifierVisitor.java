package com.softwarearchetypes.rules.discounting;

import com.softwarearchetypes.quantity.money.Percentage;
import com.softwarearchetypes.rules.discounting.client.ClientStatusVisitor;
import com.softwarearchetypes.rules.discounting.offer.modifiers.simple.PercentageOfferItemModifier;

public class OfferItemModifierVisitor implements ClientStatusVisitor<OfferItemModifier> {
    @Override
    public OfferItemModifier visitStandard() {
        return new PercentageOfferItemModifier("My friend", Percentage.ofFraction(0.05));
    }

    @Override
    public OfferItemModifier visitVIP() {
        return new PercentageOfferItemModifier("VIP", Percentage.ofFraction(0.15));
    }

    @Override
    public OfferItemModifier visitGold() {
        return new PercentageOfferItemModifier("Gold", Percentage.of(25));
    }
}
