package com.softwarearchetypes.accounting.events;

import com.softwarearchetypes.quantity.money.Money;
import java.time.Instant;
import java.util.UUID;

public record CreditEntryRegistered(
        UUID id,
        Instant occurredAt,
        Instant appliesAt,
        UUID entryId,
        UUID accountId,
        UUID transactionId,
        Money amount)
        implements AccountingEvent {

    static final String TYPE = "CreditEntryRegistered";

    public String type() {
        return TYPE;
    }
}
