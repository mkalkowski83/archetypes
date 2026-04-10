package com.softwarearchetypes.inventory.reservation;

import static org.assertj.core.api.Assertions.assertThat;

import com.softwarearchetypes.common.Result;
import com.softwarearchetypes.inventory.CreateInventoryEntry;
import com.softwarearchetypes.inventory.InstanceId;
import com.softwarearchetypes.inventory.InventoryConfiguration;
import com.softwarearchetypes.inventory.InventoryEntryId;
import com.softwarearchetypes.inventory.InventoryFacade;
import com.softwarearchetypes.inventory.InventoryProduct;
import com.softwarearchetypes.inventory.ProductIdentifier;
import com.softwarearchetypes.inventory.ResourceSpecification;
import com.softwarearchetypes.inventory.availability.AvailabilityConfiguration;
import com.softwarearchetypes.inventory.availability.AvailabilityFixture;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Reservation scenarios for temporal resources. Domain: Hotel room reservations with nightly slots.
 */
@DisplayName("Hotel Reservation Scenarios (Temporal)")
class HotelReservationScenarios {

    private static final ZoneId ZONE = ZoneId.of("Europe/Warsaw");
    private static final LocalDate CHECK_IN = LocalDate.of(2024, 6, 15);

    private static final OwnerId GUEST_ANNA = OwnerId.random();
    private static final OwnerId GUEST_TOMEK = OwnerId.random();

    private Clock clock;
    private InventoryFacade inventoryFacade;
    private ReservationFacade reservationFacade;
    private AvailabilityFixture availabilityFixture;

    @BeforeEach
    void setup() {
        clock = Clock.fixed(Instant.parse("2024-06-01T10:00:00Z"), ZONE);
        AvailabilityConfiguration availabilityConfig = AvailabilityConfiguration.inMemory(clock);
        InventoryConfiguration inventoryConfig =
                InventoryConfiguration.inMemory(availabilityConfig);
        ReservationConfiguration reservationConfig =
                ReservationConfiguration.inMemory(inventoryConfig, availabilityConfig, clock);

        inventoryFacade = inventoryConfig.facade();
        reservationFacade = reservationConfig.facade();
        availabilityFixture = new AvailabilityFixture(availabilityConfig.facade(), clock);
    }

    @Nested
    @DisplayName("Single night reservations")
    class SingleNightReservations {

        @Test
        @DisplayName("Guest can reserve room for one night")
        void guestCanReserveRoomForOneNight() {
            // given - Room 101 available for June 15
            ProductIdentifier roomType = ProductIdentifier.random();
            ResourceId room101 = setupRoomWithSlots(roomType, "Pokój Deluxe", CHECK_IN, 1);

            TimeSlot june15 = TimeSlot.ofDay(CHECK_IN);

            // when - Anna reserves for one night
            Result<String, ReservationId> result =
                    reservationFacade.handle(
                            ReserveRequest.forProduct(roomType)
                                    .quantity(Quantity.of(1, Unit.pieces()))
                                    .owner(GUEST_ANNA)
                                    .purpose(ReservationPurpose.BOOKING)
                                    .resourceSpecification(
                                            ResourceSpecification.TemporalSpecification.of(june15))
                                    .build());

            // then
            assertThat(result.success()).isTrue();
            Optional<ReservationView> view = reservationFacade.findById(result.getSuccess());
            assertThat(view).isPresent();
            assertThat(view.get().status()).isEqualTo("CONFIRMED");
        }

        @Test
        @DisplayName("Second guest cannot reserve same room for same night")
        void secondGuestCannotReserveSameRoomSameNight() {
            // given - Room 101, Anna already reserved June 15
            ProductIdentifier roomType = ProductIdentifier.random();
            setupRoomWithSlots(roomType, "Pokój Deluxe", CHECK_IN, 1);

            TimeSlot june15 = TimeSlot.ofDay(CHECK_IN);
            reservationFacade.handle(
                    ReserveRequest.forProduct(roomType)
                            .quantity(Quantity.of(1, Unit.pieces()))
                            .owner(GUEST_ANNA)
                            .purpose(ReservationPurpose.BOOKING)
                            .resourceSpecification(
                                    ResourceSpecification.TemporalSpecification.of(june15))
                            .build());

            // when - Tomek tries to reserve same night
            Result<String, ReservationId> result =
                    reservationFacade.handle(
                            ReserveRequest.forProduct(roomType)
                                    .quantity(Quantity.of(1, Unit.pieces()))
                                    .owner(GUEST_TOMEK)
                                    .purpose(ReservationPurpose.BOOKING)
                                    .resourceSpecification(
                                            ResourceSpecification.TemporalSpecification.of(june15))
                                    .build());

            // then
            assertThat(result.failure()).isTrue();
        }
    }

    @Nested
    @DisplayName("Multi-night reservations")
    class MultiNightReservations {

        @Test
        @DisplayName("Guest can reserve room for multiple consecutive nights")
        void guestCanReserveMultipleNights() {
            // given - Room 101 available for June 15-17 (3 nights)
            ProductIdentifier roomType = ProductIdentifier.random();
            setupRoomWithSlots(roomType, "Pokój Deluxe", CHECK_IN, 3);

            List<TimeSlot> threeNights =
                    List.of(
                            TimeSlot.ofDay(CHECK_IN),
                            TimeSlot.ofDay(CHECK_IN.plusDays(1)),
                            TimeSlot.ofDay(CHECK_IN.plusDays(2)));

            // when - Anna reserves for 3 nights
            Result<String, ReservationId> result =
                    reservationFacade.handle(
                            ReserveRequest.forProduct(roomType)
                                    .quantity(Quantity.of(1, Unit.pieces()))
                                    .owner(GUEST_ANNA)
                                    .purpose(ReservationPurpose.BOOKING)
                                    .resourceSpecification(
                                            ResourceSpecification.TemporalSpecification.of(
                                                    threeNights))
                                    .build());

            // then
            assertThat(result.success()).isTrue();
        }

        @Test
        @DisplayName("Reservation fails if any night in range is taken")
        void reservationFailsIfAnyNightTaken() {
            // given - Room 101, Anna reserved June 16
            ProductIdentifier roomType = ProductIdentifier.random();
            setupRoomWithSlots(roomType, "Pokój Deluxe", CHECK_IN, 3);

            TimeSlot june16 = TimeSlot.ofDay(CHECK_IN.plusDays(1));
            reservationFacade.handle(
                    ReserveRequest.forProduct(roomType)
                            .quantity(Quantity.of(1, Unit.pieces()))
                            .owner(GUEST_ANNA)
                            .purpose(ReservationPurpose.BOOKING)
                            .resourceSpecification(
                                    ResourceSpecification.TemporalSpecification.of(june16))
                            .build());

            // when - Tomek tries to reserve June 15-17 (overlaps with Anna's June 16)
            List<TimeSlot> threeNights =
                    List.of(
                            TimeSlot.ofDay(CHECK_IN),
                            TimeSlot.ofDay(CHECK_IN.plusDays(1)),
                            TimeSlot.ofDay(CHECK_IN.plusDays(2)));
            Result<String, ReservationId> result =
                    reservationFacade.handle(
                            ReserveRequest.forProduct(roomType)
                                    .quantity(Quantity.of(1, Unit.pieces()))
                                    .owner(GUEST_TOMEK)
                                    .purpose(ReservationPurpose.BOOKING)
                                    .resourceSpecification(
                                            ResourceSpecification.TemporalSpecification.of(
                                                    threeNights))
                                    .build());

            // then
            assertThat(result.failure()).isTrue();
        }
    }

    @Nested
    @DisplayName("Cancellation scenarios")
    class CancellationScenarios {

        @Test
        @DisplayName("Guest can cancel and room becomes available")
        void guestCanCancelAndRoomBecomesAvailable() {
            // given - Anna reserved June 15
            ProductIdentifier roomType = ProductIdentifier.random();
            setupRoomWithSlots(roomType, "Pokój Deluxe", CHECK_IN, 1);

            TimeSlot june15 = TimeSlot.ofDay(CHECK_IN);
            ReservationId annaReservation =
                    reservationFacade
                            .handle(
                                    ReserveRequest.forProduct(roomType)
                                            .quantity(Quantity.of(1, Unit.pieces()))
                                            .owner(GUEST_ANNA)
                                            .purpose(ReservationPurpose.BOOKING)
                                            .resourceSpecification(
                                                    ResourceSpecification.TemporalSpecification.of(
                                                            june15))
                                            .build())
                            .getSuccess();

            // when - Anna cancels
            reservationFacade.cancel(annaReservation, GUEST_ANNA);

            // then - Tomek can now reserve
            Result<String, ReservationId> tomekResult =
                    reservationFacade.handle(
                            ReserveRequest.forProduct(roomType)
                                    .quantity(Quantity.of(1, Unit.pieces()))
                                    .owner(GUEST_TOMEK)
                                    .purpose(ReservationPurpose.BOOKING)
                                    .resourceSpecification(
                                            ResourceSpecification.TemporalSpecification.of(june15))
                                    .build());
            assertThat(tomekResult.success()).isTrue();
        }
    }

    private ResourceId setupRoomWithSlots(
            ProductIdentifier productId, String name, LocalDate startDate, int nights) {
        InventoryProduct product = InventoryProduct.individuallyTracked(productId, name);
        InventoryEntryId entryId =
                inventoryFacade.handle(CreateInventoryEntry.forProduct(product)).getSuccess();

        ResourceId resourceId = ResourceId.random();
        for (int i = 0; i < nights; i++) {
            TimeSlot slot = TimeSlot.ofDay(startDate.plusDays(i));
            availabilityFixture.registerTemporalSlot(resourceId, slot);
        }

        InstanceId instanceId = InstanceId.random();
        inventoryFacade.mapInstanceToResource(entryId, instanceId, resourceId);

        return resourceId;
    }
}
