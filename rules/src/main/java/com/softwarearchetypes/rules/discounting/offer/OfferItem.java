package com.softwarearchetypes.rules.discounting.offer;

import com.softwarearchetypes.quantity.Quantity;
import com.softwarearchetypes.quantity.money.Money;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class OfferItem {
    private final UUID productId;
    private final Quantity quantity;
    private final Money basePrice;
    private Money finalPrice;
    private List<Modification> modifications;

    // package scope for unit testing
    OfferItem(
            UUID productId,
            Quantity quantity,
            Money basePrice,
            Money finalPrice,
            List<Modification> modifications) {
        this.productId = productId;
        this.quantity = quantity;
        this.basePrice = basePrice;
        this.finalPrice = finalPrice;
        this.modifications = modifications;
    }

    public OfferItem(UUID productId, Quantity quantity, Money basePrice) {
        this(productId, quantity, basePrice, basePrice, new ArrayList<>());
    }

    public OfferItem apply(Modification modification) {
        Money newPrice = modification.amount();
        List<Modification> newModifications = new ArrayList<>(modifications.size() + 1);
        newModifications.addAll(modifications);
        newModifications.add(modification);
        return new OfferItem(
                productId,
                quantity,
                basePrice,
                newPrice,
                Collections.unmodifiableList(newModifications));
    }

    public Money getBasePrice() {
        return basePrice;
    }

    public Money getFinalPrice() {
        return finalPrice;
    }

    public Quantity getQuantity() {
        return quantity;
    }

    public UUID getProductId() {
        return productId;
    }

    @Override
    public String toString() {
        return "OfferItem{" + "basePrice=" + basePrice + ", finalPrice=" + finalPrice + '}';
    }
}
