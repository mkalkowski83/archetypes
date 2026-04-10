package com.softwarearchetypes.party;

import java.util.List;

public record CapabilityView(
        CapabilityId id,
        PartyId partyId,
        CapabilityType type,
        List<OperatingScope> scopes,
        Validity validity) {
    public static CapabilityView from(Capability capability) {
        return new CapabilityView(
                capability.id(),
                capability.partyId(),
                capability.type(),
                capability.scopes(),
                capability.validity());
    }
}
