package com.softwarearchetypes.inventory.waitlist;

import java.util.Objects;
import java.util.UUID;

record WaitListId(UUID id) {

    WaitListId {
        Objects.requireNonNull(id, "WaitListId cannot be null");
    }

    static WaitListId random() {
        return new WaitListId(UUID.randomUUID());
    }

    static WaitListId of(UUID id) {
        return new WaitListId(id);
    }

    static WaitListId of(String id) {
        return new WaitListId(UUID.fromString(id));
    }
}
