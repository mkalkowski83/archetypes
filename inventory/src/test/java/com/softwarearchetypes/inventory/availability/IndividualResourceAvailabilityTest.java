package com.softwarearchetypes.inventory.availability;

import static org.assertj.core.api.Assertions.assertThat;

import com.softwarearchetypes.common.Result;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class IndividualResourceAvailabilityTest {

    private static final ResourceId LAPTOP_ID = ResourceId.random();
    private static final OwnerId ALICE = OwnerId.random();
    private static final OwnerId BOB = OwnerId.random();

    @Test
    void newResourceIsAvailable() {
        // given
        IndividualResourceAvailability laptop = IndividualResourceAvailability.create(LAPTOP_ID);

        // then
        assertThat(laptop.isAvailable()).isTrue();
    }

    @Test
    void canLockAvailableResource() {
        // given
        IndividualResourceAvailability laptop = IndividualResourceAvailability.create(LAPTOP_ID);
        IndividualLockRequest request =
                IndividualLockRequest.of(LAPTOP_ID, ALICE, LockDuration.indefinite());

        // when
        Result<String, BlockadeId> result = laptop.lock(request);

        // then
        assertThat(result.success()).isTrue();
        assertThat(laptop.isAvailable()).isFalse();
    }

    @Test
    void cannotLockAlreadyLockedResource() {
        // given
        IndividualResourceAvailability laptop = IndividualResourceAvailability.create(LAPTOP_ID);
        laptop.lock(IndividualLockRequest.of(LAPTOP_ID, ALICE, LockDuration.indefinite()));

        // when
        Result<String, BlockadeId> result =
                laptop.lock(IndividualLockRequest.of(LAPTOP_ID, BOB, LockDuration.indefinite()));

        // then
        assertThat(result.failure()).isTrue();
    }

    @Test
    void sameOwnerCanRelockResource() {
        // given
        IndividualResourceAvailability laptop = IndividualResourceAvailability.create(LAPTOP_ID);
        laptop.lock(IndividualLockRequest.of(LAPTOP_ID, ALICE, LockDuration.indefinite()));

        // when
        Result<String, BlockadeId> result =
                laptop.lock(IndividualLockRequest.of(LAPTOP_ID, ALICE, LockDuration.indefinite()));

        // then
        assertThat(result.success()).isTrue();
    }

    @Test
    void ownerCanUnlockResource() {
        // given
        IndividualResourceAvailability laptop = IndividualResourceAvailability.create(LAPTOP_ID);
        Result<String, BlockadeId> lockResult =
                laptop.lock(IndividualLockRequest.of(LAPTOP_ID, ALICE, LockDuration.indefinite()));
        BlockadeId blockadeId = lockResult.getSuccess();

        // when
        Result<String, BlockadeId> unlockResult =
                laptop.unlock(UnlockRequest.of(ALICE, blockadeId));

        // then
        assertThat(unlockResult.success()).isTrue();
        assertThat(laptop.isAvailable()).isTrue();
    }

    @Test
    void nonOwnerCannotUnlockResource() {
        // given
        IndividualResourceAvailability laptop = IndividualResourceAvailability.create(LAPTOP_ID);
        Result<String, BlockadeId> lockResult =
                laptop.lock(IndividualLockRequest.of(LAPTOP_ID, ALICE, LockDuration.indefinite()));
        BlockadeId blockadeId = lockResult.getSuccess();

        // when
        Result<String, BlockadeId> unlockResult = laptop.unlock(UnlockRequest.of(BOB, blockadeId));

        // then
        assertThat(unlockResult.failure()).isTrue();
        assertThat(laptop.isAvailable()).isFalse();
    }

    @Test
    void timedLockExpiresAfterDuration() {
        // given
        Instant now = Instant.parse("2024-01-15T10:00:00Z");
        Clock fixedClock = Clock.fixed(now, ZoneId.of("UTC"));
        IndividualResourceAvailability laptop =
                IndividualResourceAvailability.create(LAPTOP_ID, fixedClock);

        laptop.lock(
                IndividualLockRequest.of(LAPTOP_ID, ALICE, LockDuration.of(Duration.ofHours(1))));

        // when - time passes beyond lock duration
        Clock afterExpiry = Clock.fixed(now.plus(Duration.ofHours(2)), ZoneId.of("UTC"));
        IndividualResourceAvailability laptopAfter =
                new IndividualResourceAvailability(
                        laptop.id(),
                        LAPTOP_ID,
                        afterExpiry,
                        laptop.currentBlockade(),
                        laptop.version());

        // then
        assertThat(laptopAfter.isAvailable()).isTrue();
    }

    @Test
    void timedLockIsActiveWithinDuration() {
        // given
        Instant now = Instant.parse("2024-01-15T10:00:00Z");
        Clock fixedClock = Clock.fixed(now, ZoneId.of("UTC"));
        IndividualResourceAvailability laptop =
                IndividualResourceAvailability.create(LAPTOP_ID, fixedClock);

        laptop.lock(
                IndividualLockRequest.of(LAPTOP_ID, ALICE, LockDuration.of(Duration.ofHours(2))));

        // when - time passes but still within lock duration
        Clock withinDuration = Clock.fixed(now.plus(Duration.ofHours(1)), ZoneId.of("UTC"));
        IndividualResourceAvailability laptopDuring =
                new IndividualResourceAvailability(
                        laptop.id(),
                        LAPTOP_ID,
                        withinDuration,
                        laptop.currentBlockade(),
                        laptop.version());

        // then
        assertThat(laptopDuring.isAvailable()).isFalse();
    }
}
