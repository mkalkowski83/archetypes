package com.softwarearchetypes.ordering;

public record PartyId(String value) {

    public static PartyId of(String value) {
        return new PartyId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
