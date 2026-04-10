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
 * IndividualResourceAvailability manages availability of a single, indivisible resource. Examples:
 * laptop, projector, bike, parking spot. Competition model: winner takes all - first one to lock
 * wins, others fail.
 */
class IndividualResourceAvailability implements ResourceAvailability {

    private final ResourceAvailabilityId id;
    private final ResourceId resourceId;
    private final Clock clock;
    private Optional<IndividualBlockade> currentBlockade;
    private final Version version;

    public IndividualResourceAvailability(
            ResourceAvailabilityId id,
            ResourceId resourceId,
            Clock clock,
            Optional<IndividualBlockade> currentBlockade,
            Version version) {
        this.id = Objects.requireNonNull(id, "ResourceAvailabilityId cannot be null");
        this.resourceId = Objects.requireNonNull(resourceId, "ResourceId cannot be null");
        this.clock = Objects.requireNonNull(clock, "Clock cannot be null");
        this.currentBlockade =
                Objects.requireNonNull(currentBlockade, "currentBlockade cannot be null");
        this.version = version;
    }

    public IndividualResourceAvailability(
            ResourceAvailabilityId id,
            ResourceId resourceId,
            Optional<IndividualBlockade> currentBlockade,
            Version version) {
        this(id, resourceId, Clock.systemUTC(), currentBlockade, version);
    }

    public static IndividualResourceAvailability create(ResourceId resourceId, Clock clock) {
        return new IndividualResourceAvailability(
                ResourceAvailabilityId.random(),
                resourceId,
                clock,
                Optional.empty(),
                Version.initial());
    }

    public static IndividualResourceAvailability create(ResourceId resourceId) {
        return create(resourceId, Clock.systemUTC());
    }

    @Override
    public ResourceAvailabilityId id() {
        return id;
    }

    @Override
    public ResourceId resourceId() {
        return resourceId;
    }

    @Override
    public Result<String, BlockadeId> lock(LockRequest request) {
        if (!(request instanceof IndividualLockRequest individualRequest)) {
            return Result.failure(
                    "Invalid request type. Expected IndividualLockRequest but got: "
                            + request.getClass().getSimpleName());
        }

        if (!individualRequest.resourceId().equals(resourceId)) {
            return Result.failure(
                    "Resource ID mismatch. Expected: "
                            + resourceId
                            + ", got: "
                            + individualRequest.resourceId());
        }

        Instant now = Instant.now(clock);

        if (!isAvailableFor(individualRequest.owner(), now)) {
            return Result.failure(
                    "Resource is not available - already blocked by: "
                            + currentBlockade.map(b -> b.owner().toString()).orElse("unknown"));
        }

        IndividualBlockade blockade =
                IndividualBlockade.create(
                        individualRequest.owner(), individualRequest.duration(), clock);
        currentBlockade = Optional.of(blockade);

        return Result.success(blockade.id());
    }

    @Override
    public Result<String, BlockadeId> unlock(UnlockRequest request) {
        if (currentBlockade.isEmpty()) {
            return Result.failure("Resource is not blocked");
        }

        IndividualBlockade blockade = currentBlockade.get();
        if (!blockade.id().equals(request.blockadeId())) {
            return Result.failure("Blockade ID mismatch");
        }

        if (!blockade.isOwnedBy(request.requester())) {
            return Result.failure("Cannot unlock - not the owner of this blockade");
        }

        currentBlockade = Optional.empty();

        return Result.success(request.blockadeId());
    }

    @Override
    public boolean isAvailable() {
        return isAvailable(Instant.now(clock));
    }

    private boolean isAvailable(Instant now) {
        return currentBlockade.map(b -> b.isExpired(now)).orElse(true);
    }

    private boolean isAvailableFor(OwnerId requester, Instant now) {
        if (currentBlockade.isEmpty()) {
            return true;
        }
        IndividualBlockade blockade = currentBlockade.get();
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
        return currentBlockade.map(b -> b.id().equals(blockadeId)).orElse(false);
    }

    public Optional<IndividualBlockade> currentBlockade() {
        return currentBlockade;
    }

    public Version version() {
        return version;
    }

    @Override
    public boolean hasExpiredBlockades() {
        Instant now = Instant.now(clock);
        return currentBlockade.map(b -> b.isExpired(now)).orElse(false);
    }

    @Override
    public List<BlockadeId> releaseExpired() {
        Instant now = Instant.now(clock);
        if (currentBlockade.isPresent() && currentBlockade.get().isExpired(now)) {
            BlockadeId released = currentBlockade.get().id();
            currentBlockade = Optional.empty();
            return List.of(released);
        }
        return List.of();
    }
}
