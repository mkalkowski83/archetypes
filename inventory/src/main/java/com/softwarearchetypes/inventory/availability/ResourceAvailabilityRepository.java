package com.softwarearchetypes.inventory.availability;

import java.util.List;
import java.util.Optional;

interface ResourceAvailabilityRepository {

    void save(ResourceAvailability availability);

    Optional<ResourceAvailability> findById(ResourceAvailabilityId id);

    Optional<ResourceAvailability> findByBlockadeId(BlockadeId blockadeId);

    List<ResourceAvailability> findByResourceId(ResourceId resourceId);

    List<ResourceAvailability> findAll();

    List<ResourceAvailability> findWithExpiredBlockades();

    void delete(ResourceAvailabilityId id);
}
