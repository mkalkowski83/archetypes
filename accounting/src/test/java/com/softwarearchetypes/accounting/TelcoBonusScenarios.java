package com.softwarearchetypes.accounting;

import static com.softwarearchetypes.quantity.money.Money.pln;
import static com.softwarearchetypes.quantity.money.Money.zeroPln;
import static java.time.Clock.fixed;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Telco Bonus with Validity. Telco bonus with time-based validity (e.g., free minutes, GB). */
class TelcoBonusScenarios {

    static final Instant MONDAY_10_00 =
            LocalDateTime.of(2022, 2, 1, 10, 0).atZone(ZoneId.systemDefault()).toInstant();
    static final Instant MONDAY_11_00 =
            LocalDateTime.of(2022, 2, 1, 11, 0).atZone(ZoneId.systemDefault()).toInstant();
    static final Instant TUESDAY_10_00 =
            LocalDateTime.of(2022, 2, 2, 10, 0).atZone(ZoneId.systemDefault()).toInstant();
    static final Instant WEDNESDAY_10_00 =
            LocalDateTime.of(2022, 2, 3, 10, 0).atZone(ZoneId.systemDefault()).toInstant();
    static final Instant THURSDAY_10_00 =
            LocalDateTime.of(2022, 2, 4, 10, 0).atZone(ZoneId.systemDefault()).toInstant();
    static final Instant NEXT_MONTH =
            LocalDateTime.of(2022, 3, 2, 10, 0).atZone(ZoneId.systemDefault()).toInstant();
    static final Instant NOW =
            LocalDateTime.of(2022, 2, 4, 12, 50)
                    .atZone(ZoneId.systemDefault())
                    .toInstant(); // Thursday

    AccountingFacade facade =
            AccountingConfiguration.inMemory(fixed(NOW, ZoneId.systemDefault())).facade();

    @Test
    void telco_bonus_should_expire_and_be_compensated() {
        // given - bonus account and expired bonuses account
        AccountId bonusMinutes = createOffBalanceAccount("Bonus Minutes - Customer X");
        AccountId expiredBonuses = createOffBalanceAccount("Expired Bonuses");

        // and - grant bonus of 50 PLN worth of minutes, valid only until Wednesday
        Validity validUntilWednesday = Validity.until(WEDNESDAY_10_00);

        Transaction bonusGrant =
                facade.transaction()
                        .occurredAt(MONDAY_10_00)
                        .appliesAt(MONDAY_10_00)
                        .withTypeOf("bonus_grant")
                        .executing()
                        .creditTo(bonusMinutes, pln(50), validUntilWednesday)
                        .build();

        facade.execute(bonusGrant);

        // then - before expiration bonus is available
        assertThat(facade.balanceAsOf(bonusMinutes, TUESDAY_10_00)).hasValue(pln(50));

        // and - get bonus entry ID
        EntryId bonusEntryId =
                bonusGrant.entries().values().stream()
                        .flatMap(Collection::stream)
                        .findFirst()
                        .map(Entry::id)
                        .orElseThrow();

        // when - on Thursday compensate expired bonus
        Optional<Transaction> compensation =
                facade.transaction()
                        .occurredAt(THURSDAY_10_00)
                        .appliesAt(THURSDAY_10_00)
                        .compensatingExpired(bonusEntryId)
                        .withCompensationAccount(expiredBonuses)
                        .build();

        assertTrue(compensation.isPresent());
        assertTrue(facade.execute(compensation.get()).success());

        // then - bonus was zeroed out
        assertThat(facade.balance(bonusMinutes)).hasValue(zeroPln());

        // and - expired bonus went to compensation account
        assertThat(facade.balance(expiredBonuses)).hasValue(pln(50));
    }

    @Test
    void partially_used_bonus_should_compensate_only_remaining_amount() {
        // given - accounts
        AccountId bonusAccount = createOffBalanceAccount("Bonus Account");
        AccountId expiredBonuses = createOffBalanceAccount("Expired Bonuses");

        // and - 100 PLN bonus valid until Wednesday
        Validity validUntilWednesday = Validity.until(WEDNESDAY_10_00);

        Transaction bonusGrant =
                facade.transaction()
                        .occurredAt(MONDAY_10_00)
                        .appliesAt(MONDAY_10_00)
                        .withTypeOf("bonus_grant")
                        .executing()
                        .creditTo(bonusAccount, pln(100), validUntilWednesday)
                        .build();

        facade.execute(bonusGrant);

        // and - get bonus entry ID
        EntryId bonusEntryId =
                bonusGrant.entries().values().stream()
                        .flatMap(Collection::stream)
                        .findFirst()
                        .map(Entry::id)
                        .orElseThrow();

        // and - use 40 PLN of bonus (before expiration)
        Transaction usage =
                facade.transaction()
                        .occurredAt(TUESDAY_10_00)
                        .appliesAt(TUESDAY_10_00)
                        .withTypeOf("bonus_usage")
                        .executing()
                        .debitFrom(bonusAccount, pln(40), bonusEntryId)
                        .build();

        facade.execute(usage);
        assertThat(facade.balance(bonusAccount)).hasValue(pln(60));

        // when - compensate expired remaining part (60 PLN)
        Optional<Transaction> compensation =
                facade.transaction()
                        .occurredAt(THURSDAY_10_00)
                        .appliesAt(THURSDAY_10_00)
                        .compensatingExpired(bonusEntryId)
                        .withCompensationAccount(expiredBonuses)
                        .build();

        assertTrue(compensation.isPresent());
        assertTrue(facade.execute(compensation.get()).success());

        // then - bonus zeroed out, only used portion did not expire
        assertThat(facade.balance(bonusAccount)).hasValue(zeroPln());

        // and - unused portion expired (60 PLN)
        assertThat(facade.balance(expiredBonuses)).hasValue(pln(60));
    }

    @Test
    void multiple_bonuses_with_fifo_usage_and_expiration() {
        // given - accounts
        AccountId bonusAccount = createOffBalanceAccount("Bonus Account");
        AccountId expiredBonuses = createOffBalanceAccount("Expired Bonuses");

        // and - two bonuses with different validity periods
        Validity shortValidity = Validity.until(WEDNESDAY_10_00);
        Validity longValidity = Validity.until(NEXT_MONTH);

        Transaction bonus1 =
                facade.transaction()
                        .occurredAt(MONDAY_10_00)
                        .appliesAt(MONDAY_10_00)
                        .withTypeOf("bonus_grant")
                        .executing()
                        .creditTo(bonusAccount, pln(30), shortValidity)
                        .build();

        Transaction bonus2 =
                facade.transaction()
                        .occurredAt(MONDAY_11_00)
                        .appliesAt(MONDAY_11_00)
                        .withTypeOf("bonus_grant")
                        .executing()
                        .creditTo(bonusAccount, pln(70), longValidity)
                        .build();

        facade.execute(bonus1, bonus2);

        // and - use 20 PLN of bonus (FIFO - from shorter validity)
        Transaction usage =
                facade.transaction()
                        .occurredAt(TUESDAY_10_00)
                        .appliesAt(TUESDAY_10_00)
                        .withTypeOf("bonus_usage")
                        .executing()
                        .debitFrom(
                                bonusAccount,
                                pln(20),
                                EntryAllocationFilterBuilder.fifo(bonusAccount).build())
                        .build();

        facade.execute(usage);
        assertThat(facade.balance(bonusAccount)).hasValue(pln(80)); // 30-20 + 70

        // and - get expiring entry ID (with short validity)
        EntryId expiringEntryId =
                bonus1.entries().values().stream()
                        .flatMap(Collection::stream)
                        .findFirst()
                        .map(Entry::id)
                        .orElseThrow();

        // when - compensate expired remaining part of first bonus (10 PLN)
        Optional<Transaction> compensation =
                facade.transaction()
                        .occurredAt(THURSDAY_10_00)
                        .appliesAt(THURSDAY_10_00)
                        .compensatingExpired(expiringEntryId)
                        .withCompensationAccount(expiredBonuses)
                        .build();

        assertTrue(compensation.isPresent());
        assertTrue(facade.execute(compensation.get()).success());

        // then - only long-term bonus remains
        assertThat(facade.balance(bonusAccount)).hasValue(pln(70));
        assertThat(facade.balance(expiredBonuses)).hasValue(pln(10));
    }

    private AccountId createOffBalanceAccount(String name) {
        AccountId id = AccountId.generate();
        facade.createAccount(CreateAccount.generateOffBalanceAccount(id, name));
        return id;
    }
}
