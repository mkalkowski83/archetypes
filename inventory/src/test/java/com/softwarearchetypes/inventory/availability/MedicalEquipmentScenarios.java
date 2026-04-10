package com.softwarearchetypes.inventory.availability;

import static org.assertj.core.api.Assertions.assertThat;

import com.softwarearchetypes.common.Result;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Availability facade scenarios for individual resources. Domain: Medical equipment that can be
 * reserved by departments.
 *
 * <p>Each piece of equipment is unique and indivisible. Competition model is "winner takes all" -
 * first to lock wins.
 */
@DisplayName("Medical Equipment Scenarios (Individual Availability)")
class MedicalEquipmentScenarios {

    private static final ResourceId MRI_SCANNER = ResourceId.random();
    private static final ResourceId PORTABLE_XRAY = ResourceId.random();
    private static final ResourceId ULTRASOUND_MACHINE = ResourceId.random();

    private static final OwnerId RADIOLOGY_DEPT = OwnerId.random();
    private static final OwnerId CARDIOLOGY_DEPT = OwnerId.random();
    private static final OwnerId EMERGENCY_DEPT = OwnerId.random();

    private Clock clock;
    private AvailabilityFacade facade;

    @BeforeEach
    void setup() {
        clock = Clock.fixed(Instant.parse("2024-03-15T08:00:00Z"), ZoneId.of("UTC"));
        facade = AvailabilityConfiguration.inMemory(clock).facade();
    }

    @Nested
    @DisplayName("Reserving equipment")
    class ReservingEquipment {

        @Test
        @DisplayName("Department can reserve available MRI scanner")
        void departmentCanReserveAvailableEquipment() {
            // given - MRI scanner is available
            IndividualResourceAvailability mri =
                    IndividualResourceAvailability.create(MRI_SCANNER, clock);
            facade.register(mri);

            // when - Radiology reserves it
            IndividualLockRequest request =
                    IndividualLockRequest.indefinite(MRI_SCANNER, RADIOLOGY_DEPT);
            Result<String, BlockadeId> result = facade.lockIndividual(MRI_SCANNER, request);

            // then
            assertThat(result.success()).isTrue();
            assertThat(facade.isAvailable(mri.id())).isFalse();
        }

        @Test
        @DisplayName("Second department cannot reserve already taken equipment")
        void secondDepartmentCannotReserveTakenEquipment() {
            // given - MRI is already reserved by Radiology
            IndividualResourceAvailability mri =
                    IndividualResourceAvailability.create(MRI_SCANNER, clock);
            facade.register(mri);
            facade.lockIndividual(
                    MRI_SCANNER, IndividualLockRequest.indefinite(MRI_SCANNER, RADIOLOGY_DEPT));

            // when - Cardiology tries to reserve
            Result<String, BlockadeId> result =
                    facade.lockIndividual(
                            MRI_SCANNER,
                            IndividualLockRequest.indefinite(MRI_SCANNER, CARDIOLOGY_DEPT));

            // then
            assertThat(result.failure()).isTrue();
            assertThat(result.getFailure()).contains("not available");
        }

        @Test
        @DisplayName("Department can reserve different equipment simultaneously")
        void departmentCanReserveMultipleEquipment() {
            // given - Multiple equipment available
            IndividualResourceAvailability mri =
                    IndividualResourceAvailability.create(MRI_SCANNER, clock);
            IndividualResourceAvailability xray =
                    IndividualResourceAvailability.create(PORTABLE_XRAY, clock);
            facade.register(mri);
            facade.register(xray);

            // when - Radiology reserves both
            Result<String, BlockadeId> mriResult =
                    facade.lockIndividual(
                            MRI_SCANNER,
                            IndividualLockRequest.indefinite(MRI_SCANNER, RADIOLOGY_DEPT));
            Result<String, BlockadeId> xrayResult =
                    facade.lockIndividual(
                            PORTABLE_XRAY,
                            IndividualLockRequest.indefinite(PORTABLE_XRAY, RADIOLOGY_DEPT));

            // then - both succeed
            assertThat(mriResult.success()).isTrue();
            assertThat(xrayResult.success()).isTrue();
        }

        @Test
        @DisplayName("Different departments can reserve different equipment")
        void differentDepartmentsCanReserveDifferentEquipment() {
            // given - Three pieces of equipment
            IndividualResourceAvailability mri =
                    IndividualResourceAvailability.create(MRI_SCANNER, clock);
            IndividualResourceAvailability xray =
                    IndividualResourceAvailability.create(PORTABLE_XRAY, clock);
            IndividualResourceAvailability ultrasound =
                    IndividualResourceAvailability.create(ULTRASOUND_MACHINE, clock);
            facade.register(mri);
            facade.register(xray);
            facade.register(ultrasound);

            // when - Each department reserves one
            Result<String, BlockadeId> radiologyResult =
                    facade.lockIndividual(
                            MRI_SCANNER,
                            IndividualLockRequest.indefinite(MRI_SCANNER, RADIOLOGY_DEPT));
            Result<String, BlockadeId> cardiologyResult =
                    facade.lockIndividual(
                            ULTRASOUND_MACHINE,
                            IndividualLockRequest.indefinite(ULTRASOUND_MACHINE, CARDIOLOGY_DEPT));
            Result<String, BlockadeId> emergencyResult =
                    facade.lockIndividual(
                            PORTABLE_XRAY,
                            IndividualLockRequest.indefinite(PORTABLE_XRAY, EMERGENCY_DEPT));

            // then - all succeed
            assertThat(radiologyResult.success()).isTrue();
            assertThat(cardiologyResult.success()).isTrue();
            assertThat(emergencyResult.success()).isTrue();
        }
    }

    @Nested
    @DisplayName("Releasing equipment")
    class ReleasingEquipment {

        @Test
        @DisplayName("Department can release their reserved equipment")
        void departmentCanReleaseEquipment() {
            // given - Radiology has the MRI reserved
            IndividualResourceAvailability mri =
                    IndividualResourceAvailability.create(MRI_SCANNER, clock);
            facade.register(mri);
            BlockadeId reservation =
                    facade.lockIndividual(
                                    MRI_SCANNER,
                                    IndividualLockRequest.indefinite(MRI_SCANNER, RADIOLOGY_DEPT))
                            .getSuccess();

            // when - Radiology releases it
            Result<String, BlockadeId> result =
                    facade.unlock(mri.id(), UnlockRequest.of(RADIOLOGY_DEPT, reservation));

            // then
            assertThat(result.success()).isTrue();
            assertThat(facade.isAvailable(mri.id())).isTrue();
        }

        @Test
        @DisplayName("Department cannot release equipment reserved by another department")
        void departmentCannotReleaseOthersEquipment() {
            // given - Radiology has the MRI reserved
            IndividualResourceAvailability mri =
                    IndividualResourceAvailability.create(MRI_SCANNER, clock);
            facade.register(mri);
            BlockadeId reservation =
                    facade.lockIndividual(
                                    MRI_SCANNER,
                                    IndividualLockRequest.indefinite(MRI_SCANNER, RADIOLOGY_DEPT))
                            .getSuccess();

            // when - Cardiology tries to release it
            Result<String, BlockadeId> result =
                    facade.unlock(mri.id(), UnlockRequest.of(CARDIOLOGY_DEPT, reservation));

            // then
            assertThat(result.failure()).isTrue();
            assertThat(result.getFailure()).contains("not the owner");
            assertThat(facade.isAvailable(mri.id())).isFalse();
        }

        @Test
        @DisplayName("Released equipment can be reserved by another department")
        void releasedEquipmentCanBeReservedByOthers() {
            // given - Radiology reserves and releases MRI
            IndividualResourceAvailability mri =
                    IndividualResourceAvailability.create(MRI_SCANNER, clock);
            facade.register(mri);
            BlockadeId reservation =
                    facade.lockIndividual(
                                    MRI_SCANNER,
                                    IndividualLockRequest.indefinite(MRI_SCANNER, RADIOLOGY_DEPT))
                            .getSuccess();
            facade.unlock(mri.id(), UnlockRequest.of(RADIOLOGY_DEPT, reservation));

            // when - Cardiology reserves it
            Result<String, BlockadeId> result =
                    facade.lockIndividual(
                            MRI_SCANNER,
                            IndividualLockRequest.indefinite(MRI_SCANNER, CARDIOLOGY_DEPT));

            // then
            assertThat(result.success()).isTrue();
        }
    }

    @Nested
    @DisplayName("Same department rebooking")
    class SameDepartmentRebooking {

        @Test
        @DisplayName("Department can extend their reservation (re-lock)")
        void departmentCanExtendReservation() {
            // given - Radiology has the MRI reserved
            IndividualResourceAvailability mri =
                    IndividualResourceAvailability.create(MRI_SCANNER, clock);
            facade.register(mri);
            facade.lockIndividual(
                    MRI_SCANNER, IndividualLockRequest.indefinite(MRI_SCANNER, RADIOLOGY_DEPT));

            // when - Radiology re-locks (extends) the reservation
            Result<String, BlockadeId> result =
                    facade.lockIndividual(
                            MRI_SCANNER,
                            IndividualLockRequest.indefinite(MRI_SCANNER, RADIOLOGY_DEPT));

            // then - same owner can re-lock
            assertThat(result.success()).isTrue();
        }
    }

    @Nested
    @DisplayName("Equipment availability queries")
    class EquipmentAvailabilityQueries {

        @Test
        @DisplayName("Can check if specific equipment is available")
        void canCheckEquipmentAvailability() {
            // given - MRI reserved, X-ray free
            IndividualResourceAvailability mri =
                    IndividualResourceAvailability.create(MRI_SCANNER, clock);
            IndividualResourceAvailability xray =
                    IndividualResourceAvailability.create(PORTABLE_XRAY, clock);
            facade.register(mri);
            facade.register(xray);
            facade.lockIndividual(
                    MRI_SCANNER, IndividualLockRequest.indefinite(MRI_SCANNER, RADIOLOGY_DEPT));

            // then
            assertThat(facade.isAvailable(mri.id())).isFalse();
            assertThat(facade.isAvailable(xray.id())).isTrue();
        }

        @Test
        @DisplayName("Can find available equipment by resource ID")
        void canFindAvailableEquipmentByResourceId() {
            // given - Two MRI entries (e.g., different time slots or just multiple registrations)
            IndividualResourceAvailability mri1 =
                    IndividualResourceAvailability.create(MRI_SCANNER, clock);
            IndividualResourceAvailability mri2 =
                    IndividualResourceAvailability.create(MRI_SCANNER, clock);
            facade.register(mri1);
            facade.register(mri2);

            // Reserve one
            facade.lockIndividual(
                    MRI_SCANNER, IndividualLockRequest.indefinite(MRI_SCANNER, RADIOLOGY_DEPT));

            // then - can find by resource ID
            var allMriAvailabilities = facade.findByResourceId(MRI_SCANNER);
            assertThat(allMriAvailabilities).hasSize(2);

            var availableMri = facade.findAvailableByResourceId(MRI_SCANNER);
            assertThat(availableMri).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Concurrent access scenarios")
    class ConcurrentAccessScenarios {

        @Test
        @DisplayName("First department to lock wins in race condition")
        void firstToLockWins() {
            // given - MRI is available
            IndividualResourceAvailability mri =
                    IndividualResourceAvailability.create(MRI_SCANNER, clock);
            facade.register(mri);

            // when - Radiology locks first
            Result<String, BlockadeId> radiologyResult =
                    facade.lockIndividual(
                            MRI_SCANNER,
                            IndividualLockRequest.indefinite(MRI_SCANNER, RADIOLOGY_DEPT));

            // then - Cardiology's attempt fails
            Result<String, BlockadeId> cardiologyResult =
                    facade.lockIndividual(
                            MRI_SCANNER,
                            IndividualLockRequest.indefinite(MRI_SCANNER, CARDIOLOGY_DEPT));

            assertThat(radiologyResult.success()).isTrue();
            assertThat(cardiologyResult.failure()).isTrue();
        }
    }
}
