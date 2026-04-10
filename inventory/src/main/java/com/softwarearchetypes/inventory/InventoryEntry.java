package com.softwarearchetypes.inventory;

import com.softwarearchetypes.common.Result;
import com.softwarearchetypes.common.Version;
import com.softwarearchetypes.inventory.availability.AvailabilityFacade;
import com.softwarearchetypes.inventory.availability.BlockadeId;
import com.softwarearchetypes.inventory.availability.IndividualLockRequest;
import com.softwarearchetypes.inventory.availability.LockDuration;
import com.softwarearchetypes.inventory.availability.PoolLockRequest;
import com.softwarearchetypes.inventory.availability.ResourceId;
import com.softwarearchetypes.inventory.availability.TemporalLockRequest;
import com.softwarearchetypes.inventory.availability.TimeSlot;
import com.softwarearchetypes.inventory.availability.UnlockRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * InventoryEntry is the aggregate root that maps a product to its instances and resources.
 *
 * <p>- instances: Set of all InstanceIds belonging to this entry - instanceToResource: Maps
 * instances to resources for availability tracking
 *
 * <p>Not all instances need to have a resource mapping (e.g., instances not yet made available).
 * Actual availability is managed by AvailabilityFacade - this is just the mapping.
 */
class InventoryEntry {

    private final InventoryEntryId id;
    private final InventoryProduct product;
    private final Set<InstanceId> instances;
    private final Map<InstanceId, ResourceId> instanceToResource;
    private final AvailabilityFacade availabilityFacade;
    private final Version version;

    InventoryEntry(
            InventoryEntryId id,
            InventoryProduct product,
            Set<InstanceId> instances,
            Map<InstanceId, ResourceId> instanceToResource,
            AvailabilityFacade availabilityFacade,
            Version version) {
        this.id = Objects.requireNonNull(id, "InventoryEntryId cannot be null");
        this.product = Objects.requireNonNull(product, "InventoryProduct cannot be null");
        this.instances = instances != null ? new HashSet<>(instances) : new HashSet<>();
        this.instanceToResource =
                instanceToResource != null ? new HashMap<>(instanceToResource) : new HashMap<>();
        this.availabilityFacade = availabilityFacade;
        this.version = version;
    }

    static InventoryEntry create(InventoryProduct product, AvailabilityFacade availabilityFacade) {
        return new InventoryEntry(
                InventoryEntryId.random(),
                product,
                null,
                null,
                availabilityFacade,
                Version.initial());
    }

    InventoryEntryId id() {
        return id;
    }

    InventoryProduct product() {
        return product;
    }

    ProductIdentifier productId() {
        return product.productId();
    }

    // === Instance management ===

    void addInstance(Instance instance) {
        Objects.requireNonNull(instance, "instance cannot be null");
        instances.add(instance.id());
    }

    void removeInstance(InstanceId instanceId) {
        if (instances.remove(instanceId)) {
            instanceToResource.remove(instanceId);
        }
    }

    boolean hasInstance(InstanceId instanceId) {
        return instances.contains(instanceId);
    }

    Set<InstanceId> instances() {
        return Set.copyOf(instances);
    }

    int instanceCount() {
        return instances.size();
    }

    // === Resource mapping ===

    void mapInstanceToResource(InstanceId instanceId, ResourceId resourceId) {
        Objects.requireNonNull(instanceId, "instanceId cannot be null");
        Objects.requireNonNull(resourceId, "resourceId cannot be null");
        if (!instances.contains(instanceId)) {
            instances.add(instanceId);
        }
        instanceToResource.put(instanceId, resourceId);
    }

    void unmapInstanceFromResource(InstanceId instanceId) {
        instanceToResource.remove(instanceId);
    }

    Optional<ResourceId> resourceFor(InstanceId instanceId) {
        return Optional.ofNullable(instanceToResource.get(instanceId));
    }

    Optional<InstanceId> instanceFor(ResourceId resourceId) {
        return instanceToResource.entrySet().stream()
                .filter(e -> e.getValue().equals(resourceId))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    Map<InstanceId, ResourceId> instanceToResourceMap() {
        return Map.copyOf(instanceToResource);
    }

    List<ResourceId> resourceIds() {
        return List.copyOf(instanceToResource.values());
    }

    boolean hasResource(ResourceId resourceId) {
        return instanceToResource.containsValue(resourceId);
    }

    boolean isEmpty() {
        return instances.isEmpty();
    }

    Version version() {
        return version;
    }

    // === Lock handling ===

    /**
     * Handles lock command by delegating to appropriate handler based on resource specification.
     * Returns list of blockade IDs on success.
     */
    Result<String, List<BlockadeId>> handle(LockCommand cmd) {
        return switch (cmd.resourceSpecification()) {
            case ResourceSpecification.TemporalSpecification temporal ->
                    handleTemporalLock(cmd, temporal);
            case ResourceSpecification.IndividualSpecification individual ->
                    handleIndividualLock(cmd, individual);
            case ResourceSpecification.QuantitySpecification quantity ->
                    handleQuantityLock(cmd, quantity);
        };
    }

    /**
     * Handles temporal lock (e.g., hotel room for multiple nights).
     *
     * <p>Note: Current implementation takes first available resource. Alternative strategies: find
     * resource available for all slots, use customer preference, load balancing, etc.
     */
    private Result<String, List<BlockadeId>> handleTemporalLock(
            LockCommand cmd, ResourceSpecification.TemporalSpecification temporal) {

        if (instanceToResource.isEmpty()) {
            return Result.failure("No resources mapped for product: " + cmd.productId());
        }

        // Simple strategy: take first resource
        // TODO: Could implement smarter selection (check availability first, preferences, etc.)
        ResourceId resourceId = instanceToResource.values().iterator().next();
        List<BlockadeId> blockadeIds = new ArrayList<>();

        for (TimeSlot slot : temporal.timeSlots()) {
            TemporalLockRequest lockRequest =
                    TemporalLockRequest.of(
                            resourceId, slot, cmd.owner(), LockDuration.indefinite());

            Result<String, BlockadeId> lockResult =
                    availabilityFacade.lockTemporal(resourceId, lockRequest);
            if (lockResult.failure()) {
                rollback(blockadeIds, cmd);
                return Result.failure(lockResult.getFailure());
            }
            blockadeIds.add(lockResult.getSuccess());
        }

        return Result.success(blockadeIds);
    }

    /** Handles individual lock for specific instance. */
    private Result<String, List<BlockadeId>> handleIndividualLock(
            LockCommand cmd, ResourceSpecification.IndividualSpecification individual) {

        InstanceId instanceId = individual.instanceId();
        ResourceId resourceId = instanceToResource.get(instanceId);

        if (resourceId == null) {
            return Result.failure("No resource mapped for instance: " + instanceId);
        }

        IndividualLockRequest lockRequest =
                IndividualLockRequest.of(resourceId, cmd.owner(), LockDuration.indefinite());

        Result<String, BlockadeId> lockResult =
                availabilityFacade.lockIndividual(resourceId, lockRequest);
        if (lockResult.failure()) {
            return Result.failure(lockResult.getFailure());
        }

        return Result.success(List.of(lockResult.getSuccess()));
    }

    /**
     * Handles quantity lock for pool resources (e.g., milk, fuel).
     *
     * <p>Note: Current implementation takes first pool resource. Alternative strategies: distribute
     * across multiple pools, FIFO, etc.
     */
    private Result<String, List<BlockadeId>> handleQuantityLock(
            LockCommand cmd, ResourceSpecification.QuantitySpecification quantity) {

        if (instanceToResource.isEmpty()) {
            return Result.failure("No resources mapped for product: " + cmd.productId());
        }

        // Simple strategy: take first pool resource
        ResourceId resourceId = instanceToResource.values().iterator().next();

        PoolLockRequest lockRequest =
                PoolLockRequest.of(
                        resourceId, cmd.quantity(), cmd.owner(), LockDuration.indefinite());

        Result<String, BlockadeId> lockResult =
                availabilityFacade.lockPool(resourceId, lockRequest);
        if (lockResult.failure()) {
            return Result.failure(lockResult.getFailure());
        }

        return Result.success(List.of(lockResult.getSuccess()));
    }

    private void rollback(List<BlockadeId> blockadeIds, LockCommand cmd) {
        for (BlockadeId bid : blockadeIds) {
            availabilityFacade.handle(UnlockRequest.of(cmd.owner(), bid));
        }
    }
}
