package com.softwarearchetypes.accounting;

import static com.softwarearchetypes.accounting.TransactionType.TRANSFER;
import static com.softwarearchetypes.quantity.money.Money.pln;
import static java.time.Clock.fixed;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

/** Bi-temporal - occurredAt vs appliesAt. Distinction between event time and application time. */
class BiTemporalScenarios {

    static final Instant MONDAY_10_00 =
            LocalDateTime.of(2022, 2, 1, 10, 0).atZone(ZoneId.systemDefault()).toInstant();
    static final Instant TUESDAY_10_00 =
            LocalDateTime.of(2022, 2, 2, 10, 0).atZone(ZoneId.systemDefault()).toInstant();
    static final Instant WEDNESDAY_10_00 =
            LocalDateTime.of(2022, 2, 3, 10, 0).atZone(ZoneId.systemDefault()).toInstant();
    static final Instant NOW =
            LocalDateTime.of(2022, 2, 2, 12, 50).atZone(ZoneId.systemDefault()).toInstant();

    AccountingFacade facade =
            AccountingConfiguration.inMemory(fixed(NOW, ZoneId.systemDefault())).facade();

    @Test
    void backdated_entry_should_affect_historical_balance() {
        // given
        AccountId account = createAssetAccount("Account");
        AccountId offset = createAssetAccount("Offset");

        // and - Tuesday transaction
        Transaction tuesdayTx =
                facade.transaction()
                        .occurredAt(TUESDAY_10_00)
                        .appliesAt(TUESDAY_10_00)
                        .withTypeOf(TRANSFER)
                        .executing()
                        .creditTo(account, pln(100))
                        .debitFrom(offset, pln(100))
                        .build();

        facade.execute(tuesdayTx);

        // when - transaction registered on Wednesday but applies to Monday (backdated)
        Transaction backdatedTx =
                facade.transaction()
                        .occurredAt(WEDNESDAY_10_00) // registered on Wednesday
                        .appliesAt(MONDAY_10_00) // but applies to Monday
                        .withTypeOf(TRANSFER)
                        .executing()
                        .creditTo(account, pln(50))
                        .debitFrom(offset, pln(50))
                        .build();

        facade.execute(backdatedTx);

        // then - current balance includes both transactions
        assertThat(facade.balance(account)).hasValue(pln(150));

        // and - Monday balance includes only backdated transaction
        assertThat(facade.balanceAsOf(account, MONDAY_10_00)).hasValue(pln(50));

        // and - Tuesday balance includes both
        assertThat(facade.balanceAsOf(account, TUESDAY_10_00)).hasValue(pln(150));
    }

    @Test
    void future_dated_entry_should_not_affect_current_balance_until_application_time() {
        // given
        AccountId account = createAssetAccount("Account");
        AccountId offset = createAssetAccount("Offset");

        // and - Monday transaction
        Transaction mondayTx =
                facade.transaction()
                        .occurredAt(MONDAY_10_00)
                        .appliesAt(MONDAY_10_00)
                        .withTypeOf(TRANSFER)
                        .executing()
                        .creditTo(account, pln(100))
                        .debitFrom(offset, pln(100))
                        .build();

        facade.execute(mondayTx);

        // when - transaction registered on Monday but applies to Wednesday (future-dated)
        Transaction futureDatedTx =
                facade.transaction()
                        .occurredAt(MONDAY_10_00) // registered on Monday
                        .appliesAt(WEDNESDAY_10_00) // but applies to Wednesday
                        .withTypeOf(TRANSFER)
                        .executing()
                        .creditTo(account, pln(200))
                        .debitFrom(offset, pln(200))
                        .build();

        facade.execute(futureDatedTx);

        // then - Monday balance includes only first transaction
        assertThat(facade.balanceAsOf(account, MONDAY_10_00)).hasValue(pln(100));

        // and - Tuesday balance still includes only first transaction
        assertThat(facade.balanceAsOf(account, TUESDAY_10_00)).hasValue(pln(100));

        // and - Wednesday balance includes both
        assertThat(facade.balanceAsOf(account, WEDNESDAY_10_00)).hasValue(pln(300));
    }

    private AccountId createAssetAccount(String name) {
        AccountId id = AccountId.generate();
        facade.createAccount(CreateAccount.generateAssetAccount(id, name));
        return id;
    }
}
