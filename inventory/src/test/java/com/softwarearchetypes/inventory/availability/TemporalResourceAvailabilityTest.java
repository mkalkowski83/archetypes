package com.softwarearchetypes.inventory.availability;

import static org.assertj.core.api.Assertions.assertThat;

import com.softwarearchetypes.common.Result;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class TemporalResourceAvailabilityTest {

    private static final ResourceId ROOM_101 = ResourceId.random();
    private static final OwnerId ALICE = OwnerId.random();
    private static final OwnerId BOB = OwnerId.random();

    @Test
    void newSlotIsAvailable() {
        // given
        TimeSlot jan15 = TimeSlot.ofDay(LocalDate.of(2024, 1, 15));
        TemporalResourceAvailability room = TemporalResourceAvailability.create(ROOM_101, jan15);

        // then
        assertThat(room.isAvailable()).isTrue();
    }

    @Test
    void canLockAvailableSlot() {
        // given
        TimeSlot jan15 = TimeSlot.ofDay(LocalDate.of(2024, 1, 15));
        TemporalResourceAvailability room = TemporalResourceAvailability.create(ROOM_101, jan15);
        TemporalLockRequest request = TemporalLockRequest.indefinite(ROOM_101, jan15, ALICE);

        // when
        Result<String, BlockadeId> result = room.lock(request);

        // then
        assertThat(result.success()).isTrue();
        assertThat(room.isAvailable()).isFalse();
    }

    @Test
    void cannotLockAlreadyLockedSlot() {
        // given
        TimeSlot jan15 = TimeSlot.ofDay(LocalDate.of(2024, 1, 15));
        TemporalResourceAvailability room = TemporalResourceAvailability.create(ROOM_101, jan15);
        room.lock(TemporalLockRequest.indefinite(ROOM_101, jan15, ALICE));

        // when
        Result<String, BlockadeId> result =
                room.lock(TemporalLockRequest.indefinite(ROOM_101, jan15, BOB));

        // then
        assertThat(result.failure()).isTrue();
    }

    @Test
    void sameOwnerCanRelockSlot() {
        // given
        TimeSlot jan15 = TimeSlot.ofDay(LocalDate.of(2024, 1, 15));
        TemporalResourceAvailability room = TemporalResourceAvailability.create(ROOM_101, jan15);
        room.lock(TemporalLockRequest.indefinite(ROOM_101, jan15, ALICE));

        // when
        Result<String, BlockadeId> result =
                room.lock(TemporalLockRequest.indefinite(ROOM_101, jan15, ALICE));

        // then
        assertThat(result.success()).isTrue();
    }

    @Test
    void ownerCanUnlockSlot() {
        // given
        TimeSlot jan15 = TimeSlot.ofDay(LocalDate.of(2024, 1, 15));
        TemporalResourceAvailability room = TemporalResourceAvailability.create(ROOM_101, jan15);
        Result<String, BlockadeId> lockResult =
                room.lock(TemporalLockRequest.indefinite(ROOM_101, jan15, ALICE));
        BlockadeId blockadeId = lockResult.getSuccess();

        // when
        Result<String, BlockadeId> unlockResult = room.unlock(UnlockRequest.of(ALICE, blockadeId));

        // then
        assertThat(unlockResult.success()).isTrue();
        assertThat(room.isAvailable()).isTrue();
    }

    @Test
    void nonOwnerCannotUnlockSlot() {
        // given
        TimeSlot jan15 = TimeSlot.ofDay(LocalDate.of(2024, 1, 15));
        TemporalResourceAvailability room = TemporalResourceAvailability.create(ROOM_101, jan15);
        Result<String, BlockadeId> lockResult =
                room.lock(TemporalLockRequest.indefinite(ROOM_101, jan15, ALICE));
        BlockadeId blockadeId = lockResult.getSuccess();

        // when
        Result<String, BlockadeId> unlockResult = room.unlock(UnlockRequest.of(BOB, blockadeId));

        // then
        assertThat(unlockResult.failure()).isTrue();
        assertThat(room.isAvailable()).isFalse();
    }
}
