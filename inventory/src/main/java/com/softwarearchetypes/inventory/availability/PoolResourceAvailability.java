package com.softwarearchetypes.inventory.availability;

import com.softwarearchetypes.common.Result;
import com.softwarearchetypes.common.Version;
import com.softwarearchetypes.quantity.Quantity;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * PoolResourceAvailability manages availability of a quantity-based resource pool. Examples: milk
 * (liters), API tokens, parking spots, consulting hours. Competition model: multiple actors can
 * lock portions of the pool simultaneously.
 */
class PoolResourceAvailability implements ResourceAvailability {

    private final ResourceAvailabilityId id;
    private final ResourceId resourceId;
    private final Clock clock;
    private final Quantity totalCapacity;
    private Quantity withdrawn;
    private final List<PoolBlockade> blockades;
    private final Version version;

    public PoolResourceAvailability(
            ResourceAvailabilityId id,
            ResourceId resourceId,
            Quantity totalCapacity,
            Clock clock,
            Quantity withdrawn,
            List<PoolBlockade> blockades,
            Version version) {
        this.id = Objects.requireNonNull(id, "ResourceAvailabilityId cannot be null");
        this.resourceId = Objects.requireNonNull(resourceId, "ResourceId cannot be null");
        this.totalCapacity = Objects.requireNonNull(totalCapacity, "totalCapacity cannot be null");
        this.clock = Objects.requireNonNull(clock, "Clock cannot be null");
        this.withdrawn = Objects.requireNonNull(withdrawn, "withdrawn cannot be null");
        this.blockades =
                new ArrayList<>(Objects.requireNonNull(blockades, "blockades cannot be null"));
        this.version = version;
    }

    public PoolResourceAvailability(
            ResourceAvailabilityId id,
            ResourceId resourceId,
            Quantity totalCapacity,
            Quantity withdrawn,
            List<PoolBlockade> blockades,
            Version version) {
        this(id, resourceId, totalCapacity, Clock.systemUTC(), withdrawn, blockades, version);
    }

    public static PoolResourceAvailability create(
            ResourceId resourceId, Quantity totalCapacity, Clock clock) {
        return new PoolResourceAvailability(
                ResourceAvailabilityId.random(),
                resourceId,
                totalCapacity,
                clock,
                Quantity.of(0, totalCapacity.unit()),
                new ArrayList<>(),
                Version.initial());
    }

    public static PoolResourceAvailability create(ResourceId resourceId, Quantity totalCapacity) {
        return create(resourceId, totalCapacity, Clock.systemUTC());
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
        if (!(request instanceof PoolLockRequest poolRequest)) {
            return Result.failure(
                    "Invalid request type. Expected PoolLockRequest but got: "
                            + request.getClass().getSimpleName());
        }

        if (!poolRequest.resourceId().equals(resourceId)) {
            return Result.failure(
                    "Resource ID mismatch. Expected: "
                            + resourceId
                            + ", got: "
                            + poolRequest.resourceId());
        }

        Quantity requestedQuantity = poolRequest.quantity();

        if (!isAvailable(requestedQuantity)) {
            return Result.failure(
                    "Insufficient quantity. Requested: "
                            + requestedQuantity
                            + ", available: "
                            + availableQuantity());
        }

        PoolBlockade blockade =
                PoolBlockade.create(
                        poolRequest.owner(), requestedQuantity, poolRequest.duration(), clock);
        blockades.add(blockade);

        return Result.success(blockade.id());
    }

    @Override
    public Result<String, BlockadeId> unlock(UnlockRequest request) {
        Iterator<PoolBlockade> iterator = blockades.iterator();
        while (iterator.hasNext()) {
            PoolBlockade blockade = iterator.next();
            if (blockade.id().equals(request.blockadeId())) {
                if (!blockade.isOwnedBy(request.requester())) {
                    return Result.failure("Cannot unlock - not the owner of this blockade");
                }
                iterator.remove();
                return Result.success(request.blockadeId());
            }
        }
        return Result.failure("Blockade not found: " + request.blockadeId());
    }

    @Override
    public boolean isAvailable() {
        return availableQuantity().amount().signum() > 0;
    }

    public boolean isAvailable(Quantity requestedQuantity) {
        Quantity available = availableQuantity();
        return available.amount().compareTo(requestedQuantity.amount()) >= 0;
    }

    @Override
    public Quantity availableQuantity() {
        Instant now = Instant.now(clock);

        Quantity blocked =
                blockades.stream()
                        .filter(b -> b.isActive(now))
                        .map(PoolBlockade::quantity)
                        .reduce(Quantity.of(0, totalCapacity.unit()), Quantity::add);

        return totalCapacity.subtract(withdrawn).subtract(blocked);
    }

    public Quantity totalCapacity() {
        return totalCapacity;
    }

    public void withdraw(Quantity quantity) {
        if (withdrawn.add(quantity).amount().compareTo(totalCapacity.amount()) > 0) {
            throw new IllegalArgumentException("Cannot withdraw more than total capacity");
        }
        withdrawn = withdrawn.add(quantity);
    }

    public void replenish(Quantity quantity) {
        if (withdrawn.amount().compareTo(quantity.amount()) < 0) {
            throw new IllegalArgumentException("Cannot replenish more than withdrawn");
        }
        withdrawn = withdrawn.subtract(quantity);
    }

    public List<PoolBlockade> activeBlockades() {
        Instant now = Instant.now(clock);
        return blockades.stream().filter(b -> b.isActive(now)).toList();
    }

    @Override
    public boolean hasBlockade(BlockadeId blockadeId) {
        return blockades.stream().anyMatch(b -> b.id().equals(blockadeId));
    }

    public Version version() {
        return version;
    }

    @Override
    public boolean hasExpiredBlockades() {
        Instant now = Instant.now(clock);
        return blockades.stream().anyMatch(b -> b.isExpired(now));
    }

    @Override
    public List<BlockadeId> releaseExpired() {
        Instant now = Instant.now(clock);
        List<BlockadeId> released =
                blockades.stream().filter(b -> b.isExpired(now)).map(PoolBlockade::id).toList();
        blockades.removeIf(b -> b.isExpired(now));
        return released;
    }
}
