package com.softwarearchetypes.inventory.availability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class InMemoryResourceAvailabilityRepository implements ResourceAvailabilityRepository {

    private final Map<ResourceAvailabilityId, ResourceAvailability> storage = new HashMap<>();

    @Override
    public void save(ResourceAvailability availability) {
        storage.put(availability.id(), availability);
    }

    @Override
    public Optional<ResourceAvailability> findById(ResourceAvailabilityId id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public Optional<ResourceAvailability> findByBlockadeId(BlockadeId blockadeId) {
        return storage.values().stream().filter(a -> a.hasBlockade(blockadeId)).findFirst();
    }

    @Override
    public List<ResourceAvailability> findByResourceId(ResourceId resourceId) {
        return storage.values().stream().filter(a -> a.resourceId().equals(resourceId)).toList();
    }

    @Override
    public List<ResourceAvailability> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public List<ResourceAvailability> findWithExpiredBlockades() {
        return storage.values().stream().filter(ResourceAvailability::hasExpiredBlockades).toList();
    }

    @Override
    public void delete(ResourceAvailabilityId id) {
        storage.remove(id);
    }
}
