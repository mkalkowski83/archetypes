package com.softwarearchetypes.accounting;

import java.time.Instant;
import java.util.List;

public record TransactionView(
        TransactionId id,
        TransactionId refId,
        TransactionType type,
        Instant occurredAt,
        Instant appliesAt,
        List<TransactionAccountEntriesView> entries) {}
