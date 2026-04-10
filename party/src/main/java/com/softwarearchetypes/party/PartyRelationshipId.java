package com.softwarearchetypes.party;

import static com.softwarearchetypes.common.Preconditions.checkArgument;

import java.util.UUID;

public record PartyRelationshipId(UUID value) {

    public PartyRelationshipId {
        checkArgument(value != null, "Party relationship id value cannot be null");
    }

    public String asString() {
        return value.toString();
    }

    public static PartyRelationshipId of(UUID value) {
        return new PartyRelationshipId(value);
    }

    public static PartyRelationshipId random() {
        return of(UUID.randomUUID());
    }
}
