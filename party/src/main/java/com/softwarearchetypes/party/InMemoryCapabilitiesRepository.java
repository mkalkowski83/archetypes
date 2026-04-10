package com.softwarearchetypes.party;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

class InMemoryCapabilitiesRepository implements CapabilitiesRepository {

    private final Map<CapabilityId, Capability> capabilities = new ConcurrentHashMap<>();

    @Override
    public void save(Capability capability) {
        capabilities.put(capability.id(), capability);
    }

    @Override
    public void remove(CapabilityId id) {
        capabilities.remove(id);
    }

    @Override
    public Optional<Capability> findById(CapabilityId id) {
        return Optional.ofNullable(capabilities.get(id));
    }

    @Override
    public List<Capability> findByPartyId(PartyId partyId) {
        return capabilities.values().stream().filter(c -> c.partyId().equals(partyId)).toList();
    }

    @Override
    public List<Capability> findByType(CapabilityType type) {
        return capabilities.values().stream().filter(c -> c.type().equals(type)).toList();
    }

    @Override
    public List<Capability> findAll() {
        return List.copyOf(capabilities.values());
    }
}
