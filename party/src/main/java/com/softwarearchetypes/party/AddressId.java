package com.softwarearchetypes.party;

import static com.softwarearchetypes.common.Preconditions.checkNotNull;

import java.util.UUID;

public record AddressId(UUID value) {

    public AddressId {
        checkNotNull(value, "Address ID needs to be valid UUID");
    }

    public static AddressId of(UUID value) {
        return new AddressId(value);
    }

    public static AddressId random() {
        return of(UUID.randomUUID());
    }

    public String asString() {
        return value.toString();
    }
}
