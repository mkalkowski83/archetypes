package com.softwarearchetypes.ordering;

import com.softwarearchetypes.quantity.money.Money;
import java.util.List;

record PriceBreakdown(String componentName, Money amount, List<PriceBreakdown> children) {

    PriceBreakdown {
        children = List.copyOf(children);
    }

    PriceBreakdown(String componentName, Money amount) {
        this(componentName, amount, List.of());
    }
}
