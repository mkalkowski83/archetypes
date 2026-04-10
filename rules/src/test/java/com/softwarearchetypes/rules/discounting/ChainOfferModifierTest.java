package com.softwarearchetypes.rules.discounting;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.softwarearchetypes.quantity.Quantity;
import com.softwarearchetypes.quantity.Unit;
import com.softwarearchetypes.quantity.money.Money;
import com.softwarearchetypes.quantity.money.Percentage;
import com.softwarearchetypes.rules.discounting.offer.OfferItem;
import com.softwarearchetypes.rules.discounting.offer.modifiers.ChainOfferItemModifier;
import com.softwarearchetypes.rules.discounting.offer.modifiers.ConfigurableItemModifier;
import com.softwarearchetypes.rules.discounting.offer.modifiers.functors.applier.Amount;
import com.softwarearchetypes.rules.discounting.offer.modifiers.functors.guardians.MarginGuardian;
import com.softwarearchetypes.rules.discounting.offer.modifiers.functors.predicates.MoreExpensiveThanPredicate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class ChainOfferModifierTest {
    @Test
    public void testOneMod() {
        ChainOfferItemModifier modifier = new ChainOfferItemModifier();
        modifier.add(
                new ConfigurableItemModifier(
                        "expensive line",
                        new MoreExpensiveThanPredicate(Money.pln(1000)),
                        new Amount(Money.pln(100)),
                        new MarginGuardian(Percentage.of(30))));

        var item = modifier.modify(anyItemPriced(2000));
        assertEquals(anyItemPriced(1900).getFinalPrice(), item.getFinalPrice());
    }

    @Test
    public void testGuardian() {
        ChainOfferItemModifier modifier = new ChainOfferItemModifier();
        modifier.add(
                new ConfigurableItemModifier(
                        "expensive line",
                        new MoreExpensiveThanPredicate(Money.pln(1000)),
                        new Amount(Money.pln(900)),
                        new MarginGuardian(Percentage.of(60))));

        var item = modifier.modify(anyItemPriced(2000));
        assertEquals(anyItemPriced(2000).getFinalPrice(), item.getFinalPrice());
    }

    private OfferItem anyItemPriced(double amount) {
        return new OfferItem(
                UUID.randomUUID(), Quantity.of(1, Unit.kilograms()), Money.pln(amount));
    }
}
