package com.softwarearchetypes.inventory.waitlist;

import java.util.Objects;
import java.util.UUID;

record WaitListEntryId(UUID id) {

    WaitListEntryId {
        Objects.requireNonNull(id, "WaitListEntryId cannot be null");
    }

    static WaitListEntryId random() {
        return new WaitListEntryId(UUID.randomUUID());
    }

    static WaitListEntryId of(UUID id) {
        return new WaitListEntryId(id);
    }

    static WaitListEntryId of(String id) {
        return new WaitListEntryId(UUID.fromString(id));
    }
}
