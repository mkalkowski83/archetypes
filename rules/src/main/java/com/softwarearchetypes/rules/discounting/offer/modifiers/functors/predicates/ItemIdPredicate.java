package com.softwarearchetypes.rules.discounting.offer.modifiers.functors.predicates;

import com.softwarearchetypes.rules.discounting.offer.OfferItem;
import com.softwarearchetypes.rules.predicates.RichLogicalPredicate;
import java.util.UUID;

public class ItemIdPredicate implements RichLogicalPredicate<OfferItem> {
    private final UUID productId;

    public ItemIdPredicate(UUID productId) {
        this.productId = productId;
    }

    @Override
    public boolean test(OfferItem offerItem) {
        return offerItem.getProductId().equals(productId);
    }

    public UUID getProductId() {
        return productId;
    }
}
