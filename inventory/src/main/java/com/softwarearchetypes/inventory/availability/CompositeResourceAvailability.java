package com.softwarearchetypes.inventory.availability;

import com.softwarearchetypes.common.Result;
import com.softwarearchetypes.common.Version;
import com.softwarearchetypes.quantity.Quantity;
import com.softwarearchetypes.quantity.Unit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * CompositeResourceAvailability manages availability of a bundle of resources. All components must
 * be available for the composite to be available. Examples: car rental (car + GPS + child seat),
 * hotel package (room + breakfast + parking). Competition model: all-or-nothing - all components
 * must be locked together.
 */
class CompositeResourceAvailability implements ResourceAvailability {

    private final ResourceAvailabilityId id;
    private final ResourceId resourceId;
    private final Map<ResourceId, ResourceAvailability> components;
    private final Map<BlockadeId, Map<ResourceId, BlockadeId>> compositeBlockades;
    private final Version version;

    CompositeResourceAvailability(
            ResourceAvailabilityId id,
            ResourceId resourceId,
            Map<ResourceId, ResourceAvailability> components,
            Map<BlockadeId, Map<ResourceId, BlockadeId>> compositeBlockades,
            Version version) {
        this.id = Objects.requireNonNull(id, "ResourceAvailabilityId cannot be null");
        this.resourceId = Objects.requireNonNull(resourceId, "ResourceId cannot be null");
        this.components =
                new HashMap<>(Objects.requireNonNull(components, "components cannot be null"));
        this.compositeBlockades =
                new HashMap<>(
                        Objects.requireNonNull(
                                compositeBlockades, "compositeBlockades cannot be null"));
        if (components.isEmpty()) {
            throw new IllegalArgumentException(
                    "Composite resource must have at least one component");
        }
        this.version = version;
    }

    static CompositeResourceAvailability create(
            ResourceId resourceId, Map<ResourceId, ResourceAvailability> components) {
        return new CompositeResourceAvailability(
                ResourceAvailabilityId.random(),
                resourceId,
                components,
                new HashMap<>(),
                Version.initial());
    }

    static CompositeResourceAvailability of(
            ResourceId resourceId, List<ResourceAvailability> componentList) {
        Map<ResourceId, ResourceAvailability> components = new HashMap<>();
        for (ResourceAvailability component : componentList) {
            components.put(component.resourceId(), component);
        }
        return create(resourceId, components);
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
        if (!(request instanceof CompositeLockRequest compositeRequest)) {
            return Result.failure(
                    "Invalid request type. Expected CompositeLockRequest but got: "
                            + request.getClass().getSimpleName());
        }

        // Validate all component requests are present
        for (ResourceId componentId : components.keySet()) {
            Optional<LockRequest> componentRequest =
                    compositeRequest.getRequestForComponent(componentId);
            if (componentRequest.isEmpty()) {
                return Result.failure("Missing lock request for component: " + componentId);
            }
        }

        // Check availability of all components first (two-phase check)
        for (Map.Entry<ResourceId, ResourceAvailability> entry : components.entrySet()) {
            ResourceAvailability component = entry.getValue();
            if (!component.isAvailable()) {
                return Result.failure("Component not available: " + entry.getKey());
            }
        }

        // Lock all components (collect blockade IDs for potential rollback)
        Map<ResourceId, BlockadeId> lockedComponents = new HashMap<>();
        List<String> failures = new ArrayList<>();

        for (Map.Entry<ResourceId, ResourceAvailability> entry : components.entrySet()) {
            ResourceId componentId = entry.getKey();
            ResourceAvailability component = entry.getValue();
            LockRequest componentRequest =
                    compositeRequest.getRequestForComponent(componentId).get();

            Result<String, BlockadeId> lockResult = component.lock(componentRequest);
            if (lockResult.failure()) {
                failures.add(componentId + ": " + lockResult.getFailure());
            } else {
                lockedComponents.put(componentId, lockResult.getSuccess());
            }
        }

        // If any component failed, rollback all successful locks
        if (!failures.isEmpty()) {
            for (Map.Entry<ResourceId, BlockadeId> locked : lockedComponents.entrySet()) {
                ResourceAvailability component = components.get(locked.getKey());
                UnlockRequest unlockRequest =
                        UnlockRequest.of(compositeRequest.owner(), locked.getValue());
                component.unlock(unlockRequest);
            }
            return Result.failure("Failed to lock components: " + String.join(", ", failures));
        }

        BlockadeId compositeBlockadeId =
                BlockadeId.composite(new ArrayList<>(lockedComponents.values()));
        compositeBlockades.put(compositeBlockadeId, lockedComponents);

        return Result.success(compositeBlockadeId);
    }

    @Override
    public Result<String, BlockadeId> unlock(UnlockRequest request) {
        BlockadeId blockadeId = request.blockadeId();

        Map<ResourceId, BlockadeId> componentBlockades = compositeBlockades.get(blockadeId);
        if (componentBlockades == null) {
            return Result.failure("Composite blockade not found: " + blockadeId);
        }

        List<String> failures = new ArrayList<>();

        for (Map.Entry<ResourceId, BlockadeId> entry : componentBlockades.entrySet()) {
            ResourceId componentId = entry.getKey();
            BlockadeId componentBlockadeId = entry.getValue();
            ResourceAvailability component = components.get(componentId);

            UnlockRequest componentUnlock =
                    UnlockRequest.of(request.requester(), componentBlockadeId);
            Result<String, BlockadeId> result = component.unlock(componentUnlock);
            if (result.failure()) {
                failures.add(componentId + ": " + result.getFailure());
            }
        }

        if (!failures.isEmpty()) {
            return Result.failure("Partial unlock failure: " + String.join(", ", failures));
        }

        compositeBlockades.remove(blockadeId);
        return Result.success(blockadeId);
    }

    @Override
    public boolean isAvailable() {
        return components.values().stream().allMatch(ResourceAvailability::isAvailable);
    }

    @Override
    public Quantity availableQuantity() {
        return isAvailable() ? Quantity.of(1, Unit.pieces()) : Quantity.of(0, Unit.pieces());
    }

    @Override
    public boolean hasBlockade(BlockadeId blockadeId) {
        return compositeBlockades.containsKey(blockadeId);
    }

    Map<ResourceId, ResourceAvailability> components() {
        return Map.copyOf(components);
    }

    Optional<ResourceAvailability> getComponent(ResourceId componentId) {
        return Optional.ofNullable(components.get(componentId));
    }

    Version version() {
        return version;
    }

    @Override
    public boolean hasExpiredBlockades() {
        return components.values().stream().anyMatch(ResourceAvailability::hasExpiredBlockades);
    }

    @Override
    public List<BlockadeId> releaseExpired() {
        List<BlockadeId> released = new ArrayList<>();
        for (ResourceAvailability component : components.values()) {
            released.addAll(component.releaseExpired());
        }
        return released;
    }
}
