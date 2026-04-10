package com.softwarearchetypes.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import com.softwarearchetypes.common.Result;
import com.softwarearchetypes.inventory.availability.AvailabilityConfiguration;
import com.softwarearchetypes.inventory.availability.AvailabilityFixture;
import com.softwarearchetypes.inventory.availability.BlockadeId;
import com.softwarearchetypes.inventory.availability.OwnerId;
import com.softwarearchetypes.inventory.availability.ResourceId;
import com.softwarearchetypes.inventory.availability.TimeSlot;
import com.softwarearchetypes.quantity.Quantity;
import com.softwarearchetypes.quantity.Unit;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class InventoryFacadeTest {

    private Clock clock = Clock.fixed(Instant.parse("2024-06-01T10:00:00Z"), ZoneId.of("UTC"));
    private AvailabilityConfiguration availabilityConfig =
            AvailabilityConfiguration.inMemory(clock);
    private InventoryConfiguration config = InventoryConfiguration.inMemory(availabilityConfig);
    private InventoryFacade facade = config.facade();
    private AvailabilityFixture availabilityFixture =
            new AvailabilityFixture(availabilityConfig.facade(), clock);

    // === InventoryEntry tests ===

    @Test
    void createsInventoryEntry() {
        // given
        InventoryProduct laptop =
                InventoryProduct.individuallyTracked(ProductIdentifier.random(), "MacBook Pro 16");

        // when
        Result<String, InventoryEntryId> result =
                facade.handle(CreateInventoryEntry.forProduct(laptop));

        // then
        assertThat(result.success()).isTrue();
        Optional<InventoryEntryView> view = facade.findEntry(result.getSuccess());
        assertThat(view).isPresent();
        assertThat(view.get().productName()).isEqualTo("MacBook Pro 16");
        assertThat(view.get().instanceIds()).isEmpty();
        assertThat(view.get().instanceToResource()).isEmpty();
    }

    @Test
    void findsEntryByProductId() {
        // given
        ProductIdentifier productId = ProductIdentifier.random();
        InventoryProduct laptop = InventoryProduct.individuallyTracked(productId, "MacBook Pro 16");
        facade.handle(CreateInventoryEntry.forProduct(laptop));

        // when
        Optional<InventoryEntryView> found = facade.findEntryByProduct(productId);

        // then
        assertThat(found).isPresent();
        assertThat(found.get().productId()).isEqualTo(productId);
    }

    @Test
    void findsAllEntries() {
        // given
        facade.handle(
                CreateInventoryEntry.forProduct(
                        InventoryProduct.individuallyTracked(
                                ProductIdentifier.random(), "Laptop")));
        facade.handle(
                CreateInventoryEntry.forProduct(
                        InventoryProduct.individuallyTracked(
                                ProductIdentifier.random(), "Projector")));
        facade.handle(
                CreateInventoryEntry.forProduct(
                        InventoryProduct.identical(ProductIdentifier.random(), "Milk")));

        // when
        List<InventoryEntryView> all = facade.findAllEntries();

        // then
        assertThat(all).hasSize(3);
    }

    // === Instance creation tests ===

    @Test
    void createsInstanceAndAddsToEntry() {
        // given
        ProductIdentifier productId = ProductIdentifier.random();
        InventoryProduct laptop = InventoryProduct.individuallyTracked(productId, "MacBook Pro 16");
        InventoryEntryId entryId =
                facade.handle(CreateInventoryEntry.forProduct(laptop)).getSuccess();

        CreateInstance command =
                CreateInstance.forProduct(productId).withSerial("SN-12345").build();

        // when
        Result<String, InstanceId> result = facade.createInstance(command);

        // then
        assertThat(result.success()).isTrue();

        // instance is saved
        Optional<InstanceView> instanceView = facade.findInstance(result.getSuccess());
        assertThat(instanceView).isPresent();
        assertThat(instanceView.get().serialNumber()).isEqualTo("SN-12345");

        // instance is added to entry
        Optional<InventoryEntryView> entryView = facade.findEntry(entryId);
        assertThat(entryView).isPresent();
        assertThat(entryView.get().instanceIds()).containsExactly(result.getSuccess());
    }

    @Test
    void createsMultipleInstancesInEntry() {
        // given
        ProductIdentifier productId = ProductIdentifier.random();
        InventoryProduct laptops =
                InventoryProduct.individuallyTracked(productId, "MacBook Pro 16");
        InventoryEntryId entryId =
                facade.handle(CreateInventoryEntry.forProduct(laptops)).getSuccess();

        // when
        InstanceId instance1 =
                facade.createInstance(
                                CreateInstance.forProduct(productId).withSerial("SN-001").build())
                        .getSuccess();
        InstanceId instance2 =
                facade.createInstance(
                                CreateInstance.forProduct(productId).withSerial("SN-002").build())
                        .getSuccess();
        InstanceId instance3 =
                facade.createInstance(
                                CreateInstance.forProduct(productId).withSerial("SN-003").build())
                        .getSuccess();

        // then
        Optional<InventoryEntryView> view = facade.findEntry(entryId);
        assertThat(view).isPresent();
        assertThat(view.get().instanceIds()).hasSize(3);
        assertThat(view.get().instanceIds())
                .containsExactlyInAnyOrder(instance1, instance2, instance3);
    }

    @Test
    void failsToCreateInstanceWithoutEntry() {
        // given - no entry created for this product
        ProductIdentifier productId = ProductIdentifier.random();
        CreateInstance command =
                CreateInstance.forProduct(productId).withSerial("SN-12345").build();

        // when
        Result<String, InstanceId> result = facade.createInstance(command);

        // then
        assertThat(result.failure()).isTrue();
        assertThat(result.getFailure()).contains("No inventory entry found");
    }

    @Test
    void createsInstanceWithBatchId() {
        // given
        ProductIdentifier productId = ProductIdentifier.random();
        InventoryProduct fuel = InventoryProduct.batchTracked(productId, "Fuel 95");
        facade.handle(CreateInventoryEntry.forProduct(fuel));

        BatchId batchId = BatchId.random();
        CreateInstance command = CreateInstance.forProduct(productId).withBatch(batchId).build();

        // when
        Result<String, InstanceId> result = facade.createInstance(command);

        // then
        assertThat(result.success()).isTrue();
        Optional<InstanceView> view = facade.findInstance(result.getSuccess());
        assertThat(view).isPresent();
        assertThat(view.get().batchId()).isEqualTo(batchId.toString());
    }

    @Test
    void createsInstanceWithFeatures() {
        // given
        ProductIdentifier productId = ProductIdentifier.random();
        InventoryProduct phone = InventoryProduct.individuallyTracked(productId, "iPhone 15 Pro");
        facade.handle(CreateInventoryEntry.forProduct(phone));

        CreateInstance command =
                CreateInstance.forProduct(productId)
                        .withSerial("SN-12345")
                        .withFeatures(java.util.Map.of("color", "silver", "storage", "256GB"))
                        .build();

        // when
        Result<String, InstanceId> result = facade.createInstance(command);

        // then
        assertThat(result.success()).isTrue();
        Optional<InstanceView> view = facade.findInstance(result.getSuccess());
        assertThat(view).isPresent();
        assertThat(view.get().features()).containsEntry("color", "silver");
        assertThat(view.get().features()).containsEntry("storage", "256GB");
    }

    // === Instance to Resource mapping tests ===

    @Test
    void mapsInstanceToResource() {
        // given
        ProductIdentifier productId = ProductIdentifier.random();
        InventoryProduct laptop = InventoryProduct.individuallyTracked(productId, "MacBook Pro 16");
        InventoryEntryId entryId =
                facade.handle(CreateInventoryEntry.forProduct(laptop)).getSuccess();

        InstanceId instanceId =
                facade.createInstance(
                                CreateInstance.forProduct(productId).withSerial("SN-001").build())
                        .getSuccess();

        ResourceId resourceId = ResourceId.random();

        // when
        Result<String, InventoryEntryId> result =
                facade.mapInstanceToResource(entryId, instanceId, resourceId);

        // then
        assertThat(result.success()).isTrue();
        Optional<InventoryEntryView> view = facade.findEntry(entryId);
        assertThat(view).isPresent();
        assertThat(view.get().instanceToResource()).containsEntry(instanceId, resourceId);
    }

    @Test
    void mapsMultipleInstancesToResources() {
        // given
        ProductIdentifier productId = ProductIdentifier.random();
        InventoryProduct laptops =
                InventoryProduct.individuallyTracked(productId, "MacBook Pro 16");
        InventoryEntryId entryId =
                facade.handle(CreateInventoryEntry.forProduct(laptops)).getSuccess();

        InstanceId instance1 =
                facade.createInstance(
                                CreateInstance.forProduct(productId).withSerial("SN-001").build())
                        .getSuccess();
        InstanceId instance2 =
                facade.createInstance(
                                CreateInstance.forProduct(productId).withSerial("SN-002").build())
                        .getSuccess();

        ResourceId resource1 = ResourceId.random();
        ResourceId resource2 = ResourceId.random();

        // when
        facade.mapInstanceToResource(entryId, instance1, resource1);
        facade.mapInstanceToResource(entryId, instance2, resource2);

        // then
        Optional<InventoryEntryView> view = facade.findEntry(entryId);
        assertThat(view).isPresent();
        assertThat(view.get().instanceToResource()).hasSize(2);
        assertThat(view.get().instanceToResource()).containsEntry(instance1, resource1);
        assertThat(view.get().instanceToResource()).containsEntry(instance2, resource2);
    }

    @Test
    void mapsNewInstanceToResourceCreatingInstanceReferenceIfNeeded() {
        // given
        ProductIdentifier productId = ProductIdentifier.random();
        InventoryProduct laptop = InventoryProduct.individuallyTracked(productId, "MacBook Pro 16");
        InventoryEntryId entryId =
                facade.handle(CreateInventoryEntry.forProduct(laptop)).getSuccess();

        InstanceId newInstance = InstanceId.random();
        ResourceId resourceId = ResourceId.random();

        // when - mapping should succeed and add the instance reference
        Result<String, InventoryEntryId> result =
                facade.mapInstanceToResource(entryId, newInstance, resourceId);

        // then
        assertThat(result.success()).isTrue();
        Optional<InventoryEntryView> view = facade.findEntry(entryId);
        assertThat(view).isPresent();
        assertThat(view.get().instanceToResource()).containsEntry(newInstance, resourceId);
    }

    // === Remove instance tests ===

    @Test
    void removesInstanceFromEntry() {
        // given
        ProductIdentifier productId = ProductIdentifier.random();
        InventoryProduct laptop = InventoryProduct.individuallyTracked(productId, "MacBook Pro 16");
        InventoryEntryId entryId =
                facade.handle(CreateInventoryEntry.forProduct(laptop)).getSuccess();

        InstanceId instanceId =
                facade.createInstance(
                                CreateInstance.forProduct(productId).withSerial("SN-001").build())
                        .getSuccess();

        // when
        Result<String, InventoryEntryId> result =
                facade.removeInstanceFromEntry(entryId, instanceId);

        // then
        assertThat(result.success()).isTrue();
        Optional<InventoryEntryView> view = facade.findEntry(entryId);
        assertThat(view).isPresent();
        assertThat(view.get().instanceIds()).isEmpty();
    }

    @Test
    void removingInstanceAlsoRemovesResourceMapping() {
        // given
        ProductIdentifier productId = ProductIdentifier.random();
        InventoryProduct laptop = InventoryProduct.individuallyTracked(productId, "MacBook Pro 16");
        InventoryEntryId entryId =
                facade.handle(CreateInventoryEntry.forProduct(laptop)).getSuccess();

        InstanceId instanceId =
                facade.createInstance(
                                CreateInstance.forProduct(productId).withSerial("SN-001").build())
                        .getSuccess();

        ResourceId resourceId = ResourceId.random();
        facade.mapInstanceToResource(entryId, instanceId, resourceId);

        // when
        facade.removeInstanceFromEntry(entryId, instanceId);

        // then
        Optional<InventoryEntryView> view = facade.findEntry(entryId);
        assertThat(view).isPresent();
        assertThat(view.get().instanceIds()).isEmpty();
        assertThat(view.get().instanceToResource()).isEmpty();
    }

    // === Instance query tests ===

    @Test
    void findsInstanceBySerialNumber() {
        // given
        ProductIdentifier productId = ProductIdentifier.random();
        InventoryProduct phone = InventoryProduct.individuallyTracked(productId, "iPhone 15 Pro");
        facade.handle(CreateInventoryEntry.forProduct(phone));

        facade.createInstance(
                CreateInstance.forProduct(productId).withSerial("UNIQUE-SN-999").build());

        // when
        Optional<InstanceView> found =
                facade.findInstanceBySerial(SerialNumber.of("UNIQUE-SN-999"));

        // then
        assertThat(found).isPresent();
        assertThat(found.get().serialNumber()).isEqualTo("UNIQUE-SN-999");
    }

    @Test
    void findsInstancesByBatch() {
        // given
        ProductIdentifier productId = ProductIdentifier.random();
        InventoryProduct fuel = InventoryProduct.batchTracked(productId, "Fuel 95");
        facade.handle(CreateInventoryEntry.forProduct(fuel));

        BatchId batchId = BatchId.random();
        facade.createInstance(CreateInstance.forProduct(productId).withBatch(batchId).build());
        facade.createInstance(CreateInstance.forProduct(productId).withBatch(batchId).build());
        facade.createInstance(
                CreateInstance.forProduct(productId).withBatch(BatchId.random()).build());

        // when
        List<InstanceView> instances = facade.findInstancesByBatch(batchId);

        // then
        assertThat(instances).hasSize(2);
    }

    @Test
    void findsInstancesByProduct() {
        // given
        ProductIdentifier productId1 = ProductIdentifier.random();
        ProductIdentifier productId2 = ProductIdentifier.random();

        facade.handle(
                CreateInventoryEntry.forProduct(
                        InventoryProduct.individuallyTracked(productId1, "Laptop 1")));
        facade.handle(
                CreateInventoryEntry.forProduct(
                        InventoryProduct.individuallyTracked(productId2, "Laptop 2")));

        facade.createInstance(CreateInstance.forProduct(productId1).withSerial("SN-1").build());
        facade.createInstance(CreateInstance.forProduct(productId1).withSerial("SN-2").build());
        facade.createInstance(CreateInstance.forProduct(productId2).withSerial("SN-3").build());

        // when
        List<InstanceView> instances = facade.findInstancesByProduct(productId1);

        // then
        assertThat(instances).hasSize(2);
    }

    // === Counting tests ===

    @Test
    void countsIndividuallyTrackedProducts() {
        // given - 3 individually tracked laptops (each has implicit quantity of 1 piece)
        ProductIdentifier productId = ProductIdentifier.random();
        InventoryProduct laptops =
                InventoryProduct.individuallyTracked(productId, "MacBook Pro 16");
        facade.handle(CreateInventoryEntry.forProduct(laptops));

        facade.createInstance(CreateInstance.forProduct(productId).withSerial("SN-001").build());
        facade.createInstance(CreateInstance.forProduct(productId).withSerial("SN-002").build());
        facade.createInstance(CreateInstance.forProduct(productId).withSerial("SN-003").build());

        // when
        Quantity count = facade.countProduct(productId);

        // then
        assertThat(count.amount().intValue()).isEqualTo(3);
    }

    @Test
    void countsBatchTrackedProductsWithQuantity() {
        // given - 2 fuel deliveries with explicit quantities
        ProductIdentifier productId = ProductIdentifier.random();
        InventoryProduct fuel =
                InventoryProduct.of(
                        productId, "Fuel 95", ProductTrackingStrategy.BATCH_TRACKED, Unit.liters());
        facade.handle(CreateInventoryEntry.forProduct(fuel));

        facade.createInstance(
                CreateInstance.forProduct(productId)
                        .withBatch(BatchId.random())
                        .withQuantity(Quantity.of(5000, Unit.liters()))
                        .build());
        facade.createInstance(
                CreateInstance.forProduct(productId)
                        .withBatch(BatchId.random())
                        .withQuantity(Quantity.of(3000, Unit.liters()))
                        .build());

        // when
        Quantity count = facade.countProduct(productId);

        // then
        assertThat(count.amount().intValue()).isEqualTo(8000);
        assertThat(count.unit()).isEqualTo(Unit.liters());
    }

    @Test
    void countsWithCriteria() {
        // given - instances with different batches
        ProductIdentifier productId = ProductIdentifier.random();
        InventoryProduct fuel = InventoryProduct.batchTracked(productId, "Fuel 95");
        facade.handle(CreateInventoryEntry.forProduct(fuel));

        BatchId targetBatch = BatchId.random();
        BatchId otherBatch = BatchId.random();

        facade.createInstance(CreateInstance.forProduct(productId).withBatch(targetBatch).build());
        facade.createInstance(CreateInstance.forProduct(productId).withBatch(targetBatch).build());
        facade.createInstance(CreateInstance.forProduct(productId).withBatch(otherBatch).build());

        // when - count only target batch
        Quantity count = facade.countProduct(productId, InstanceCriteria.byBatch(targetBatch));

        // then
        assertThat(count.amount().intValue()).isEqualTo(2);
    }

    @Test
    void findsInstancesWithCriteria() {
        // given
        ProductIdentifier productId = ProductIdentifier.random();
        InventoryProduct fuel = InventoryProduct.batchTracked(productId, "Fuel 95");
        facade.handle(CreateInventoryEntry.forProduct(fuel));

        BatchId targetBatch = BatchId.random();
        BatchId otherBatch = BatchId.random();

        InstanceId inst1 =
                facade.createInstance(
                                CreateInstance.forProduct(productId).withBatch(targetBatch).build())
                        .getSuccess();
        InstanceId inst2 =
                facade.createInstance(
                                CreateInstance.forProduct(productId).withBatch(targetBatch).build())
                        .getSuccess();
        facade.createInstance(CreateInstance.forProduct(productId).withBatch(otherBatch).build());

        // when
        Set<InstanceId> found =
                facade.findInstances(productId, InstanceCriteria.byBatch(targetBatch));

        // then
        assertThat(found).hasSize(2);
        assertThat(found).containsExactlyInAnyOrder(inst1, inst2);
    }

    @Test
    void returnsZeroForEmptyEntry() {
        // given
        ProductIdentifier productId = ProductIdentifier.random();
        InventoryProduct laptops =
                InventoryProduct.individuallyTracked(productId, "MacBook Pro 16");
        facade.handle(CreateInventoryEntry.forProduct(laptops));
        // no instances added

        // when
        Quantity count = facade.countProduct(productId);

        // then
        assertThat(count.amount().intValue()).isEqualTo(0);
    }

    @Test
    void returnsZeroForUnknownProduct() {
        // when
        Quantity count = facade.countProduct(ProductIdentifier.random());

        // then
        assertThat(count.amount().intValue()).isEqualTo(0);
    }

    // === Find resources for product tests ===

    @Test
    void findsResourcesForProduct() {
        // given
        ProductIdentifier productId = ProductIdentifier.random();
        InventoryProduct laptop = InventoryProduct.individuallyTracked(productId, "MacBook Pro 16");
        InventoryEntryId entryId =
                facade.handle(CreateInventoryEntry.forProduct(laptop)).getSuccess();

        InstanceId instance1 = InstanceId.random();
        InstanceId instance2 = InstanceId.random();
        ResourceId resource1 = ResourceId.random();
        ResourceId resource2 = ResourceId.random();

        facade.mapInstanceToResource(entryId, instance1, resource1);
        facade.mapInstanceToResource(entryId, instance2, resource2);

        // when
        List<ResourceId> resources = facade.findResourcesForProduct(productId);

        // then
        assertThat(resources).hasSize(2);
        assertThat(resources).containsExactlyInAnyOrder(resource1, resource2);
    }

    @Test
    void returnsEmptyListForProductWithoutResources() {
        // given
        ProductIdentifier productId = ProductIdentifier.random();
        InventoryProduct laptop = InventoryProduct.individuallyTracked(productId, "MacBook Pro 16");
        facade.handle(CreateInventoryEntry.forProduct(laptop));
        // no resources mapped

        // when
        List<ResourceId> resources = facade.findResourcesForProduct(productId);

        // then
        assertThat(resources).isEmpty();
    }

    @Test
    void returnsEmptyListForUnknownProduct() {
        // when
        List<ResourceId> resources = facade.findResourcesForProduct(ProductIdentifier.random());

        // then
        assertThat(resources).isEmpty();
    }

    // === Duplicate entry prevention ===

    @Test
    void failsToCreateDuplicateEntryForSameProduct() {
        // given
        ProductIdentifier productId = ProductIdentifier.random();
        InventoryProduct laptop = InventoryProduct.individuallyTracked(productId, "MacBook Pro 16");
        facade.handle(CreateInventoryEntry.forProduct(laptop));

        // when - try to create another entry for same product
        Result<String, InventoryEntryId> result =
                facade.handle(CreateInventoryEntry.forProduct(laptop));

        // then
        assertThat(result.failure()).isTrue();
        assertThat(result.getFailure()).contains("already exists");
    }

    // === Error handling ===

    @Test
    void failsToMapResourceForNonexistentEntry() {
        // when
        Result<String, InventoryEntryId> result =
                facade.mapInstanceToResource(
                        InventoryEntryId.random(), InstanceId.random(), ResourceId.random());

        // then
        assertThat(result.failure()).isTrue();
        assertThat(result.getFailure()).contains("Entry not found");
    }

    @Test
    void failsToRemoveInstanceFromNonexistentEntry() {
        // when
        Result<String, InventoryEntryId> result =
                facade.removeInstanceFromEntry(InventoryEntryId.random(), InstanceId.random());

        // then
        assertThat(result.failure()).isTrue();
        assertThat(result.getFailure()).contains("Entry not found");
    }

    // === LockCommand handling tests ===

    @Test
    void handlesIndividualLockCommand() {
        // given - product with instance mapped to individual resource
        ProductIdentifier productId = ProductIdentifier.random();
        InventoryProduct laptop = InventoryProduct.individuallyTracked(productId, "MacBook Pro");
        InventoryEntryId entryId =
                facade.handle(CreateInventoryEntry.forProduct(laptop)).getSuccess();

        InstanceId instanceId = InstanceId.random();
        ResourceId resourceId = ResourceId.random();
        availabilityFixture.registerIndividual(resourceId);
        facade.mapInstanceToResource(entryId, instanceId, resourceId);

        OwnerId owner = OwnerId.random();

        // when
        LockCommand cmd =
                new LockCommand(
                        productId,
                        Quantity.of(1, Unit.pieces()),
                        owner,
                        ResourceSpecification.IndividualSpecification.of(instanceId));
        Result<String, List<BlockadeId>> result = facade.handle(cmd);

        // then
        assertThat(result.success()).isTrue();
        assertThat(result.getSuccess()).hasSize(1);
    }

    @Test
    void handlesPoolLockCommand() {
        // given - product with pool resource (e.g., fuel tank)
        ProductIdentifier productId = ProductIdentifier.random();
        InventoryProduct fuel = InventoryProduct.identical(productId, "Diesel");
        InventoryEntryId entryId =
                facade.handle(CreateInventoryEntry.forProduct(fuel)).getSuccess();

        InstanceId instanceId = InstanceId.random();
        ResourceId resourceId = ResourceId.random();
        availabilityFixture.registerPool(resourceId, Quantity.of(10000, Unit.liters()));
        facade.mapInstanceToResource(entryId, instanceId, resourceId);

        OwnerId owner = OwnerId.random();

        // when
        LockCommand cmd =
                new LockCommand(
                        productId,
                        Quantity.of(500, Unit.liters()),
                        owner,
                        ResourceSpecification.QuantitySpecification.instance());
        Result<String, List<BlockadeId>> result = facade.handle(cmd);

        // then
        assertThat(result.success()).isTrue();
        assertThat(result.getSuccess()).hasSize(1);
    }

    @Test
    void handlesTemporalLockCommand() {
        // given - product with temporal resource (e.g., hotel room)
        ProductIdentifier productId = ProductIdentifier.random();
        InventoryProduct room = InventoryProduct.individuallyTracked(productId, "Deluxe Room");
        InventoryEntryId entryId =
                facade.handle(CreateInventoryEntry.forProduct(room)).getSuccess();

        InstanceId instanceId = InstanceId.random();
        ResourceId resourceId = ResourceId.random();
        TimeSlot june15 = TimeSlot.ofDay(LocalDate.of(2024, 6, 15));
        availabilityFixture.registerTemporalSlot(resourceId, june15);
        facade.mapInstanceToResource(entryId, instanceId, resourceId);

        OwnerId owner = OwnerId.random();

        // when
        LockCommand cmd =
                new LockCommand(
                        productId,
                        Quantity.of(1, Unit.pieces()),
                        owner,
                        ResourceSpecification.TemporalSpecification.of(june15));
        Result<String, List<BlockadeId>> result = facade.handle(cmd);

        // then
        assertThat(result.success()).isTrue();
        assertThat(result.getSuccess()).hasSize(1);
    }

    @Test
    void handlesMultiSlotTemporalLockCommand() {
        // given - room available for 3 nights
        ProductIdentifier productId = ProductIdentifier.random();
        InventoryProduct room = InventoryProduct.individuallyTracked(productId, "Deluxe Room");
        InventoryEntryId entryId =
                facade.handle(CreateInventoryEntry.forProduct(room)).getSuccess();

        InstanceId instanceId = InstanceId.random();
        ResourceId resourceId = ResourceId.random();
        LocalDate checkIn = LocalDate.of(2024, 6, 15);
        TimeSlot night1 = TimeSlot.ofDay(checkIn);
        TimeSlot night2 = TimeSlot.ofDay(checkIn.plusDays(1));
        TimeSlot night3 = TimeSlot.ofDay(checkIn.plusDays(2));
        availabilityFixture.registerTemporalSlot(resourceId, night1);
        availabilityFixture.registerTemporalSlot(resourceId, night2);
        availabilityFixture.registerTemporalSlot(resourceId, night3);
        facade.mapInstanceToResource(entryId, instanceId, resourceId);

        OwnerId owner = OwnerId.random();

        // when - lock all 3 nights
        LockCommand cmd =
                new LockCommand(
                        productId,
                        Quantity.of(1, Unit.pieces()),
                        owner,
                        ResourceSpecification.TemporalSpecification.of(
                                List.of(night1, night2, night3)));
        Result<String, List<BlockadeId>> result = facade.handle(cmd);

        // then
        assertThat(result.success()).isTrue();
        assertThat(result.getSuccess()).hasSize(3);
    }

    @Test
    void failsLockWhenResourceUnavailable() {
        // given - individual resource already locked
        ProductIdentifier productId = ProductIdentifier.random();
        InventoryProduct laptop = InventoryProduct.individuallyTracked(productId, "MacBook Pro");
        InventoryEntryId entryId =
                facade.handle(CreateInventoryEntry.forProduct(laptop)).getSuccess();

        InstanceId instanceId = InstanceId.random();
        ResourceId resourceId = ResourceId.random();
        availabilityFixture.registerIndividual(resourceId);
        facade.mapInstanceToResource(entryId, instanceId, resourceId);

        OwnerId alice = OwnerId.random();
        OwnerId bob = OwnerId.random();

        // Alice locks first
        facade.handle(
                new LockCommand(
                        productId,
                        Quantity.of(1, Unit.pieces()),
                        alice,
                        ResourceSpecification.IndividualSpecification.of(instanceId)));

        // when - Bob tries to lock
        Result<String, List<BlockadeId>> result =
                facade.handle(
                        new LockCommand(
                                productId,
                                Quantity.of(1, Unit.pieces()),
                                bob,
                                ResourceSpecification.IndividualSpecification.of(instanceId)));

        // then
        assertThat(result.failure()).isTrue();
    }

    @Test
    void failsLockWhenNoResourceMapped() {
        // given - product exists but no resource mapped
        ProductIdentifier productId = ProductIdentifier.random();
        InventoryProduct laptop = InventoryProduct.individuallyTracked(productId, "MacBook Pro");
        facade.handle(CreateInventoryEntry.forProduct(laptop));

        InstanceId instanceId = InstanceId.random(); // not mapped to any resource

        // when
        LockCommand cmd =
                new LockCommand(
                        productId,
                        Quantity.of(1, Unit.pieces()),
                        OwnerId.random(),
                        ResourceSpecification.IndividualSpecification.of(instanceId));
        Result<String, List<BlockadeId>> result = facade.handle(cmd);

        // then
        assertThat(result.failure()).isTrue();
        assertThat(result.getFailure()).contains("No resource mapped");
    }
}
