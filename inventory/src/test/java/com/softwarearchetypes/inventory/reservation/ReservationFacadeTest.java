package com.softwarearchetypes.inventory.reservation;

import static org.assertj.core.api.Assertions.assertThat;

import com.softwarearchetypes.common.Result;
import com.softwarearchetypes.inventory.CreateInventoryEntry;
import com.softwarearchetypes.inventory.InstanceId;
import com.softwarearchetypes.inventory.InventoryConfiguration;
import com.softwarearchetypes.inventory.InventoryFacade;
import com.softwarearchetypes.inventory.InventoryProduct;
import com.softwarearchetypes.inventory.ProductIdentifier;
import com.softwarearchetypes.inventory.ResourceSpecification;
import com.softwarearchetypes.inventory.availability.AvailabilityConfiguration;
import com.softwarearchetypes.inventory.availability.AvailabilityFixture;
import com.softwarearchetypes.inventory.availability.OwnerId;
import com.softwarearchetypes.inventory.availability.ResourceId;
import com.softwarearchetypes.quantity.Quantity;
import com.softwarearchetypes.quantity.Unit;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ReservationFacadeTest {

    private static final OwnerId GUEST_ALICE = OwnerId.random();
    private static final OwnerId GUEST_BOB = OwnerId.random();

    private Clock clock = Clock.fixed(Instant.parse("2024-01-15T10:00:00Z"), ZoneId.of("UTC"));
    private AvailabilityConfiguration availabilityConfig =
            AvailabilityConfiguration.inMemory(clock);
    private InventoryConfiguration inventoryConfig =
            InventoryConfiguration.inMemory(availabilityConfig);
    private ReservationConfiguration reservationConfig =
            ReservationConfiguration.inMemory(inventoryConfig, availabilityConfig, clock);
    private ReservationFacade reservationFacade = reservationConfig.facade();
    private InventoryFacade inventoryFacade = inventoryConfig.facade();
    private AvailabilityFixture availabilityFixture =
            new AvailabilityFixture(availabilityConfig.facade(), clock);

    @Test
    void createsReservationWhenResourceAvailable() {
        // given - product with instance mapped to resource
        ProductIdentifier productId = ProductIdentifier.random();
        InstanceId instanceId = setupProductWithResource(productId, "Laptop");

        // when
        ReserveRequest request =
                ReserveRequest.forProduct(productId)
                        .quantity(Quantity.of(1, Unit.pieces()))
                        .owner(GUEST_ALICE)
                        .purpose(ReservationPurpose.BOOKING)
                        .resourceSpecification(
                                ResourceSpecification.IndividualSpecification.of(instanceId))
                        .build();

        Result<String, ReservationId> result = reservationFacade.handle(request);

        // then
        assertThat(result.success()).isTrue();
        ReservationId reservationId = result.getSuccess();
        Optional<ReservationView> view = reservationFacade.findById(reservationId);
        assertThat(view).isPresent();
        assertThat(view.get().owner()).isEqualTo(GUEST_ALICE);
        assertThat(view.get().status()).isEqualTo("CONFIRMED");
        assertThat(view.get().purpose()).isEqualTo(ReservationPurpose.BOOKING);
    }

    @Test
    void failsReservationWhenResourceUnavailable() {
        // given
        ProductIdentifier productId = ProductIdentifier.random();
        InstanceId instanceId = setupProductWithResource(productId, "Laptop");

        // Alice reserves first
        reservationFacade.handle(
                ReserveRequest.forProduct(productId)
                        .quantity(Quantity.of(1, Unit.pieces()))
                        .owner(GUEST_ALICE)
                        .purpose(ReservationPurpose.BOOKING)
                        .resourceSpecification(
                                ResourceSpecification.IndividualSpecification.of(instanceId))
                        .build());

        // when - Bob tries to reserve same resource
        Result<String, ReservationId> result =
                reservationFacade.handle(
                        ReserveRequest.forProduct(productId)
                                .quantity(Quantity.of(1, Unit.pieces()))
                                .owner(GUEST_BOB)
                                .purpose(ReservationPurpose.BOOKING)
                                .resourceSpecification(
                                        ResourceSpecification.IndividualSpecification.of(
                                                instanceId))
                                .build());

        // then
        assertThat(result.failure()).isTrue();
    }

    @Test
    void ownerCanCancelReservation() {
        // given
        ProductIdentifier productId = ProductIdentifier.random();
        InstanceId instanceId = setupProductWithResource(productId, "Laptop");

        Result<String, ReservationId> reserveResult =
                reservationFacade.handle(
                        ReserveRequest.forProduct(productId)
                                .quantity(Quantity.of(1, Unit.pieces()))
                                .owner(GUEST_ALICE)
                                .purpose(ReservationPurpose.BOOKING)
                                .resourceSpecification(
                                        ResourceSpecification.IndividualSpecification.of(
                                                instanceId))
                                .build());
        ReservationId reservationId = reserveResult.getSuccess();

        // when
        Result<String, ReservationId> cancelResult =
                reservationFacade.cancel(reservationId, GUEST_ALICE);

        // then
        assertThat(cancelResult.success()).isTrue();
        Optional<ReservationView> cancelled = reservationFacade.findById(reservationId);
        assertThat(cancelled).isPresent();
        assertThat(cancelled.get().status()).isEqualTo("CANCELLED");
    }

    @Test
    void cancellationReleasesResource() {
        // given
        ProductIdentifier productId = ProductIdentifier.random();
        InstanceId instanceId = setupProductWithResource(productId, "Laptop");

        Result<String, ReservationId> reserveResult =
                reservationFacade.handle(
                        ReserveRequest.forProduct(productId)
                                .quantity(Quantity.of(1, Unit.pieces()))
                                .owner(GUEST_ALICE)
                                .purpose(ReservationPurpose.BOOKING)
                                .resourceSpecification(
                                        ResourceSpecification.IndividualSpecification.of(
                                                instanceId))
                                .build());
        ReservationId reservationId = reserveResult.getSuccess();

        // when
        reservationFacade.cancel(reservationId, GUEST_ALICE);

        // then - Bob can now reserve
        Result<String, ReservationId> bobResult =
                reservationFacade.handle(
                        ReserveRequest.forProduct(productId)
                                .quantity(Quantity.of(1, Unit.pieces()))
                                .owner(GUEST_BOB)
                                .purpose(ReservationPurpose.BOOKING)
                                .resourceSpecification(
                                        ResourceSpecification.IndividualSpecification.of(
                                                instanceId))
                                .build());
        assertThat(bobResult.success()).isTrue();
    }

    @Test
    void nonOwnerCannotCancelReservation() {
        // given
        ProductIdentifier productId = ProductIdentifier.random();
        InstanceId instanceId = setupProductWithResource(productId, "Laptop");

        Result<String, ReservationId> reserveResult =
                reservationFacade.handle(
                        ReserveRequest.forProduct(productId)
                                .quantity(Quantity.of(1, Unit.pieces()))
                                .owner(GUEST_ALICE)
                                .purpose(ReservationPurpose.BOOKING)
                                .resourceSpecification(
                                        ResourceSpecification.IndividualSpecification.of(
                                                instanceId))
                                .build());
        ReservationId reservationId = reserveResult.getSuccess();

        // when - Bob tries to cancel Alice's reservation
        Result<String, ReservationId> cancelResult =
                reservationFacade.cancel(reservationId, GUEST_BOB);

        // then
        assertThat(cancelResult.failure()).isTrue();
        assertThat(cancelResult.getFailure()).contains("Not authorized");
    }

    @Test
    void findsReservationsByOwner() {
        // given
        ProductIdentifier productId1 = ProductIdentifier.random();
        ProductIdentifier productId2 = ProductIdentifier.random();
        InstanceId instanceId1 = setupProductWithResource(productId1, "Laptop 1");
        InstanceId instanceId2 = setupProductWithResource(productId2, "Laptop 2");

        reservationFacade.handle(
                ReserveRequest.forProduct(productId1)
                        .quantity(Quantity.of(1, Unit.pieces()))
                        .owner(GUEST_ALICE)
                        .purpose(ReservationPurpose.BOOKING)
                        .resourceSpecification(
                                ResourceSpecification.IndividualSpecification.of(instanceId1))
                        .build());
        reservationFacade.handle(
                ReserveRequest.forProduct(productId2)
                        .quantity(Quantity.of(1, Unit.pieces()))
                        .owner(GUEST_ALICE)
                        .purpose(ReservationPurpose.BOOKING)
                        .resourceSpecification(
                                ResourceSpecification.IndividualSpecification.of(instanceId2))
                        .build());

        // when
        List<ReservationView> aliceReservations = reservationFacade.findByOwner(GUEST_ALICE);

        // then
        assertThat(aliceReservations).hasSize(2);
        assertThat(aliceReservations).allMatch(r -> r.owner().equals(GUEST_ALICE));
    }

    @Test
    void findsActiveReservations() {
        // given
        ProductIdentifier productId1 = ProductIdentifier.random();
        ProductIdentifier productId2 = ProductIdentifier.random();
        InstanceId instanceId1 = setupProductWithResource(productId1, "Laptop 1");
        InstanceId instanceId2 = setupProductWithResource(productId2, "Laptop 2");

        Result<String, ReservationId> res1 =
                reservationFacade.handle(
                        ReserveRequest.forProduct(productId1)
                                .quantity(Quantity.of(1, Unit.pieces()))
                                .owner(GUEST_ALICE)
                                .purpose(ReservationPurpose.BOOKING)
                                .resourceSpecification(
                                        ResourceSpecification.IndividualSpecification.of(
                                                instanceId1))
                                .build());
        reservationFacade.handle(
                ReserveRequest.forProduct(productId2)
                        .quantity(Quantity.of(1, Unit.pieces()))
                        .owner(GUEST_BOB)
                        .purpose(ReservationPurpose.BOOKING)
                        .resourceSpecification(
                                ResourceSpecification.IndividualSpecification.of(instanceId2))
                        .build());

        // Alice cancels her reservation
        reservationFacade.cancel(res1.getSuccess(), GUEST_ALICE);

        // when
        List<ReservationView> activeReservations = reservationFacade.findActive();

        // then
        assertThat(activeReservations).hasSize(1);
        assertThat(activeReservations.get(0).owner()).isEqualTo(GUEST_BOB);
    }

    private InstanceId setupProductWithResource(ProductIdentifier productId, String name) {
        InventoryProduct product = InventoryProduct.individuallyTracked(productId, name);
        var entryId = inventoryFacade.handle(CreateInventoryEntry.forProduct(product)).getSuccess();

        ResourceId resourceId = ResourceId.random();
        availabilityFixture.registerIndividual(resourceId);

        InstanceId instanceId = InstanceId.random();
        inventoryFacade.mapInstanceToResource(entryId, instanceId, resourceId);

        return instanceId;
    }
}
