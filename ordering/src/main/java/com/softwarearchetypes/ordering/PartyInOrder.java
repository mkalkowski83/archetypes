package com.softwarearchetypes.ordering;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Links a party snapshot with the roles they play in the order. A party can have multiple roles
 * (e.g., ORDERER and PAYER).
 */
record PartyInOrder(PartySnapshot party, Set<RoleInOrder> roles) {
    public PartyInOrder {
        if (party == null) {
            throw new IllegalArgumentException("Party cannot be null");
        }
        if (roles == null || roles.isEmpty()) {
            throw new IllegalArgumentException("At least one role must be specified");
        }
        roles = Collections.unmodifiableSet(new HashSet<>(roles));
    }

    public static PartyInOrder of(PartySnapshot party, RoleInOrder... roles) {
        return new PartyInOrder(party, Set.of(roles));
    }

    public static PartyInOrder of(PartySnapshot party, Set<RoleInOrder> roles) {
        return new PartyInOrder(party, roles);
    }

    public boolean hasRole(RoleInOrder role) {
        return roles.contains(role);
    }

    public PartyId partyId() {
        return party.partyId();
    }
}
