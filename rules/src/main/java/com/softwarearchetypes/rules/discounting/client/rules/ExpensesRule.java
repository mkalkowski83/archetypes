package com.softwarearchetypes.rules.discounting.client.rules;

import com.softwarearchetypes.quantity.money.Money;
import com.softwarearchetypes.rules.discounting.client.ClientContext;
import com.softwarearchetypes.rules.predicates.RichLogicalPredicate;

public class ExpensesRule implements RichLogicalPredicate<ClientContext> {
    private final Money minAmount;

    public ExpensesRule(Money minAmount) {
        this.minAmount = minAmount;
    }

    public static ExpensesRule of(Money minAmount) {
        return new ExpensesRule(minAmount);
    }

    @Override
    public boolean test(ClientContext clientContext) {
        return clientContext.totalExpenses().isGreaterThanOrEqualTo(minAmount);
    }

    public Money getMinAmount() {
        return minAmount;
    }
}
