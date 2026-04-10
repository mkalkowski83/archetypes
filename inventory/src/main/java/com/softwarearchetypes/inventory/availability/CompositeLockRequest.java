package com.softwarearchetypes.inventory.availability;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record CompositeLockRequest(
        ResourceId resourceId,
        Map<ResourceId, LockRequest> componentRequests,
        OwnerId owner,
        LockDuration duration)
        implements LockRequest {

    public CompositeLockRequest {
        Objects.requireNonNull(componentRequests, "componentRequests cannot be null");
        Objects.requireNonNull(owner, "OwnerId cannot be null");
        Objects.requireNonNull(duration, "LockDuration cannot be null");
        if (componentRequests.isEmpty()) {
            throw new IllegalArgumentException("componentRequests cannot be empty");
        }
    }

    public static CompositeLockRequest of(
            Map<ResourceId, LockRequest> componentRequests, OwnerId owner, LockDuration duration) {
        return new CompositeLockRequest(null, componentRequests, owner, duration);
    }

    public static CompositeLockRequest withId(
            ResourceId resourceId,
            Map<ResourceId, LockRequest> componentRequests,
            OwnerId owner,
            LockDuration duration) {
        return new CompositeLockRequest(resourceId, componentRequests, owner, duration);
    }

    public Optional<LockRequest> getRequestForComponent(ResourceId componentId) {
        return Optional.ofNullable(componentRequests.get(componentId));
    }
}
