package com.softwarearchetypes.inventory;

import com.softwarearchetypes.common.Result;
import com.softwarearchetypes.inventory.availability.AvailabilityFacade;
import com.softwarearchetypes.inventory.availability.BlockadeId;
import com.softwarearchetypes.inventory.availability.ResourceId;
import com.softwarearchetypes.quantity.Quantity;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * InventoryFacade manages inventory entries and product instances. It delegates availability
 * operations to AvailabilityFacade.
 */
public class InventoryFacade {

    private final InventoryEntryRepository entryRepository;
    private final InstanceRepository instanceRepository;
    private final ProductDefinitionValidator productValidator;
    private final AvailabilityFacade availabilityFacade;

    InventoryFacade(
            InventoryEntryRepository entryRepository,
            InstanceRepository instanceRepository,
            ProductDefinitionValidator productValidator,
            AvailabilityFacade availabilityFacade) {
        this.entryRepository = entryRepository;
        this.instanceRepository = instanceRepository;
        this.productValidator = productValidator;
        this.availabilityFacade = availabilityFacade;
    }

    public Result<String, InventoryEntryId> handle(CreateInventoryEntry command) {
        if (entryRepository.findByProductId(command.product().productId()).isPresent()) {
            return Result.failure(
                    "Entry already exists for product: " + command.product().productId());
        }
        InventoryEntry entry = InventoryEntry.create(command.product(), availabilityFacade);
        entryRepository.save(entry);
        return Result.success(entry.id());
    }

    public Result<String, InventoryEntryId> mapInstanceToResource(
            InventoryEntryId entryId, InstanceId instanceId, ResourceId resourceId) {
        Optional<InventoryEntry> entry = entryRepository.findById(entryId);
        if (entry.isEmpty()) {
            return Result.failure("Entry not found: " + entryId);
        }
        entry.get().mapInstanceToResource(instanceId, resourceId);
        entryRepository.save(entry.get());
        return Result.success(entryId);
    }

    public Result<String, InventoryEntryId> removeInstanceFromEntry(
            InventoryEntryId entryId, InstanceId instanceId) {
        Optional<InventoryEntry> entry = entryRepository.findById(entryId);
        if (entry.isEmpty()) {
            return Result.failure("Entry not found: " + entryId);
        }
        entry.get().removeInstance(instanceId);
        entryRepository.save(entry.get());
        return Result.success(entryId);
    }

    public Optional<InventoryEntryView> findEntry(InventoryEntryId entryId) {
        return entryRepository.findById(entryId).map(InventoryEntryView::from);
    }

    public Optional<InventoryEntryView> findEntryByProduct(ProductIdentifier productId) {
        return entryRepository.findByProductId(productId).map(InventoryEntryView::from);
    }

    public List<InventoryEntryView> findAllEntries() {
        return entryRepository.findAll().stream().map(InventoryEntryView::from).toList();
    }

    public List<ResourceId> findResourcesForProduct(ProductIdentifier productId) {
        return entryRepository
                .findByProductId(productId)
                .map(InventoryEntry::resourceIds)
                .orElse(List.of());
    }

    // === Counting and filtering ===

    /**
     * Counts the total quantity of all instances for a product. Sums effectiveQuantity() of all
     * instances in the entry.
     */
    public Quantity countProduct(ProductIdentifier productId) {
        return countProduct(productId, InstanceCriteria.any());
    }

    /** Counts the quantity of instances matching the given criteria. */
    public Quantity countProduct(ProductIdentifier productId, InstanceCriteria criteria) {
        Optional<InventoryEntry> entry = entryRepository.findByProductId(productId);
        if (entry.isEmpty()) {
            return Quantity.of(0, com.softwarearchetypes.quantity.Unit.pieces());
        }

        Quantity zero = Quantity.of(0, entry.get().product().preferredUnit());

        return entry.get().instances().stream()
                .map(instanceRepository::findById)
                .flatMap(Optional::stream)
                .filter(criteria::isSatisfiedBy)
                .map(Instance::effectiveQuantity)
                .reduce(zero, Quantity::add);
    }

    /** Finds instance IDs matching the given criteria for a product. */
    public Set<InstanceId> findInstances(ProductIdentifier productId, InstanceCriteria criteria) {
        Optional<InventoryEntry> entry = entryRepository.findByProductId(productId);
        if (entry.isEmpty()) {
            return Set.of();
        }

        return entry.get().instances().stream()
                .map(instanceRepository::findById)
                .flatMap(Optional::stream)
                .filter(criteria::isSatisfiedBy)
                .map(Instance::id)
                .collect(Collectors.toSet());
    }

    // === Instance operations ===

    /**
     * Creates a new product instance and adds it to the InventoryEntry. Validates the instance data
     * against the product definition via ProductDefinitionValidator.
     */
    public Result<String, InstanceId> createInstance(CreateInstance command) {
        // Find or create InventoryEntry for this product
        InventoryEntry entry = entryRepository.findByProductId(command.productId()).orElse(null);

        if (entry == null) {
            return Result.failure("No inventory entry found for product: " + command.productId());
        }

        // Validate against product definition (can call Product archetype)
        Result<String, Void> validation =
                productValidator.validate(
                        command.productId(),
                        entry.product().trackingStrategy(),
                        command.features());
        if (validation.failure()) {
            return Result.failure(validation.getFailure());
        }

        ProductInstance instance =
                new InstanceBuilder(InstanceId.random(), command.productId())
                        .withSerial(command.serialNumber())
                        .withBatch(command.batchId())
                        .withQuantity(command.quantity())
                        .withFeatures(command.features())
                        .build();

        // Save instance to repository
        instanceRepository.save(instance);

        // Add instance to entry (stores only InstanceId)
        entry.addInstance(instance);
        entryRepository.save(entry);

        return Result.success(instance.id());
    }

    public Optional<InstanceView> findInstance(InstanceId instanceId) {
        return instanceRepository.findById(instanceId).map(InstanceView::from);
    }

    public Optional<InstanceView> findInstanceBySerial(SerialNumber serialNumber) {
        return instanceRepository.findBySerialNumber(serialNumber).map(InstanceView::from);
    }

    public List<InstanceView> findInstancesByBatch(BatchId batchId) {
        return instanceRepository.findByBatchId(batchId).stream().map(InstanceView::from).toList();
    }

    public List<InstanceView> findInstancesByProduct(ProductIdentifier productId) {
        return instanceRepository.findByProductId(productId).stream()
                .map(InstanceView::from)
                .toList();
    }

    // === Lock handling ===

    public Result<String, List<BlockadeId>> handle(LockCommand cmd) {
        InventoryEntry entry =
                entryRepository
                        .findByProductId(cmd.productId())
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Product not found: " + cmd.productId()));
        return entry.handle(cmd);
    }
}
