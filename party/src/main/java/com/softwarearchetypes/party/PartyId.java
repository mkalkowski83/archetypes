package com.softwarearchetypes.party;

import static com.softwarearchetypes.common.Preconditions.checkArgument;

import java.util.UUID;

public record PartyId(UUID value) {
    public PartyId {
        checkArgument(value != null, "Party Id value cannot be null");
    }

    public String asString() {
        return value.toString();
    }

    public static PartyId of(UUID value) {
        return new PartyId(value);
    }

    public static PartyId random() {
        return of(UUID.randomUUID());
    }
}
