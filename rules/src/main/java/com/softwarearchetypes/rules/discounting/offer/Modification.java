package com.softwarearchetypes.rules.discounting.offer;

import com.softwarearchetypes.quantity.money.Money;

public record Modification(Money amount, String description) {}
