package com.softwarearchetypes.inventory.availability;

import com.softwarearchetypes.common.Result;
import com.softwarearchetypes.common.Version;
import com.softwarearchetypes.quantity.Quantity;
import com.softwarearchetypes.quantity.Unit;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * TemporalResourceAvailability manages availability of a resource within a specific time slot.
 * Examples: hotel room for a specific night, conference room for a specific hour, appointment slot.
 * Competition model: winner takes all within the time slot.
 *
 * <p>Following the timed-availability pattern: one slot = one blockade. For multi-slot
 * reservations, use TemporalResourceGroupedAvailability.
 */
class TemporalResourceAvailability implements ResourceAvailability {

    private final ResourceAvailabilityId id;
    private final ResourceId resourceId;
    private final TimeSlot slot;
    private final Clock clock;
    private TemporalBlockade blockade;
    private final Version version;

    TemporalResourceAvailability(
            ResourceAvailabilityId id,
            ResourceId resourceId,
            TimeSlot slot,
            Clock clock,
            TemporalBlockade blockade,
            Version version) {
        this.id = Objects.requireNonNull(id, "ResourceAvailabilityId cannot be null");
        this.resourceId = Objects.requireNonNull(resourceId, "ResourceId cannot be null");
        this.slot = Objects.requireNonNull(slot, "TimeSlot cannot be null");
        this.clock = Objects.requireNonNull(clock, "Clock cannot be null");
        this.blockade = blockade;
        this.version = version;
    }

    TemporalResourceAvailability(
            ResourceAvailabilityId id,
            ResourceId resourceId,
            TimeSlot slot,
            TemporalBlockade blockade,
            Version version) {
        this(id, resourceId, slot, Clock.systemUTC(), blockade, version);
    }

    static TemporalResourceAvailability create(ResourceId resourceId, TimeSlot slot, Clock clock) {
        return new TemporalResourceAvailability(
                ResourceAvailabilityId.random(), resourceId, slot, clock, null, Version.initial());
    }

    static TemporalResourceAvailability create(ResourceId resourceId, TimeSlot slot) {
        return create(resourceId, slot, Clock.systemUTC());
    }

    @Override
    public ResourceAvailabilityId id() {
        return id;
    }

    @Override
    public ResourceId resourceId() {
        return resourceId;
    }

    TimeSlot slot() {
        return slot;
    }

    @Override
    public Result<String, BlockadeId> lock(LockRequest request) {
        if (!(request instanceof TemporalLockRequest temporalRequest)) {
            return Result.failure(
                    "Invalid request type. Expected TemporalLockRequest but got: "
                            + request.getClass().getSimpleName());
        }

        if (!temporalRequest.resourceId().equals(resourceId)) {
            return Result.failure(
                    "Resource ID mismatch. Expected: "
                            + resourceId
                            + ", got: "
                            + temporalRequest.resourceId());
        }

        TimeSlot requestedSlot = temporalRequest.slot();
        if (!slot.equals(requestedSlot) && !slot.overlaps(requestedSlot)) {
            return Result.failure("Requested slot does not match this availability slot");
        }

        Instant now = Instant.now(clock);
        if (!isAvailableFor(temporalRequest.owner(), now)) {
            return Result.failure(
                    "Slot is not available - already blocked by: "
                            + (blockade != null ? blockade.owner() : "unknown"));
        }

        TemporalBlockade newBlockade =
                TemporalBlockade.create(temporalRequest.owner(), temporalRequest.duration(), clock);
        this.blockade = newBlockade;

        return Result.success(newBlockade.id());
    }

    @Override
    public Result<String, BlockadeId> unlock(UnlockRequest request) {
        if (blockade == null) {
            return Result.failure("Slot is not blocked");
        }

        if (!blockade.id().equals(request.blockadeId())) {
            return Result.failure("Blockade ID mismatch");
        }

        if (!blockade.isOwnedBy(request.requester())) {
            return Result.failure("Cannot unlock - not the owner of this blockade");
        }

        this.blockade = null;

        return Result.success(request.blockadeId());
    }

    @Override
    public boolean isAvailable() {
        Instant now = Instant.now(clock);
        return blockade == null || blockade.isExpired(now);
    }

    boolean isAvailableFor(OwnerId requester, Instant now) {
        if (blockade == null) {
            return true;
        }
        if (blockade.isExpired(now)) {
            return true;
        }
        return blockade.isOwnedBy(requester);
    }

    @Override
    public Quantity availableQuantity() {
        return isAvailable() ? Quantity.of(1, Unit.pieces()) : Quantity.of(0, Unit.pieces());
    }

    @Override
    public boolean hasBlockade(BlockadeId blockadeId) {
        return blockade != null && blockade.id().equals(blockadeId);
    }

    Optional<TemporalBlockade> blockade() {
        return Optional.ofNullable(blockade);
    }

    OwnerId blockedBy() {
        return blockade != null ? blockade.owner() : OwnerId.none();
    }

    Version version() {
        return version;
    }

    @Override
    public boolean hasExpiredBlockades() {
        Instant now = Instant.now(clock);
        return blockade != null && blockade.isExpired(now);
    }

    @Override
    public List<BlockadeId> releaseExpired() {
        Instant now = Instant.now(clock);
        if (blockade != null && blockade.isExpired(now)) {
            BlockadeId released = blockade.id();
            blockade = null;
            return List.of(released);
        }
        return List.of();
    }
}
