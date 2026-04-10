package com.softwarearchetypes.ordering;

import java.util.Set;
import java.util.stream.Collectors;

public record PartyInOrderView(String partyId, String partyName, Set<String> roles) {

    static PartyInOrderView from(PartyInOrder partyInOrder) {
        return new PartyInOrderView(
                partyInOrder.partyId().value(),
                partyInOrder.party().name(),
                partyInOrder.roles().stream().map(Enum::name).collect(Collectors.toSet()));
    }
}
