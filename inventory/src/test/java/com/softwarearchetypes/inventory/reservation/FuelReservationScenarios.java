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
import com.softwarearchetypes.quantity.Quantity;
import com.softwarearchetypes.quantity.Unit;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Reservation scenarios for pool resources. Domain: Fleet fuel reservations at a fuel station. */
@DisplayName("Fuel Reservation Scenarios (Pool)")
class FuelReservationScenarios {

    private static final Unit LITERS = Unit.liters();

    private static final OwnerId FLEET_COMPANY_A = OwnerId.random();
    private static final OwnerId FLEET_COMPANY_B = OwnerId.random();
    private static final OwnerId TAXI_CORPORATION = OwnerId.random();

    private Clock clock;
    private InventoryFacade inventoryFacade;
    private ReservationFacade reservationFacade;
    private AvailabilityFixture availabilityFixture;

    @BeforeEach
    void setup() {
        clock = Clock.fixed(Instant.parse("2024-06-01T10:00:00Z"), ZoneId.of("UTC"));
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
    @DisplayName("Basic fuel reservations")
    class BasicFuelReservations {

        @Test
        @DisplayName("Fleet can reserve fuel from available tank")
        void fleetCanReserveFuel() {
            // given - Diesel tank with 10000 liters
            ProductIdentifier diesel = ProductIdentifier.random();
            setupFuelTank(diesel, "Diesel ON", Quantity.of(10000, LITERS));

            // when - Fleet A reserves 500 liters
            Result<String, ReservationId> result =
                    reservationFacade.handle(
                            ReserveRequest.forProduct(diesel)
                                    .quantity(Quantity.of(500, LITERS))
                                    .owner(FLEET_COMPANY_A)
                                    .purpose(ReservationPurpose.BOOKING)
                                    .resourceSpecification(
                                            ResourceSpecification.QuantitySpecification.instance())
                                    .build());

            // then
            assertThat(result.success()).isTrue();
            Optional<ReservationView> view = reservationFacade.findById(result.getSuccess());
            assertThat(view).isPresent();
            assertThat(view.get().status()).isEqualTo("CONFIRMED");
        }

        @Test
        @DisplayName("Multiple fleets can reserve from same tank")
        void multipleFleetsCanReserveFromSameTank() {
            // given - Diesel tank with 10000 liters
            ProductIdentifier diesel = ProductIdentifier.random();
            setupFuelTank(diesel, "Diesel ON", Quantity.of(10000, LITERS));

            // when - Three companies reserve fuel
            Result<String, ReservationId> fleetA =
                    reservationFacade.handle(
                            ReserveRequest.forProduct(diesel)
                                    .quantity(Quantity.of(2000, LITERS))
                                    .owner(FLEET_COMPANY_A)
                                    .purpose(ReservationPurpose.BOOKING)
                                    .resourceSpecification(
                                            ResourceSpecification.QuantitySpecification.instance())
                                    .build());

            Result<String, ReservationId> fleetB =
                    reservationFacade.handle(
                            ReserveRequest.forProduct(diesel)
                                    .quantity(Quantity.of(3000, LITERS))
                                    .owner(FLEET_COMPANY_B)
                                    .purpose(ReservationPurpose.BOOKING)
                                    .resourceSpecification(
                                            ResourceSpecification.QuantitySpecification.instance())
                                    .build());

            Result<String, ReservationId> taxi =
                    reservationFacade.handle(
                            ReserveRequest.forProduct(diesel)
                                    .quantity(Quantity.of(1500, LITERS))
                                    .owner(TAXI_CORPORATION)
                                    .purpose(ReservationPurpose.BOOKING)
                                    .resourceSpecification(
                                            ResourceSpecification.QuantitySpecification.instance())
                                    .build());

            // then - all succeed (2000 + 3000 + 1500 = 6500 < 10000)
            assertThat(fleetA.success()).isTrue();
            assertThat(fleetB.success()).isTrue();
            assertThat(taxi.success()).isTrue();
        }

        @Test
        @DisplayName("Reservation fails when requesting more than available")
        void reservationFailsWhenInsufficientFuel() {
            // given - Small tank with 1000 liters
            ProductIdentifier petrol = ProductIdentifier.random();
            setupFuelTank(petrol, "Petrol 95", Quantity.of(1000, LITERS));

            // when - Fleet tries to reserve 1500 liters
            Result<String, ReservationId> result =
                    reservationFacade.handle(
                            ReserveRequest.forProduct(petrol)
                                    .quantity(Quantity.of(1500, LITERS))
                                    .owner(FLEET_COMPANY_A)
                                    .purpose(ReservationPurpose.BOOKING)
                                    .resourceSpecification(
                                            ResourceSpecification.QuantitySpecification.instance())
                                    .build());

            // then
            assertThat(result.failure()).isTrue();
        }
    }

    @Nested
    @DisplayName("Tank exhaustion scenarios")
    class TankExhaustionScenarios {

        @Test
        @DisplayName("Reservation fails when tank exhausted by previous reservations")
        void reservationFailsWhenTankExhausted() {
            // given - 5000 liters, Fleet A already reserved 4500
            ProductIdentifier diesel = ProductIdentifier.random();
            setupFuelTank(diesel, "Diesel ON", Quantity.of(5000, LITERS));

            reservationFacade.handle(
                    ReserveRequest.forProduct(diesel)
                            .quantity(Quantity.of(4500, LITERS))
                            .owner(FLEET_COMPANY_A)
                            .purpose(ReservationPurpose.BOOKING)
                            .resourceSpecification(
                                    ResourceSpecification.QuantitySpecification.instance())
                            .build());

            // when - Fleet B tries to reserve 1000 (only 500 available)
            Result<String, ReservationId> result =
                    reservationFacade.handle(
                            ReserveRequest.forProduct(diesel)
                                    .quantity(Quantity.of(1000, LITERS))
                                    .owner(FLEET_COMPANY_B)
                                    .purpose(ReservationPurpose.BOOKING)
                                    .resourceSpecification(
                                            ResourceSpecification.QuantitySpecification.instance())
                                    .build());

            // then
            assertThat(result.failure()).isTrue();
        }

        @Test
        @DisplayName("Fleet can reserve exact remaining quantity")
        void fleetCanReserveExactRemaining() {
            // given - 5000 liters, 4500 already reserved
            ProductIdentifier diesel = ProductIdentifier.random();
            setupFuelTank(diesel, "Diesel ON", Quantity.of(5000, LITERS));

            reservationFacade.handle(
                    ReserveRequest.forProduct(diesel)
                            .quantity(Quantity.of(4500, LITERS))
                            .owner(FLEET_COMPANY_A)
                            .purpose(ReservationPurpose.BOOKING)
                            .resourceSpecification(
                                    ResourceSpecification.QuantitySpecification.instance())
                            .build());

            // when - Taxi reserves exactly 500 (the remaining)
            Result<String, ReservationId> result =
                    reservationFacade.handle(
                            ReserveRequest.forProduct(diesel)
                                    .quantity(Quantity.of(500, LITERS))
                                    .owner(TAXI_CORPORATION)
                                    .purpose(ReservationPurpose.BOOKING)
                                    .resourceSpecification(
                                            ResourceSpecification.QuantitySpecification.instance())
                                    .build());

            // then
            assertThat(result.success()).isTrue();
        }
    }

    @Nested
    @DisplayName("Cancellation scenarios")
    class CancellationScenarios {

        @Test
        @DisplayName("Cancelled reservation releases fuel for others")
        void cancelledReservationReleasesFuel() {
            // given - 5000 liters, Fleet A reserved 4500
            ProductIdentifier diesel = ProductIdentifier.random();
            setupFuelTank(diesel, "Diesel ON", Quantity.of(5000, LITERS));

            ReservationId fleetAReservation =
                    reservationFacade
                            .handle(
                                    ReserveRequest.forProduct(diesel)
                                            .quantity(Quantity.of(4500, LITERS))
                                            .owner(FLEET_COMPANY_A)
                                            .purpose(ReservationPurpose.BOOKING)
                                            .resourceSpecification(
                                                    ResourceSpecification.QuantitySpecification
                                                            .instance())
                                            .build())
                            .getSuccess();

            // Fleet B can't get 1000 yet
            assertThat(
                            reservationFacade
                                    .handle(
                                            ReserveRequest.forProduct(diesel)
                                                    .quantity(Quantity.of(1000, LITERS))
                                                    .owner(FLEET_COMPANY_B)
                                                    .purpose(ReservationPurpose.BOOKING)
                                                    .resourceSpecification(
                                                            ResourceSpecification
                                                                    .QuantitySpecification
                                                                    .instance())
                                                    .build())
                                    .failure())
                    .isTrue();

            // when - Fleet A cancels
            reservationFacade.cancel(fleetAReservation, FLEET_COMPANY_A);

            // then - Fleet B can now reserve
            Result<String, ReservationId> result =
                    reservationFacade.handle(
                            ReserveRequest.forProduct(diesel)
                                    .quantity(Quantity.of(1000, LITERS))
                                    .owner(FLEET_COMPANY_B)
                                    .purpose(ReservationPurpose.BOOKING)
                                    .resourceSpecification(
                                            ResourceSpecification.QuantitySpecification.instance())
                                    .build());
            assertThat(result.success()).isTrue();
        }
    }

    @Nested
    @DisplayName("Query scenarios")
    class QueryScenarios {

        @Test
        @DisplayName("Can find all reservations by owner")
        void canFindReservationsByOwner() {
            // given - Fleet A makes multiple reservations
            ProductIdentifier diesel = ProductIdentifier.random();
            ProductIdentifier petrol = ProductIdentifier.random();
            setupFuelTank(diesel, "Diesel ON", Quantity.of(10000, LITERS));
            setupFuelTank(petrol, "Petrol 95", Quantity.of(5000, LITERS));

            reservationFacade.handle(
                    ReserveRequest.forProduct(diesel)
                            .quantity(Quantity.of(1000, LITERS))
                            .owner(FLEET_COMPANY_A)
                            .purpose(ReservationPurpose.BOOKING)
                            .resourceSpecification(
                                    ResourceSpecification.QuantitySpecification.instance())
                            .build());
            reservationFacade.handle(
                    ReserveRequest.forProduct(petrol)
                            .quantity(Quantity.of(500, LITERS))
                            .owner(FLEET_COMPANY_A)
                            .purpose(ReservationPurpose.BOOKING)
                            .resourceSpecification(
                                    ResourceSpecification.QuantitySpecification.instance())
                            .build());

            // when
            List<ReservationView> fleetAReservations =
                    reservationFacade.findByOwner(FLEET_COMPANY_A);

            // then
            assertThat(fleetAReservations).hasSize(2);
            assertThat(fleetAReservations).allMatch(r -> r.owner().equals(FLEET_COMPANY_A));
        }
    }

    private void setupFuelTank(ProductIdentifier productId, String name, Quantity capacity) {
        InventoryProduct product = InventoryProduct.identical(productId, name);
        InventoryEntryId entryId =
                inventoryFacade.handle(CreateInventoryEntry.forProduct(product)).getSuccess();

        ResourceId resourceId = ResourceId.random();
        availabilityFixture.registerPool(resourceId, capacity);

        InstanceId instanceId = InstanceId.random();
        inventoryFacade.mapInstanceToResource(entryId, instanceId, resourceId);
    }
}
