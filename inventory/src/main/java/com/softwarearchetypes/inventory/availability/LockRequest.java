package com.softwarearchetypes.inventory.availability;

/**
 * Base interface for lock requests. Different resource types require different lock request
 * implementations.
 */
public sealed interface LockRequest
        permits IndividualLockRequest, PoolLockRequest, TemporalLockRequest, CompositeLockRequest {

    ResourceId resourceId();

    OwnerId owner();

    LockDuration duration();
}
