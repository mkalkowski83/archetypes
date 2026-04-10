package com.softwarearchetypes.rules.discounting.config;

import com.softwarearchetypes.quantity.money.Money;
import com.softwarearchetypes.quantity.money.Percentage;
import com.softwarearchetypes.rules.discounting.OfferItemModifier;
import com.softwarearchetypes.rules.discounting.client.ClientContext;
import com.softwarearchetypes.rules.discounting.client.ClientStatus;
import com.softwarearchetypes.rules.discounting.client.rules.ExpensesRule;
import com.softwarearchetypes.rules.discounting.client.rules.StatusRule;
import com.softwarearchetypes.rules.discounting.client.rules.TimeBeingCustomer;
import com.softwarearchetypes.rules.discounting.offer.modifiers.ConfigurableItemModifier;
import com.softwarearchetypes.rules.discounting.offer.modifiers.functors.applier.PercentageFromBase;
import com.softwarearchetypes.rules.discounting.offer.modifiers.functors.guardians.EmptyGuardian;
import com.softwarearchetypes.rules.discounting.offer.modifiers.functors.predicates.MoreExpensiveThanPredicate;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public class SampleStaticConfig implements ConfigProvider {
    @Override
    public Map<OfferItemModifier, Predicate<ClientContext>> loadConfig() {
        Map<OfferItemModifier, Predicate<ClientContext>> configuration = new HashMap<>();

        OfferItemModifier mod1 =
                new ConfigurableItemModifier(
                        "3 years of VIPs",
                        new MoreExpensiveThanPredicate(Money.pln(50)),
                        new PercentageFromBase(Percentage.of(10)),
                        EmptyGuardian.INSTANCE);
        Predicate<ClientContext> pred1 =
                StatusRule.of(ClientStatus.VIP).and(TimeBeingCustomer.ofYears(3));
        configuration.put(mod1, pred1);

        OfferItemModifier mod2 =
                new ConfigurableItemModifier(
                        "VIPs - big fish",
                        new MoreExpensiveThanPredicate(Money.pln(100)),
                        new PercentageFromBase(Percentage.of(10)),
                        EmptyGuardian.INSTANCE);
        Predicate<ClientContext> pred2 =
                StatusRule.of(ClientStatus.VIP).and(ExpensesRule.of(Money.pln(500000)));
        configuration.put(mod2, pred2);

        return configuration;
    }
}
