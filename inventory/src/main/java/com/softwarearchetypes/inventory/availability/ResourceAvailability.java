package com.softwarearchetypes.inventory.availability;

import com.softwarearchetypes.common.Result;
import com.softwarearchetypes.quantity.Quantity;
import java.util.List;

/**
 * ResourceAvailability is the core interface for managing resource availability. Different
 * implementations handle different types of resources: - IndividualResourceAvailability: single
 * resources (one winner takes all) - PoolResourceAvailability: quantity-based resources (shared
 * pool) - TemporalResourceAvailability: time-slot based resources
 */
interface ResourceAvailability {

    ResourceAvailabilityId id();

    ResourceId resourceId();

    Result<String, BlockadeId> lock(LockRequest request);

    Result<String, BlockadeId> unlock(UnlockRequest request);

    boolean isAvailable();

    Quantity availableQuantity();

    boolean hasBlockade(BlockadeId blockadeId);

    /** Checks if this resource has any expired blockades. */
    boolean hasExpiredBlockades();

    /** Releases all expired blockades from this resource. Returns list of released blockade IDs. */
    List<BlockadeId> releaseExpired();
}
