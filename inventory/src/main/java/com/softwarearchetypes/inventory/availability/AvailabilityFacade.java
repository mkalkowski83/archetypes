package com.softwarearchetypes.inventory.availability;

import com.softwarearchetypes.common.Result;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * AvailabilityFacade manages resource availability independently of inventory entries. Resources
 * can be created, locked, and unlocked without going through InventoryEntry.
 */
public class AvailabilityFacade {

    private final ResourceAvailabilityRepository repository;
    private final Clock clock;

    AvailabilityFacade(ResourceAvailabilityRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    AvailabilityFacade(ResourceAvailabilityRepository repository) {
        this(repository, Clock.systemUTC());
    }

    public Result<String, ResourceAvailabilityId> register(ResourceAvailability availability) {
        repository.save(availability);
        return Result.success(availability.id());
    }

    public Result<String, BlockadeId> lock(
            ResourceAvailabilityId availabilityId, LockRequest request) {
        Optional<ResourceAvailability> availability = repository.findById(availabilityId);
        if (availability.isEmpty()) {
            return Result.failure("Availability not found: " + availabilityId);
        }

        Result<String, BlockadeId> result = availability.get().lock(request);
        if (result.success()) {
            repository.save(availability.get());
        }
        return result;
    }

    public Result<String, BlockadeId> handle(LockCommand command) {
        return lock(command.availabilityId(), command.request());
    }

    public Result<String, BlockadeId> unlock(
            ResourceAvailabilityId availabilityId, UnlockRequest request) {
        Optional<ResourceAvailability> availability = repository.findById(availabilityId);
        if (availability.isEmpty()) {
            return Result.failure("Availability not found: " + availabilityId);
        }

        Result<String, BlockadeId> result = availability.get().unlock(request);
        if (result.success()) {
            repository.save(availability.get());
        }
        return result;
    }

    public Result<String, BlockadeId> handle(UnlockRequest request) {
        Optional<ResourceAvailability> availability =
                repository.findByBlockadeId(request.blockadeId());
        if (availability.isEmpty()) {
            return Result.failure("No resource found with blockade: " + request.blockadeId());
        }

        Result<String, BlockadeId> result = availability.get().unlock(request);
        if (result.success()) {
            repository.save(availability.get());
        }
        return result;
    }

    public boolean isAvailable(ResourceAvailabilityId availabilityId) {
        return repository
                .findById(availabilityId)
                .map(ResourceAvailability::isAvailable)
                .orElse(false);
    }

    public Result<String, BlockadeId> lockIndividual(
            ResourceId resourceId, IndividualLockRequest request) {
        List<ResourceAvailability> availabilities = repository.findByResourceId(resourceId);
        if (availabilities.isEmpty()) {
            return Result.failure("No availability found for resource: " + resourceId);
        }
        return lock(availabilities.get(0).id(), request);
    }

    public Result<String, BlockadeId> lockTemporal(
            ResourceId resourceId, TemporalLockRequest request) {
        List<ResourceAvailability> availabilities = repository.findByResourceId(resourceId);
        Optional<ResourceAvailability> matching =
                availabilities.stream()
                        .filter(
                                a ->
                                        a instanceof TemporalResourceAvailability tra
                                                && tra.slot().equals(request.slot()))
                        .findFirst();

        if (matching.isEmpty()) {
            return Result.failure(
                    "No temporal availability found for resource: "
                            + resourceId
                            + " slot: "
                            + request.slot());
        }
        return lock(matching.get().id(), request);
    }

    public Result<String, BlockadeId> lockPool(ResourceId resourceId, PoolLockRequest request) {
        List<ResourceAvailability> availabilities = repository.findByResourceId(resourceId);
        if (availabilities.isEmpty()) {
            return Result.failure("No availability found for resource: " + resourceId);
        }
        return lock(availabilities.get(0).id(), request);
    }

    Optional<ResourceAvailability> find(ResourceAvailabilityId availabilityId) {
        return repository.findById(availabilityId);
    }

    List<ResourceAvailability> findByResourceId(ResourceId resourceId) {
        return repository.findByResourceId(resourceId);
    }

    List<ResourceAvailability> findAvailable() {
        return repository.findAll().stream().filter(ResourceAvailability::isAvailable).toList();
    }

    List<ResourceAvailability> findAvailableByResourceId(ResourceId resourceId) {
        return repository.findByResourceId(resourceId).stream()
                .filter(ResourceAvailability::isAvailable)
                .toList();
    }

    /**
     * Releases all expired blockades from resources that have them. Returns list of all released
     * blockade IDs.
     */
    public List<BlockadeId> releaseExpired() {
        List<BlockadeId> allReleased = new ArrayList<>();
        for (ResourceAvailability availability : repository.findWithExpiredBlockades()) {
            allReleased.addAll(availability.releaseExpired());
            repository.save(availability);
        }
        return allReleased;
    }
}
