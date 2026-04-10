package com.softwarearchetypes.inventory.availability;

import static org.assertj.core.api.Assertions.assertThat;

import com.softwarearchetypes.common.Result;
import com.softwarearchetypes.quantity.Quantity;
import com.softwarearchetypes.quantity.Unit;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PoolResourceAvailabilityTest {

    private static final ResourceId MILK_ID = ResourceId.random();
    private static final OwnerId ALICE = OwnerId.random();
    private static final OwnerId BOB = OwnerId.random();
    private static final Unit LITERS = Unit.liters();

    @Test
    void newPoolHasFullCapacity() {
        // given
        Quantity capacity = Quantity.of(100, LITERS);
        PoolResourceAvailability milk = PoolResourceAvailability.create(MILK_ID, capacity);

        // then
        assertThat(milk.isAvailable()).isTrue();
        assertThat(milk.availableQuantity()).isEqualTo(capacity);
    }

    @Test
    void canLockPartOfPool() {
        // given
        Quantity capacity = Quantity.of(100, LITERS);
        PoolResourceAvailability milk = PoolResourceAvailability.create(MILK_ID, capacity);
        Quantity requested = Quantity.of(30, LITERS);

        // when
        Result<String, BlockadeId> result =
                milk.lock(PoolLockRequest.of(MILK_ID, requested, ALICE, LockDuration.indefinite()));

        // then
        assertThat(result.success()).isTrue();
        assertThat(milk.availableQuantity().amount()).isEqualByComparingTo(BigDecimal.valueOf(70));
    }

    @Test
    void multipleOwnersCanLockDifferentPortions() {
        // given
        Quantity capacity = Quantity.of(100, LITERS);
        PoolResourceAvailability milk = PoolResourceAvailability.create(MILK_ID, capacity);

        // when
        milk.lock(
                PoolLockRequest.of(
                        MILK_ID, Quantity.of(30, LITERS), ALICE, LockDuration.indefinite()));
        milk.lock(
                PoolLockRequest.of(
                        MILK_ID, Quantity.of(40, LITERS), BOB, LockDuration.indefinite()));

        // then
        assertThat(milk.availableQuantity().amount()).isEqualByComparingTo(BigDecimal.valueOf(30));
        assertThat(milk.activeBlockades()).hasSize(2);
    }

    @Test
    void cannotLockMoreThanAvailable() {
        // given
        Quantity capacity = Quantity.of(100, LITERS);
        PoolResourceAvailability milk = PoolResourceAvailability.create(MILK_ID, capacity);
        milk.lock(
                PoolLockRequest.of(
                        MILK_ID, Quantity.of(80, LITERS), ALICE, LockDuration.indefinite()));

        // when
        Result<String, BlockadeId> result =
                milk.lock(
                        PoolLockRequest.of(
                                MILK_ID, Quantity.of(30, LITERS), BOB, LockDuration.indefinite()));

        // then
        assertThat(result.failure()).isTrue();
    }

    @Test
    void unlockingRestoresCapacity() {
        // given
        Quantity capacity = Quantity.of(100, LITERS);
        PoolResourceAvailability milk = PoolResourceAvailability.create(MILK_ID, capacity);
        Result<String, BlockadeId> lockResult =
                milk.lock(
                        PoolLockRequest.of(
                                MILK_ID,
                                Quantity.of(30, LITERS),
                                ALICE,
                                LockDuration.indefinite()));
        BlockadeId blockadeId = lockResult.getSuccess();

        // when
        milk.unlock(UnlockRequest.of(ALICE, blockadeId));

        // then
        assertThat(milk.availableQuantity()).isEqualTo(capacity);
    }

    @Test
    void withdrawReducesCapacityPermanently() {
        // given
        Quantity capacity = Quantity.of(100, LITERS);
        PoolResourceAvailability milk = PoolResourceAvailability.create(MILK_ID, capacity);

        // when
        milk.withdraw(Quantity.of(20, LITERS));

        // then
        assertThat(milk.availableQuantity().amount()).isEqualByComparingTo(BigDecimal.valueOf(80));
    }

    @Test
    void replenishRestoresWithdrawnCapacity() {
        // given
        Quantity capacity = Quantity.of(100, LITERS);
        PoolResourceAvailability milk = PoolResourceAvailability.create(MILK_ID, capacity);
        milk.withdraw(Quantity.of(20, LITERS));

        // when
        milk.replenish(Quantity.of(10, LITERS));

        // then
        assertThat(milk.availableQuantity().amount()).isEqualByComparingTo(BigDecimal.valueOf(90));
    }

    @Test
    void withdrawnAndBlockedBothReduceAvailability() {
        // given
        Quantity capacity = Quantity.of(100, LITERS);
        PoolResourceAvailability milk = PoolResourceAvailability.create(MILK_ID, capacity);

        // when
        milk.withdraw(Quantity.of(20, LITERS)); // permanently consumed
        milk.lock(
                PoolLockRequest.of(
                        MILK_ID,
                        Quantity.of(30, LITERS),
                        ALICE,
                        LockDuration.indefinite())); // temporarily blocked

        // then
        assertThat(milk.availableQuantity().amount()).isEqualByComparingTo(BigDecimal.valueOf(50));
    }
}
