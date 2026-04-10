package com.softwarearchetypes.inventory.availability;

import com.softwarearchetypes.common.Result;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * TemporalResourceGroupedAvailability coordinates availability across multiple time slots. Used for
 * multi-slot reservations (e.g., hotel stay over multiple nights). All slots must be available for
 * the reservation to succeed.
 */
class TemporalResourceGroupedAvailability {

    private final List<TemporalResourceAvailability> availabilities;

    TemporalResourceGroupedAvailability(List<TemporalResourceAvailability> availabilities) {
        this.availabilities = availabilities;
    }

    static TemporalResourceGroupedAvailability of(
            ResourceId resourceId, List<TimeSlot> slots, Clock clock) {
        List<TemporalResourceAvailability> availabilities =
                slots.stream()
                        .map(slot -> TemporalResourceAvailability.create(resourceId, slot, clock))
                        .toList();
        return new TemporalResourceGroupedAvailability(availabilities);
    }

    static TemporalResourceGroupedAvailability of(ResourceId resourceId, List<TimeSlot> slots) {
        return of(resourceId, slots, Clock.systemUTC());
    }

    Result<String, List<BlockadeId>> block(OwnerId owner, LockDuration duration) {
        if (!isEntirelyAvailable()) {
            return Result.failure("Not all slots are available");
        }

        List<BlockadeId> blockadeIds = new java.util.ArrayList<>();
        for (TemporalResourceAvailability availability : availabilities) {
            TemporalLockRequest request =
                    TemporalLockRequest.of(
                            availability.resourceId(), availability.slot(), owner, duration);
            Result<String, BlockadeId> result = availability.lock(request);
            if (result.failure()) {
                // Rollback already locked slots
                for (int i = 0; i < blockadeIds.size(); i++) {
                    availabilities.get(i).unlock(UnlockRequest.of(owner, blockadeIds.get(i)));
                }
                return Result.failure("Failed to lock slot: " + result.getFailure());
            }
            blockadeIds.add(result.getSuccess());
        }

        return Result.success(blockadeIds);
    }

    Result<String, List<BlockadeId>> release(OwnerId owner, List<BlockadeId> blockadeIds) {
        if (blockadeIds.size() != availabilities.size()) {
            return Result.failure("Blockade count mismatch");
        }

        List<BlockadeId> releasedIds = new java.util.ArrayList<>();
        for (int i = 0; i < availabilities.size(); i++) {
            TemporalResourceAvailability availability = availabilities.get(i);
            BlockadeId blockadeId = blockadeIds.get(i);
            Result<String, BlockadeId> result =
                    availability.unlock(UnlockRequest.of(owner, blockadeId));
            if (result.failure()) {
                return Result.failure("Failed to release slot: " + result.getFailure());
            }
            releasedIds.add(result.getSuccess());
        }

        return Result.success(releasedIds);
    }

    List<TemporalResourceAvailability> availabilities() {
        return availabilities;
    }

    Optional<ResourceId> resourceId() {
        return availabilities.stream().map(TemporalResourceAvailability::resourceId).findFirst();
    }

    int size() {
        return availabilities.size();
    }

    boolean blockedEntirelyBy(OwnerId owner) {
        return availabilities.stream().allMatch(ra -> ra.blockedBy().equals(owner));
    }

    boolean isEntirelyAvailable() {
        return availabilities.stream().allMatch(TemporalResourceAvailability::isAvailable);
    }

    boolean hasNoSlots() {
        return availabilities.isEmpty();
    }

    Set<OwnerId> owners() {
        return availabilities.stream()
                .map(TemporalResourceAvailability::blockedBy)
                .collect(Collectors.toSet());
    }

    List<TemporalResourceAvailability> findBlockedBy(OwnerId owner) {
        return availabilities.stream().filter(ra -> ra.blockedBy().equals(owner)).toList();
    }
}
