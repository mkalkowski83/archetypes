package com.softwarearchetypes.rules.discounting.offer.modifiers.functors.guardians;

import com.softwarearchetypes.rules.discounting.offer.OfferItem;
import java.util.function.Predicate;

public class EmptyGuardian implements Predicate<OfferItem> {

    public static final EmptyGuardian INSTANCE = new EmptyGuardian();

    @Override
    public boolean test(OfferItem offerItem) {
        return true;
    }
}
