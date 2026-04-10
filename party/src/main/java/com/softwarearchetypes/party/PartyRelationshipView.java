package com.softwarearchetypes.party;

public record PartyRelationshipView(
        PartyRelationshipId id,
        PartyId fromPartyId,
        String fromRole,
        PartyId toPartyId,
        String toRole,
        String relationshipName,
        Validity validity) {}
