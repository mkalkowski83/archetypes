package com.softwarearchetypes.rules.discounting.config;

import com.softwarearchetypes.quantity.money.Money;
import com.softwarearchetypes.quantity.money.Percentage;
import com.softwarearchetypes.rules.discounting.OfferItemModifier;
import com.softwarearchetypes.rules.discounting.client.ClientContext;
import com.softwarearchetypes.rules.discounting.client.ClientFinder;
import com.softwarearchetypes.rules.discounting.client.ClientStatus;
import com.softwarearchetypes.rules.discounting.client.rules.ExpensesRule;
import com.softwarearchetypes.rules.discounting.client.rules.StatusRule;
import com.softwarearchetypes.rules.discounting.client.rules.TimeBeingCustomer;
import com.softwarearchetypes.rules.discounting.offer.modifiers.ConfigurableItemModifier;
import com.softwarearchetypes.rules.discounting.offer.modifiers.functors.applier.PercentageFromBase;
import com.softwarearchetypes.rules.discounting.offer.modifiers.functors.guardians.EmptyGuardian;
import com.softwarearchetypes.rules.discounting.offer.modifiers.functors.predicates.ItemIdPredicate;
import com.softwarearchetypes.rules.discounting.offer.modifiers.functors.predicates.MoreExpensiveThanPredicate;
import com.softwarearchetypes.rules.discounting.stock.InventoryFinder;
import com.softwarearchetypes.rules.discounting.stock.ProductStock;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public class SampleDynamicConfig implements ConfigProvider {

    private final InventoryFinder inventoryFinder;
    private final ClientFinder clientFinder;

    public SampleDynamicConfig(InventoryFinder inventoryFinder, ClientFinder clientFinder) {
        this.inventoryFinder = inventoryFinder;
        this.clientFinder = clientFinder;
    }

    @Override
    public Map<OfferItemModifier, Predicate<ClientContext>> loadConfig() {
        Map<OfferItemModifier, Predicate<ClientContext>> configuration = new HashMap<>();

        for (ProductStock stock : inventoryFinder.findOverstockedProducts()) {
            Percentage discount = calculateDiscountFor(stock);

            OfferItemModifier overstockModifier =
                    new ConfigurableItemModifier(
                            "Overstock promo for " + stock.productId(),
                            new ItemIdPredicate(stock.productId()),
                            new PercentageFromBase(discount),
                            EmptyGuardian.INSTANCE);

            Predicate<ClientContext> appliesToEveryone = client -> true;

            configuration.put(overstockModifier, appliesToEveryone);
        }

        long vipCount = clientFinder.countVipClients();
        long allCount = clientFinder.countAllClients();
        double vipRatio = allCount == 0 ? 0.0 : (double) vipCount / allCount;

        if (vipRatio < 0.05) {
            OfferItemModifier growVipBaseModifier =
                    new ConfigurableItemModifier(
                            "Grow VIP base - strong promo",
                            new MoreExpensiveThanPredicate(Money.pln(50)),
                            new PercentageFromBase(Percentage.of(20)),
                            EmptyGuardian.INSTANCE);

            Predicate<ClientContext> targetRegularsWithPotential =
                    StatusRule.of(ClientStatus.STANDARD).and(ExpensesRule.of(Money.pln(1000)));

            configuration.put(growVipBaseModifier, targetRegularsWithPotential);
        } else {
            OfferItemModifier vipRetentionModifier =
                    new ConfigurableItemModifier(
                            "VIP retention promo",
                            new MoreExpensiveThanPredicate(Money.pln(100)),
                            new PercentageFromBase(Percentage.of(10)),
                            EmptyGuardian.INSTANCE);

            Predicate<ClientContext> oldVipClients =
                    StatusRule.of(ClientStatus.VIP).and(TimeBeingCustomer.ofYears(3));

            configuration.put(vipRetentionModifier, oldVipClients);
        }

        return configuration;
    }

    /* Sample logic:
    - the more in stock, the bigger the discount
    - the longer the stock lasts, the bigger the discount
    */
    private Percentage calculateDiscountFor(ProductStock stock) {
        int base = 5;
        int extraFromQuantity =
                stock.quantity().amount().doubleValue() > 500
                        ? 10
                        : stock.quantity().amount().doubleValue() > 200 ? 5 : 0;
        int extraFromDays = stock.daysInStock() > 90 ? 10 : stock.daysInStock() > 30 ? 5 : 0;

        int total = Math.min(30, base + extraFromQuantity + extraFromDays); // max 30%
        return Percentage.of(total);
    }
}
