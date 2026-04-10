package com.softwarearchetypes.rules.discounting.offer.modifiers;

import com.softwarearchetypes.quantity.money.Money;
import com.softwarearchetypes.rules.discounting.offer.Modification;
import com.softwarearchetypes.rules.discounting.offer.OfferItem;
import java.util.function.Function;
import java.util.function.Predicate;

public class ConfigurableItemModifier extends NamedOfferItemModifier {
    private final Predicate<OfferItem> predicate;
    private final Function<OfferItem, Money> applier;
    private final Predicate<OfferItem> guardian;

    public ConfigurableItemModifier(
            String name,
            Predicate<OfferItem> predicate,
            Function<OfferItem, Money> applier,
            Predicate<OfferItem> guardian) {
        super(name);
        this.predicate = predicate;
        this.applier = applier;
        this.guardian = guardian;
    }

    @Override
    public OfferItem modify(OfferItem item) {
        if (predicate.test(item)) {
            Money applied = applier.apply(item);
            if (!applied.equals(item.getFinalPrice())) {
                OfferItem newItem = item.apply(new Modification(applied, getName()));
                if (guardian.test(newItem)) return newItem;
            }
        }
        return item; // unchanged
    }

    public Function<OfferItem, Money> getApplier() {
        return applier;
    }

    public Predicate<OfferItem> getGuardian() {
        return guardian;
    }

    public Predicate<OfferItem> getPredicate() {
        return predicate;
    }
}
