package com.softwarearchetypes.inventory.availability;

import static org.assertj.core.api.Assertions.assertThat;

import com.softwarearchetypes.common.Result;
import com.softwarearchetypes.quantity.Quantity;
import com.softwarearchetypes.quantity.Unit;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Availability facade scenarios for pool resources. Domain: Fuel station managing fuel tanks.
 *
 * <p>Each fuel type has a tank with capacity in liters. Multiple customers can reserve fuel
 * simultaneously. Competition model is "shared pool".
 */
@DisplayName("Fuel Station Scenarios (Pool Availability)")
class FuelStationScenarios {

    private static final Unit LITERS = Unit.liters();

    private static final ResourceId PETROL_95_TANK = ResourceId.random();
    private static final ResourceId DIESEL_TANK = ResourceId.random();

    private static final OwnerId FLEET_COMPANY_A = OwnerId.random();
    private static final OwnerId FLEET_COMPANY_B = OwnerId.random();
    private static final OwnerId TAXI_CORPORATION = OwnerId.random();

    private Clock clock;
    private AvailabilityFacade facade;

    @BeforeEach
    void setup() {
        clock = Clock.fixed(Instant.parse("2024-03-15T10:00:00Z"), ZoneId.of("UTC"));
        facade = AvailabilityConfiguration.inMemory(clock).facade();
    }

    @Nested
    @DisplayName("Reserving fuel")
    class ReservingFuel {

        @Test
        @DisplayName("Fleet company can reserve fuel from available tank")
        void fleetCanReserveAvailableFuel() {
            // given - Station has 5000 liters of Petrol 95
            PoolResourceAvailability petrolTank =
                    PoolResourceAvailability.create(
                            PETROL_95_TANK, Quantity.of(5000, LITERS), clock);
            facade.register(petrolTank);

            // when - Fleet A reserves 500 liters
            PoolLockRequest reservation =
                    PoolLockRequest.indefinite(
                            PETROL_95_TANK, Quantity.of(500, LITERS), FLEET_COMPANY_A);
            Result<String, BlockadeId> result = facade.lockPool(PETROL_95_TANK, reservation);

            // then
            assertThat(result.success()).isTrue();
            assertThat(facade.isAvailable(petrolTank.id())).isTrue(); // still has capacity
        }

        @Test
        @DisplayName("Multiple fleet companies can reserve from same tank")
        void multipleCompaniesCanReserveFromSameTank() {
            // given - 10000 liters of Diesel
            PoolResourceAvailability dieselTank =
                    PoolResourceAvailability.create(DIESEL_TANK, Quantity.of(10000, LITERS), clock);
            facade.register(dieselTank);

            // when - Three companies each reserve different amounts
            Result<String, BlockadeId> fleetAResult =
                    facade.lockPool(
                            DIESEL_TANK,
                            PoolLockRequest.indefinite(
                                    DIESEL_TANK, Quantity.of(2000, LITERS), FLEET_COMPANY_A));
            Result<String, BlockadeId> fleetBResult =
                    facade.lockPool(
                            DIESEL_TANK,
                            PoolLockRequest.indefinite(
                                    DIESEL_TANK, Quantity.of(1500, LITERS), FLEET_COMPANY_B));
            Result<String, BlockadeId> taxiResult =
                    facade.lockPool(
                            DIESEL_TANK,
                            PoolLockRequest.indefinite(
                                    DIESEL_TANK, Quantity.of(3000, LITERS), TAXI_CORPORATION));

            // then - all reservations succeed (2000 + 1500 + 3000 = 6500 < 10000)
            assertThat(fleetAResult.success()).isTrue();
            assertThat(fleetBResult.success()).isTrue();
            assertThat(taxiResult.success()).isTrue();
        }

        @Test
        @DisplayName("Reservation fails when requesting more than available")
        void reservationFailsWhenInsufficientFuel() {
            // given - Only 1000 liters available
            PoolResourceAvailability smallTank =
                    PoolResourceAvailability.create(
                            PETROL_95_TANK, Quantity.of(1000, LITERS), clock);
            facade.register(smallTank);

            // when - Company tries to reserve 1500 liters
            Result<String, BlockadeId> result =
                    facade.lockPool(
                            PETROL_95_TANK,
                            PoolLockRequest.indefinite(
                                    PETROL_95_TANK, Quantity.of(1500, LITERS), FLEET_COMPANY_A));

            // then
            assertThat(result.failure()).isTrue();
            assertThat(result.getFailure()).contains("Insufficient quantity");
        }

        @Test
        @DisplayName("Reservation fails when tank is exhausted by previous reservations")
        void reservationFailsWhenTankExhausted() {
            // given - 5000 liters, Fleet A already reserved 4000
            PoolResourceAvailability tank =
                    PoolResourceAvailability.create(DIESEL_TANK, Quantity.of(5000, LITERS), clock);
            facade.register(tank);
            facade.lockPool(
                    DIESEL_TANK,
                    PoolLockRequest.indefinite(
                            DIESEL_TANK, Quantity.of(4000, LITERS), FLEET_COMPANY_A));

            // when - Fleet B tries to reserve 1500 (only 1000 available)
            Result<String, BlockadeId> result =
                    facade.lockPool(
                            DIESEL_TANK,
                            PoolLockRequest.indefinite(
                                    DIESEL_TANK, Quantity.of(1500, LITERS), FLEET_COMPANY_B));

            // then
            assertThat(result.failure()).isTrue();
            assertThat(result.getFailure()).contains("Insufficient");
        }

        @Test
        @DisplayName("Same company can make multiple reservations")
        void sameCompanyCanMakeMultipleReservations() {
            // given - 5000 liters available
            PoolResourceAvailability tank =
                    PoolResourceAvailability.create(
                            PETROL_95_TANK, Quantity.of(5000, LITERS), clock);
            facade.register(tank);

            // when - Fleet A makes two separate reservations for different vehicles
            Result<String, BlockadeId> firstVehicle =
                    facade.lockPool(
                            PETROL_95_TANK,
                            PoolLockRequest.indefinite(
                                    PETROL_95_TANK, Quantity.of(200, LITERS), FLEET_COMPANY_A));
            Result<String, BlockadeId> secondVehicle =
                    facade.lockPool(
                            PETROL_95_TANK,
                            PoolLockRequest.indefinite(
                                    PETROL_95_TANK, Quantity.of(300, LITERS), FLEET_COMPANY_A));

            // then - both succeed with different blockade IDs
            assertThat(firstVehicle.success()).isTrue();
            assertThat(secondVehicle.success()).isTrue();
            assertThat(firstVehicle.getSuccess()).isNotEqualTo(secondVehicle.getSuccess());
        }
    }

    @Nested
    @DisplayName("Cancelling reservations")
    class CancellingReservations {

        @Test
        @DisplayName("Company can cancel their fuel reservation")
        void companyCanCancelReservation() {
            // given - Fleet A has a reservation
            PoolResourceAvailability tank =
                    PoolResourceAvailability.create(
                            PETROL_95_TANK, Quantity.of(5000, LITERS), clock);
            facade.register(tank);
            BlockadeId reservationId =
                    facade.lockPool(
                                    PETROL_95_TANK,
                                    PoolLockRequest.indefinite(
                                            PETROL_95_TANK,
                                            Quantity.of(1000, LITERS),
                                            FLEET_COMPANY_A))
                            .getSuccess();

            // when - Fleet A cancels reservation
            Result<String, BlockadeId> result =
                    facade.unlock(tank.id(), UnlockRequest.of(FLEET_COMPANY_A, reservationId));

            // then
            assertThat(result.success()).isTrue();
        }

        @Test
        @DisplayName("Company cannot cancel another company's reservation")
        void companyCannotCancelOthersReservation() {
            // given - Fleet A has a reservation
            PoolResourceAvailability tank =
                    PoolResourceAvailability.create(DIESEL_TANK, Quantity.of(5000, LITERS), clock);
            facade.register(tank);
            BlockadeId reservationId =
                    facade.lockPool(
                                    DIESEL_TANK,
                                    PoolLockRequest.indefinite(
                                            DIESEL_TANK,
                                            Quantity.of(2000, LITERS),
                                            FLEET_COMPANY_A))
                            .getSuccess();

            // when - Fleet B tries to cancel Fleet A's reservation
            Result<String, BlockadeId> result =
                    facade.unlock(tank.id(), UnlockRequest.of(FLEET_COMPANY_B, reservationId));

            // then
            assertThat(result.failure()).isTrue();
            assertThat(result.getFailure()).contains("not the owner");
        }

        @Test
        @DisplayName("Cancelled fuel becomes available for others")
        void cancelledFuelBecomesAvailable() {
            // given - Tank of 5000L, Fleet A reserved 4000L, then cancels
            PoolResourceAvailability tank =
                    PoolResourceAvailability.create(
                            PETROL_95_TANK, Quantity.of(5000, LITERS), clock);
            facade.register(tank);

            BlockadeId fleetAReservation =
                    facade.lockPool(
                                    PETROL_95_TANK,
                                    PoolLockRequest.indefinite(
                                            PETROL_95_TANK,
                                            Quantity.of(4000, LITERS),
                                            FLEET_COMPANY_A))
                            .getSuccess();

            // Fleet B can't reserve 1500L yet
            assertThat(
                            facade.lockPool(
                                            PETROL_95_TANK,
                                            PoolLockRequest.indefinite(
                                                    PETROL_95_TANK,
                                                    Quantity.of(1500, LITERS),
                                                    FLEET_COMPANY_B))
                                    .failure())
                    .isTrue();

            // Fleet A cancels
            facade.unlock(tank.id(), UnlockRequest.of(FLEET_COMPANY_A, fleetAReservation));

            // when - Fleet B tries again
            Result<String, BlockadeId> result =
                    facade.lockPool(
                            PETROL_95_TANK,
                            PoolLockRequest.indefinite(
                                    PETROL_95_TANK, Quantity.of(1500, LITERS), FLEET_COMPANY_B));

            // then - now it succeeds
            assertThat(result.success()).isTrue();
        }
    }

    @Nested
    @DisplayName("Tank capacity edge cases")
    class TankCapacityEdgeCases {

        @Test
        @DisplayName("Company can reserve exact remaining fuel")
        void canReserveExactRemaining() {
            // given - 5000L tank, 4800L already reserved
            PoolResourceAvailability tank =
                    PoolResourceAvailability.create(DIESEL_TANK, Quantity.of(5000, LITERS), clock);
            facade.register(tank);
            facade.lockPool(
                    DIESEL_TANK,
                    PoolLockRequest.indefinite(
                            DIESEL_TANK, Quantity.of(4800, LITERS), FLEET_COMPANY_A));

            // when - Taxi reserves exactly 200L (the remaining)
            Result<String, BlockadeId> result =
                    facade.lockPool(
                            DIESEL_TANK,
                            PoolLockRequest.indefinite(
                                    DIESEL_TANK, Quantity.of(200, LITERS), TAXI_CORPORATION));

            // then
            assertThat(result.success()).isTrue();
        }

        @Test
        @DisplayName("Tank shows as unavailable when fully reserved")
        void tankUnavailableWhenFullyReserved() {
            // given - 3000L tank, all reserved
            PoolResourceAvailability tank =
                    PoolResourceAvailability.create(
                            PETROL_95_TANK, Quantity.of(3000, LITERS), clock);
            facade.register(tank);
            facade.lockPool(
                    PETROL_95_TANK,
                    PoolLockRequest.indefinite(
                            PETROL_95_TANK, Quantity.of(3000, LITERS), FLEET_COMPANY_A));

            // then
            assertThat(facade.isAvailable(tank.id())).isFalse();
        }

        @Test
        @DisplayName("Multiple small reservations can fill the tank")
        void multipleSmallReservationsCanFillTank() {
            // given - 1000L tank
            PoolResourceAvailability tank =
                    PoolResourceAvailability.create(
                            PETROL_95_TANK, Quantity.of(1000, LITERS), clock);
            facade.register(tank);

            // when - Multiple small reservations totaling 1000L
            facade.lockPool(
                    PETROL_95_TANK,
                    PoolLockRequest.indefinite(
                            PETROL_95_TANK, Quantity.of(300, LITERS), FLEET_COMPANY_A));
            facade.lockPool(
                    PETROL_95_TANK,
                    PoolLockRequest.indefinite(
                            PETROL_95_TANK, Quantity.of(400, LITERS), FLEET_COMPANY_B));
            Result<String, BlockadeId> lastReservation =
                    facade.lockPool(
                            PETROL_95_TANK,
                            PoolLockRequest.indefinite(
                                    PETROL_95_TANK, Quantity.of(300, LITERS), TAXI_CORPORATION));

            // then - all succeed, tank is now full
            assertThat(lastReservation.success()).isTrue();
            assertThat(facade.isAvailable(tank.id())).isFalse();

            // and no more reservations possible
            assertThat(
                            facade.lockPool(
                                            PETROL_95_TANK,
                                            PoolLockRequest.indefinite(
                                                    PETROL_95_TANK,
                                                    Quantity.of(1, LITERS),
                                                    FLEET_COMPANY_A))
                                    .failure())
                    .isTrue();
        }
    }
}
