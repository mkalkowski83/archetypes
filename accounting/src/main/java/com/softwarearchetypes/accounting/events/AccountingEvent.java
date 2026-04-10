package com.softwarearchetypes.accounting.events;

import com.softwarearchetypes.common.events.PublishedEvent;

public sealed interface AccountingEvent extends PublishedEvent
        permits CreditEntryRegistered, DebitEntryRegistered {}
