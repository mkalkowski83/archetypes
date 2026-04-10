package com.softwarearchetypes.rules.discounting.client;

import com.softwarearchetypes.quantity.money.Money;
import java.time.LocalDate;
import java.util.UUID;

public record ClientContext(
        UUID id, ClientStatus status, Money totalExpenses, LocalDate firstOrder) {}
