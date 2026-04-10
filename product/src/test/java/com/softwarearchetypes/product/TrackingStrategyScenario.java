package com.softwarearchetypes.product;

import static org.junit.jupiter.api.Assertions.*;

import com.softwarearchetypes.quantity.Quantity;
import com.softwarearchetypes.quantity.Unit;
import org.junit.jupiter.api.Test;

/**
 * Scenario from lesson: ProductTrackingStrategy examples
 *
 * <p>Demonstrates how different tracking strategies affect product instances:
 *
 * <p>- UNIQUE: One-of-a-kind products (Hetfield's guitar, da Vinci's painting) -
 * INDIVIDUALLY_TRACKED: Each instance uniquely identified (iPhone, parcel) - BATCH_TRACKED: Tracked
 * by production batch (milk, pharmaceuticals) - INDIVIDUALLY_AND_BATCH_TRACKED: Both tracking
 * methods (TV, smartphone) - IDENTICAL: Interchangeable items (screws, rice)
 */
class TrackingStrategyScenario {

    @Test
    void shouldTrackUniqueProduct() {
        // UNIQUE: Hetfield's 1968 Gibson Explorer "EET FUK"
        ProductType hetfieldsGuitar =
                Product.builder(
                                UuidProductIdentifier.random(),
                                ProductName.of("Hetfield's EET FUK Guitar"),
                                ProductDescription.of("1968 Gibson Explorer - one of a kind"))
                        .asProductType(Unit.pieces(), ProductTrackingStrategy.UNIQUE)
                        .build();

        // Unique product requires serial number (it's unique!)
        ProductInstance guitarInstance =
                new InstanceBuilder(InstanceId.newOne())
                        .withSerial(SerialNumber.of("HETFIELD-EET-FUK-1968"))
                        .asProductInstance(hetfieldsGuitar)
                        .withQuantity(Quantity.of(1, Unit.pieces()))
                        .build();

        assertNotNull(guitarInstance);
        assertTrue(guitarInstance.serialNumber().isPresent());
        assertEquals("HETFIELD-EET-FUK-1968", guitarInstance.serialNumber().get().toString());

        // Strategy checks
        assertTrue(hetfieldsGuitar.trackingStrategy().isTrackedIndividually());
        assertFalse(hetfieldsGuitar.trackingStrategy().isTrackedByBatch());
    }

    @Test
    void shouldTrackIndividuallyTrackedProduct() {
        // INDIVIDUALLY_TRACKED: iPhone 15 Pro
        ProductType iphone =
                Product.builder(
                                UuidProductIdentifier.random(),
                                ProductName.of("iPhone 15 Pro"),
                                ProductDescription.of("256GB Space Gray"))
                        .asProductType(Unit.pieces(), ProductTrackingStrategy.INDIVIDUALLY_TRACKED)
                        .build();

        // Each iPhone has unique serial number
        ProductInstance iphone1 =
                new InstanceBuilder(InstanceId.newOne())
                        .withSerial(SerialNumber.of("IPHONE-123456789"))
                        .asProductInstance(iphone)
                        .withQuantity(Quantity.of(1, Unit.pieces()))
                        .build();

        ProductInstance iphone2 =
                new InstanceBuilder(InstanceId.newOne())
                        .withSerial(SerialNumber.of("IPHONE-987654321"))
                        .asProductInstance(iphone)
                        .withQuantity(Quantity.of(1, Unit.pieces()))
                        .build();

        // Different serial numbers, same product type
        assertNotEquals(iphone1.serialNumber().get(), iphone2.serialNumber().get());
        assertEquals(iphone, iphone1.product());
        assertEquals(iphone, iphone2.product());

        // Strategy checks
        assertTrue(iphone.trackingStrategy().isTrackedIndividually());
        assertFalse(iphone.trackingStrategy().isTrackedByBatch());
    }

    @Test
    void shouldTrackBatchTrackedProduct() {
        // BATCH_TRACKED: Milk bottles
        ProductType milk =
                Product.builder(
                                UuidProductIdentifier.random(),
                                ProductName.of("Fresh Milk"),
                                ProductDescription.of("1L whole milk"))
                        .asProductType(Unit.liters(), ProductTrackingStrategy.BATCH_TRACKED)
                        .build();

        // Milk tracked by production batch (for quality control and recalls)
        ProductInstance milkBatch1 =
                new InstanceBuilder(InstanceId.newOne())
                        .withBatch(BatchId.newOne())
                        .asProductInstance(milk)
                        .withQuantity(Quantity.of(100, Unit.liters()))
                        .build();

        assertNotNull(milkBatch1);
        assertTrue(milkBatch1.batchId().isPresent());
        assertFalse(
                milkBatch1.serialNumber().isPresent(),
                "Batch tracked products don't need serial numbers");

        // Strategy checks
        assertFalse(milk.trackingStrategy().isTrackedIndividually());
        assertTrue(milk.trackingStrategy().isTrackedByBatch());
    }

    @Test
    void shouldTrackProductWithBothMethods() {
        // INDIVIDUALLY_AND_BATCH_TRACKED: TV (for warranty and recalls)
        ProductType tv =
                Product.builder(
                                UuidProductIdentifier.random(),
                                ProductName.of("Samsung QLED 65\""),
                                ProductDescription.of("4K Smart TV"))
                        .asProductType(
                                Unit.pieces(),
                                ProductTrackingStrategy.INDIVIDUALLY_AND_BATCH_TRACKED)
                        .build();

        // TV has both serial (for warranty) and batch (for recalls)
        ProductInstance tvInstance =
                new InstanceBuilder(InstanceId.newOne())
                        .withSerial(SerialNumber.of("TV-SERIAL-123"))
                        .withBatch(BatchId.newOne())
                        .asProductInstance(tv)
                        .withQuantity(Quantity.of(1, Unit.pieces()))
                        .build();

        assertTrue(tvInstance.serialNumber().isPresent(), "Should have serial for warranty");
        assertTrue(tvInstance.batchId().isPresent(), "Should have batch for recalls");

        // Strategy checks
        assertTrue(tv.trackingStrategy().isTrackedIndividually());
        assertTrue(tv.trackingStrategy().isTrackedByBatch());
        assertTrue(tv.trackingStrategy().requiresBothTrackingMethods());
    }

    @Test
    void shouldTrackIdenticalProduct() {
        // IDENTICAL: Screws (interchangeable, bulk items)
        ProductType screws =
                Product.builder(
                                UuidProductIdentifier.random(),
                                ProductName.of("M6x20 Screws"),
                                ProductDescription.of("Stainless steel screws"))
                        .asProductType(Unit.pieces(), ProductTrackingStrategy.IDENTICAL)
                        .build();

        // Identical products are interchangeable - may not need instances at all
        // But if we create instances, they don't need serial or batch
        ProductInstance screwsBox =
                new InstanceBuilder(InstanceId.newOne())
                        .asProductInstance(screws)
                        .withQuantity(Quantity.of(1000, Unit.pieces()))
                        .build();

        assertFalse(
                screwsBox.serialNumber().isPresent(),
                "Identical products don't need serial numbers");
        assertFalse(screwsBox.batchId().isPresent(), "Identical products don't need batch IDs");

        // Strategy checks
        assertFalse(screws.trackingStrategy().isTrackedIndividually());
        assertFalse(screws.trackingStrategy().isTrackedByBatch());
        assertTrue(screws.trackingStrategy().isInterchangeable());
    }

    @Test
    void shouldEnforceTrackingRequirementsForIndividuallyTracked() {
        ProductType laptop =
                Product.builder(
                                UuidProductIdentifier.random(),
                                ProductName.of("MacBook Pro"),
                                ProductDescription.of("16-inch M3 Max"))
                        .asProductType(Unit.pieces(), ProductTrackingStrategy.INDIVIDUALLY_TRACKED)
                        .build();

        // Should fail: individually tracked requires serial number
        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    new InstanceBuilder(InstanceId.newOne())
                            .asProductInstance(laptop)
                            .withQuantity(Quantity.of(1, Unit.pieces()))
                            .build();
                },
                "Should require serial number for individually tracked products");
    }

    @Test
    void shouldEnforceTrackingRequirementsForBatchTracked() {
        ProductType medicine =
                Product.builder(
                                UuidProductIdentifier.random(),
                                ProductName.of("Aspirin 500mg"),
                                ProductDescription.of("Pain reliever"))
                        .asProductType(Unit.pieces(), ProductTrackingStrategy.BATCH_TRACKED)
                        .build();

        // Should fail: batch tracked requires batch ID
        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    new InstanceBuilder(InstanceId.newOne())
                            .asProductInstance(medicine)
                            .withQuantity(Quantity.of(100, Unit.pieces()))
                            .build();
                },
                "Should require batch ID for batch tracked products");
    }

    @Test
    void shouldEnforceTrackingRequirementsForBothMethods() {
        ProductType smartphone =
                Product.builder(
                                UuidProductIdentifier.random(),
                                ProductName.of("Samsung Galaxy S24"),
                                ProductDescription.of("256GB Phantom Black"))
                        .asProductType(
                                Unit.pieces(),
                                ProductTrackingStrategy.INDIVIDUALLY_AND_BATCH_TRACKED)
                        .build();

        // Should fail: missing serial
        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    new InstanceBuilder(InstanceId.newOne())
                            .withBatch(BatchId.newOne())
                            .asProductInstance(smartphone)
                            .withQuantity(Quantity.of(1, Unit.pieces()))
                            .build();
                },
                "Should require both serial and batch");

        // Should fail: missing batch
        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    new InstanceBuilder(InstanceId.newOne())
                            .withSerial(SerialNumber.of("PHONE-123"))
                            .asProductInstance(smartphone)
                            .withQuantity(Quantity.of(1, Unit.pieces()))
                            .build();
                },
                "Should require both serial and batch");
    }

    @Test
    void shouldNotAllowTrackingForIdenticalProducts() {
        ProductType rice =
                Product.builder(
                                UuidProductIdentifier.random(),
                                ProductName.of("Basmati Rice"),
                                ProductDescription.of("Premium long grain"))
                        .asProductType(Unit.kilograms(), ProductTrackingStrategy.IDENTICAL)
                        .build();

        // Identical products can exist without any tracking
        ProductInstance riceBag =
                new InstanceBuilder(InstanceId.newOne())
                        .asProductInstance(rice)
                        .withQuantity(Quantity.of(25, Unit.kilograms()))
                        .build();

        assertNotNull(riceBag);
        assertFalse(
                riceBag.serialNumber().isPresent(),
                "Identical products must not have serial numbers");
        assertFalse(riceBag.batchId().isPresent(), "Identical products must not have batch IDs");

        // Should fail: trying to add batch to identical product
        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    new InstanceBuilder(InstanceId.newOne())
                            .withBatch(BatchId.newOne())
                            .asProductInstance(rice)
                            .withQuantity(Quantity.of(25, Unit.kilograms()))
                            .build();
                },
                "IDENTICAL products cannot have batch tracking");

        // Should fail: trying to add serial to identical product
        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    new InstanceBuilder(InstanceId.newOne())
                            .withSerial(SerialNumber.of("RICE-001"))
                            .asProductInstance(rice)
                            .withQuantity(Quantity.of(25, Unit.kilograms()))
                            .build();
                },
                "IDENTICAL products cannot have serial numbers");
    }
}
