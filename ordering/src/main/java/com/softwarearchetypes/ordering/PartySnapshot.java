package com.softwarearchetypes.ordering;

/**
 * Immutable snapshot of party data at the time of order creation. This is a historical record -
 * even if the party's data changes later, the order retains the values that were valid when it was
 * created.
 */
record PartySnapshot(PartyId partyId, String name, String contactInfo) {
    public PartySnapshot {
        if (partyId == null) {
            throw new IllegalArgumentException("PartyId cannot be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Party name cannot be null or blank");
        }
    }

    public static PartySnapshot of(PartyId partyId, String name, String contactInfo) {
        return new PartySnapshot(partyId, name, contactInfo);
    }

    public static PartySnapshot of(PartyId partyId, String name) {
        return new PartySnapshot(partyId, name, null);
    }
}
