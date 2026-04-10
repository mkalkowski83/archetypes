package com.softwarearchetypes.inventory.availability;

import static org.assertj.core.api.Assertions.assertThat;

import com.softwarearchetypes.common.Result;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Availability facade scenarios for temporal resources. Domain: Medical clinic with appointment
 * slots.
 *
 * <p>Each doctor has defined time slots (e.g., 30-minute appointments). Patients can book available
 * slots, competition model is "winner takes all".
 */
@DisplayName("Medical Appointment Scenarios (Temporal Availability)")
class MedicalAppointmentScenarios {

    private static final ZoneId ZONE = ZoneId.of("Europe/Warsaw");
    private static final LocalDate TODAY = LocalDate.of(2024, 3, 15);

    private static final ResourceId DR_KOWALSKI = ResourceId.random();
    private static final ResourceId DR_NOWAK = ResourceId.random();

    private static final OwnerId PATIENT_ANNA = OwnerId.random();
    private static final OwnerId PATIENT_TOMEK = OwnerId.random();
    private static final OwnerId PATIENT_KASIA = OwnerId.random();

    private Clock clock;
    private AvailabilityFacade facade;

    @BeforeEach
    void setup() {
        clock = Clock.fixed(Instant.parse("2024-03-15T08:00:00Z"), ZONE);
        facade = AvailabilityConfiguration.inMemory(clock).facade();
    }

    @Nested
    @DisplayName("Booking appointments")
    class BookingAppointments {

        @Test
        @DisplayName("Patient can book an available appointment slot")
        void patientCanBookAvailableSlot() {
            // given - Dr. Kowalski has a 9:00-9:30 slot available
            TimeSlot morningSlot =
                    TimeSlot.of(
                            TODAY.atTime(LocalTime.of(9, 0)).atZone(ZONE).toInstant(),
                            TODAY.atTime(LocalTime.of(9, 30)).atZone(ZONE).toInstant());
            TemporalResourceAvailability slot =
                    TemporalResourceAvailability.create(DR_KOWALSKI, morningSlot, clock);
            facade.register(slot);

            // when - Anna books the appointment
            TemporalLockRequest booking =
                    TemporalLockRequest.indefinite(DR_KOWALSKI, morningSlot, PATIENT_ANNA);
            Result<String, BlockadeId> result = facade.lockTemporal(DR_KOWALSKI, booking);

            // then
            assertThat(result.success()).isTrue();
            assertThat(facade.isAvailable(slot.id())).isFalse();
        }

        @Test
        @DisplayName("Second patient cannot book already taken slot")
        void secondPatientCannotBookTakenSlot() {
            // given - Dr. Kowalski's 10:00 slot is already booked by Anna
            TimeSlot tenOClock =
                    TimeSlot.of(
                            TODAY.atTime(LocalTime.of(10, 0)).atZone(ZONE).toInstant(),
                            TODAY.atTime(LocalTime.of(10, 30)).atZone(ZONE).toInstant());
            TemporalResourceAvailability slot =
                    TemporalResourceAvailability.create(DR_KOWALSKI, tenOClock, clock);
            facade.register(slot);
            facade.lockTemporal(
                    DR_KOWALSKI,
                    TemporalLockRequest.indefinite(DR_KOWALSKI, tenOClock, PATIENT_ANNA));

            // when - Tomek tries to book the same slot
            Result<String, BlockadeId> result =
                    facade.lockTemporal(
                            DR_KOWALSKI,
                            TemporalLockRequest.indefinite(DR_KOWALSKI, tenOClock, PATIENT_TOMEK));

            // then
            assertThat(result.failure()).isTrue();
            assertThat(result.getFailure()).contains("not available");
        }

        @Test
        @DisplayName("Patient can book different slot with same doctor")
        void patientCanBookDifferentSlot() {
            // given - Dr. Kowalski has two slots, 9:00 and 10:00
            TimeSlot nineOClock =
                    TimeSlot.of(
                            TODAY.atTime(LocalTime.of(9, 0)).atZone(ZONE).toInstant(),
                            TODAY.atTime(LocalTime.of(9, 30)).atZone(ZONE).toInstant());
            TimeSlot tenOClock =
                    TimeSlot.of(
                            TODAY.atTime(LocalTime.of(10, 0)).atZone(ZONE).toInstant(),
                            TODAY.atTime(LocalTime.of(10, 30)).atZone(ZONE).toInstant());
            TemporalResourceAvailability slot1 =
                    TemporalResourceAvailability.create(DR_KOWALSKI, nineOClock, clock);
            TemporalResourceAvailability slot2 =
                    TemporalResourceAvailability.create(DR_KOWALSKI, tenOClock, clock);
            facade.register(slot1);
            facade.register(slot2);

            // Anna books 9:00
            facade.lockTemporal(
                    DR_KOWALSKI,
                    TemporalLockRequest.indefinite(DR_KOWALSKI, nineOClock, PATIENT_ANNA));

            // when - Tomek books 10:00
            Result<String, BlockadeId> result =
                    facade.lockTemporal(
                            DR_KOWALSKI,
                            TemporalLockRequest.indefinite(DR_KOWALSKI, tenOClock, PATIENT_TOMEK));

            // then
            assertThat(result.success()).isTrue();
        }

        @Test
        @DisplayName("Different doctors can have appointments at the same time")
        void differentDoctorsCanHaveParallelAppointments() {
            // given - Both doctors have 9:00 slots
            TimeSlot nineOClock =
                    TimeSlot.of(
                            TODAY.atTime(LocalTime.of(9, 0)).atZone(ZONE).toInstant(),
                            TODAY.atTime(LocalTime.of(9, 30)).atZone(ZONE).toInstant());
            TemporalResourceAvailability kowalskiSlot =
                    TemporalResourceAvailability.create(DR_KOWALSKI, nineOClock, clock);
            TemporalResourceAvailability nowakSlot =
                    TemporalResourceAvailability.create(DR_NOWAK, nineOClock, clock);
            facade.register(kowalskiSlot);
            facade.register(nowakSlot);

            // when - Anna books Kowalski, Tomek books Nowak at same time
            Result<String, BlockadeId> annaResult =
                    facade.lockTemporal(
                            DR_KOWALSKI,
                            TemporalLockRequest.indefinite(DR_KOWALSKI, nineOClock, PATIENT_ANNA));
            Result<String, BlockadeId> tomekResult =
                    facade.lockTemporal(
                            DR_NOWAK,
                            TemporalLockRequest.indefinite(DR_NOWAK, nineOClock, PATIENT_TOMEK));

            // then
            assertThat(annaResult.success()).isTrue();
            assertThat(tomekResult.success()).isTrue();
        }
    }

    @Nested
    @DisplayName("Cancelling appointments")
    class CancellingAppointments {

        @Test
        @DisplayName("Patient can cancel their own appointment")
        void patientCanCancelOwnAppointment() {
            // given - Anna has booked a slot
            TimeSlot slot =
                    TimeSlot.of(
                            TODAY.atTime(LocalTime.of(11, 0)).atZone(ZONE).toInstant(),
                            TODAY.atTime(LocalTime.of(11, 30)).atZone(ZONE).toInstant());
            TemporalResourceAvailability availability =
                    TemporalResourceAvailability.create(DR_KOWALSKI, slot, clock);
            facade.register(availability);
            BlockadeId bookingId =
                    facade.lockTemporal(
                                    DR_KOWALSKI,
                                    TemporalLockRequest.indefinite(DR_KOWALSKI, slot, PATIENT_ANNA))
                            .getSuccess();

            // when - Anna cancels
            Result<String, BlockadeId> result =
                    facade.unlock(availability.id(), UnlockRequest.of(PATIENT_ANNA, bookingId));

            // then
            assertThat(result.success()).isTrue();
            assertThat(facade.isAvailable(availability.id())).isTrue();
        }

        @Test
        @DisplayName("Patient cannot cancel another patient's appointment")
        void patientCannotCancelOthersAppointment() {
            // given - Anna has booked a slot
            TimeSlot slot =
                    TimeSlot.of(
                            TODAY.atTime(LocalTime.of(14, 0)).atZone(ZONE).toInstant(),
                            TODAY.atTime(LocalTime.of(14, 30)).atZone(ZONE).toInstant());
            TemporalResourceAvailability availability =
                    TemporalResourceAvailability.create(DR_KOWALSKI, slot, clock);
            facade.register(availability);
            BlockadeId bookingId =
                    facade.lockTemporal(
                                    DR_KOWALSKI,
                                    TemporalLockRequest.indefinite(DR_KOWALSKI, slot, PATIENT_ANNA))
                            .getSuccess();

            // when - Tomek tries to cancel Anna's appointment
            Result<String, BlockadeId> result =
                    facade.unlock(availability.id(), UnlockRequest.of(PATIENT_TOMEK, bookingId));

            // then
            assertThat(result.failure()).isTrue();
            assertThat(result.getFailure()).contains("not the owner");
        }

        @Test
        @DisplayName("Cancelled slot becomes available for other patients")
        void cancelledSlotBecomesAvailable() {
            // given - Anna books and then cancels
            TimeSlot slot =
                    TimeSlot.of(
                            TODAY.atTime(LocalTime.of(15, 0)).atZone(ZONE).toInstant(),
                            TODAY.atTime(LocalTime.of(15, 30)).atZone(ZONE).toInstant());
            TemporalResourceAvailability availability =
                    TemporalResourceAvailability.create(DR_KOWALSKI, slot, clock);
            facade.register(availability);

            BlockadeId bookingId =
                    facade.lockTemporal(
                                    DR_KOWALSKI,
                                    TemporalLockRequest.indefinite(DR_KOWALSKI, slot, PATIENT_ANNA))
                            .getSuccess();
            facade.unlock(availability.id(), UnlockRequest.of(PATIENT_ANNA, bookingId));

            // when - Tomek books the now-free slot
            Result<String, BlockadeId> result =
                    facade.lockTemporal(
                            DR_KOWALSKI,
                            TemporalLockRequest.indefinite(DR_KOWALSKI, slot, PATIENT_TOMEK));

            // then
            assertThat(result.success()).isTrue();
        }
    }

    @Nested
    @DisplayName("Same patient rebooking")
    class SamePatientRebooking {

        @Test
        @DisplayName("Patient can extend their booking (re-lock same slot)")
        void patientCanExtendBooking() {
            // given - Anna has booked a slot
            TimeSlot slot =
                    TimeSlot.of(
                            TODAY.atTime(LocalTime.of(16, 0)).atZone(ZONE).toInstant(),
                            TODAY.atTime(LocalTime.of(16, 30)).atZone(ZONE).toInstant());
            TemporalResourceAvailability availability =
                    TemporalResourceAvailability.create(DR_KOWALSKI, slot, clock);
            facade.register(availability);
            facade.lockTemporal(
                    DR_KOWALSKI, TemporalLockRequest.indefinite(DR_KOWALSKI, slot, PATIENT_ANNA));

            // when - Anna re-locks (extends) the same slot
            Result<String, BlockadeId> result =
                    facade.lockTemporal(
                            DR_KOWALSKI,
                            TemporalLockRequest.indefinite(DR_KOWALSKI, slot, PATIENT_ANNA));

            // then - same owner can re-lock
            assertThat(result.success()).isTrue();
        }
    }
}
