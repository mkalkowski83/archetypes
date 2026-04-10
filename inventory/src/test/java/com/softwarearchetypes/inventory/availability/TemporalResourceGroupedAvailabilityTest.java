package com.softwarearchetypes.inventory.availability;

import static org.assertj.core.api.Assertions.assertThat;

import com.softwarearchetypes.common.Result;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class TemporalResourceGroupedAvailabilityTest {

    private static final ResourceId ROOM_101 = ResourceId.random();
    private static final OwnerId ALICE = OwnerId.random();
    private static final OwnerId BOB = OwnerId.random();

    @Test
    void canBlockMultipleSlotsAtOnce() {
        // given - hotel room for 3 nights
        List<TimeSlot> slots =
                List.of(
                        TimeSlot.ofDay(LocalDate.of(2024, 1, 15)),
                        TimeSlot.ofDay(LocalDate.of(2024, 1, 16)),
                        TimeSlot.ofDay(LocalDate.of(2024, 1, 17)));
        TemporalResourceGroupedAvailability stay =
                TemporalResourceGroupedAvailability.of(ROOM_101, slots);

        // when
        Result<String, List<BlockadeId>> result = stay.block(ALICE, LockDuration.indefinite());

        // then
        assertThat(result.success()).isTrue();
        assertThat(result.getSuccess()).hasSize(3);
        assertThat(stay.blockedEntirelyBy(ALICE)).isTrue();
    }

    @Test
    void failsIfAnySlotsAreUnavailable() {
        // given - room already booked for middle night
        List<TimeSlot> slots =
                List.of(
                        TimeSlot.ofDay(LocalDate.of(2024, 1, 15)),
                        TimeSlot.ofDay(LocalDate.of(2024, 1, 16)),
                        TimeSlot.ofDay(LocalDate.of(2024, 1, 17)));
        TemporalResourceGroupedAvailability stay =
                TemporalResourceGroupedAvailability.of(ROOM_101, slots);

        // someone books the middle night
        stay.availabilities()
                .get(1)
                .lock(TemporalLockRequest.indefinite(ROOM_101, slots.get(1), BOB));

        // when - Alice tries to book all 3 nights
        Result<String, List<BlockadeId>> result = stay.block(ALICE, LockDuration.indefinite());

        // then
        assertThat(result.failure()).isTrue();
        // first night should not be blocked (rollback)
        assertThat(stay.availabilities().get(0).isAvailable()).isTrue();
    }

    @Test
    void canReleaseMultipleSlots() {
        // given
        List<TimeSlot> slots =
                List.of(
                        TimeSlot.ofDay(LocalDate.of(2024, 1, 15)),
                        TimeSlot.ofDay(LocalDate.of(2024, 1, 16)));
        TemporalResourceGroupedAvailability stay =
                TemporalResourceGroupedAvailability.of(ROOM_101, slots);
        Result<String, List<BlockadeId>> blockResult = stay.block(ALICE, LockDuration.indefinite());
        List<BlockadeId> blockadeIds = blockResult.getSuccess();

        // when
        Result<String, List<BlockadeId>> releaseResult = stay.release(ALICE, blockadeIds);

        // then
        assertThat(releaseResult.success()).isTrue();
        assertThat(stay.isEntirelyAvailable()).isTrue();
    }

    @Test
    void tracksMultipleOwners() {
        // given - two separate single-night bookings
        List<TimeSlot> slots =
                List.of(
                        TimeSlot.ofDay(LocalDate.of(2024, 1, 15)),
                        TimeSlot.ofDay(LocalDate.of(2024, 1, 16)));
        TemporalResourceGroupedAvailability stay =
                TemporalResourceGroupedAvailability.of(ROOM_101, slots);

        // when
        stay.availabilities()
                .get(0)
                .lock(TemporalLockRequest.indefinite(ROOM_101, slots.get(0), ALICE));
        stay.availabilities()
                .get(1)
                .lock(TemporalLockRequest.indefinite(ROOM_101, slots.get(1), BOB));

        // then
        assertThat(stay.owners()).containsExactlyInAnyOrder(ALICE, BOB);
        assertThat(stay.isEntirelyAvailable()).isFalse();
        assertThat(stay.blockedEntirelyBy(ALICE)).isFalse();
    }
}
