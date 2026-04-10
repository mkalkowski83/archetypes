package com.softwarearchetypes.accounting;

import static com.softwarearchetypes.quantity.money.Money.pln;
import static java.time.Clock.fixed;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softwarearchetypes.common.Result;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Fleet Card Limit with Validity. Fleet card limit with time-based validity periods. */
class FleetCardScenarios {

    static final Instant MONDAY_10_00 =
            LocalDateTime.of(2022, 2, 1, 10, 0).atZone(ZoneId.systemDefault()).toInstant();
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
    void fleet_card_limit_should_have_validity_period() {
        // given - fleet card limit account
        AccountId fleetCardLimit = createOffBalanceAccount("Fleet Card Limit - ABC123");

        // and - grant limit of 5000 PLN valid until end of month
        Validity validUntilEndOfMonth = Validity.until(NEXT_MONTH);

        Transaction limitGrant =
                facade.transaction()
                        .occurredAt(MONDAY_10_00)
                        .appliesAt(MONDAY_10_00)
                        .withTypeOf("limit_grant")
                        .executing()
                        .creditTo(fleetCardLimit, pln(5000), validUntilEndOfMonth)
                        .build();

        facade.execute(limitGrant);

        // when - partial limit usage
        Transaction usage =
                facade.transaction()
                        .occurredAt(TUESDAY_10_00)
                        .appliesAt(TUESDAY_10_00)
                        .withTypeOf("fuel_purchase")
                        .executing()
                        .debitFrom(fleetCardLimit, pln(200))
                        .build();

        Result<String, TransactionId> result = facade.execute(usage);

        // then
        assertTrue(result.success());
        assertThat(facade.balance(fleetCardLimit)).hasValue(pln(4800));
    }

    @Test
    void expired_limit_should_be_compensated() {
        // given - limit account and compensation account
        AccountId fleetCardLimit = createOffBalanceAccount("Fleet Card Limit");
        AccountId expiredLimitsAccount = createOffBalanceAccount("Expired Limits");

        // and - limit valid only until Wednesday
        Validity validUntilWednesday = Validity.until(WEDNESDAY_10_00);

        Transaction limitGrant =
                facade.transaction()
                        .occurredAt(MONDAY_10_00)
                        .appliesAt(MONDAY_10_00)
                        .withTypeOf("limit_grant")
                        .executing()
                        .creditTo(fleetCardLimit, pln(1000), validUntilWednesday)
                        .build();

        facade.execute(limitGrant);

        // and - get limit entry ID
        EntryId limitEntryId =
                limitGrant.entries().values().stream()
                        .flatMap(Collection::stream)
                        .findFirst()
                        .map(Entry::id)
                        .orElseThrow();

        // when - on Thursday (after expiration) compensate expired limit
        Optional<Transaction> compensation =
                facade.transaction()
                        .occurredAt(THURSDAY_10_00)
                        .appliesAt(THURSDAY_10_00)
                        .compensatingExpired(limitEntryId)
                        .withCompensationAccount(expiredLimitsAccount)
                        .build();

        // then - compensation should be created
        assertTrue(compensation.isPresent());
        assertTrue(facade.execute(compensation.get()).success());

        // and - limit was zeroed out
        assertThat(facade.balance(fleetCardLimit)).hasValue(pln(0));

        // and - expired limit was transferred to compensation account
        assertThat(facade.balance(expiredLimitsAccount)).hasValue(pln(1000));
    }

    @Test
    void multiple_limits_with_different_validity_and_expiration_compensation() {
        // given - accounts
        AccountId fleetCardLimit = createOffBalanceAccount("Fleet Card Limit");
        AccountId expiredLimitsAccount = createOffBalanceAccount("Expired Limits");

        // and - two limits with different validity periods
        Validity validUntilWednesday = Validity.until(WEDNESDAY_10_00);
        Validity validUntilNextMonth = Validity.until(NEXT_MONTH);

        Transaction limit1 =
                facade.transaction()
                        .occurredAt(MONDAY_10_00)
                        .appliesAt(MONDAY_10_00)
                        .withTypeOf("limit_grant")
                        .executing()
                        .creditTo(fleetCardLimit, pln(1000), validUntilWednesday)
                        .build();

        Transaction limit2 =
                facade.transaction()
                        .occurredAt(MONDAY_10_00)
                        .appliesAt(MONDAY_10_00)
                        .withTypeOf("limit_grant")
                        .executing()
                        .creditTo(fleetCardLimit, pln(2000), validUntilNextMonth)
                        .build();

        facade.execute(limit1, limit2);

        // then - total limit is sum of both
        assertThat(facade.balance(fleetCardLimit)).hasValue(pln(3000));

        // and - get expiring entry ID
        EntryId expiringEntryId =
                limit1.entries().values().stream()
                        .flatMap(Collection::stream)
                        .findFirst()
                        .map(Entry::id)
                        .orElseThrow();

        // when - compensate expired limit
        Optional<Transaction> compensation =
                facade.transaction()
                        .occurredAt(THURSDAY_10_00)
                        .appliesAt(THURSDAY_10_00)
                        .compensatingExpired(expiringEntryId)
                        .withCompensationAccount(expiredLimitsAccount)
                        .build();

        assertTrue(compensation.isPresent());
        assertTrue(facade.execute(compensation.get()).success());

        // then - only long-term limit remains
        assertThat(facade.balance(fleetCardLimit)).hasValue(pln(2000));
        assertThat(facade.balance(expiredLimitsAccount)).hasValue(pln(1000));
    }

    private AccountId createOffBalanceAccount(String name) {
        AccountId id = AccountId.generate();
        facade.createAccount(CreateAccount.generateOffBalanceAccount(id, name));
        return id;
    }
}
