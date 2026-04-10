package com.softwarearchetypes.rules.discounting;

import com.softwarearchetypes.rules.discounting.offer.OfferItem;

public interface OfferItemModifier {
    OfferItem modify(OfferItem item);
}
