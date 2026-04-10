package com.softwarearchetypes.accounting;

import static com.softwarearchetypes.quantity.money.Money.pln;
import static java.time.Clock.fixed;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softwarearchetypes.common.Result;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

/** Banking - Loan Repayment Breakdown. Loan repayment split into principal, interest and fees. */
class BankingScenarios {

    static final Instant MONDAY_10_00 =
            LocalDateTime.of(2022, 2, 1, 10, 0).atZone(ZoneId.systemDefault()).toInstant();
    static final Instant TUESDAY_10_00 =
            LocalDateTime.of(2022, 2, 2, 10, 0).atZone(ZoneId.systemDefault()).toInstant();
    static final Instant NOW =
            LocalDateTime.of(2022, 2, 2, 12, 50).atZone(ZoneId.systemDefault()).toInstant();

    AccountingFacade facade =
            AccountingConfiguration.inMemory(fixed(NOW, ZoneId.systemDefault())).facade();

    @Test
    void loan_repayment_should_be_split_into_principal_interest_and_fees() {
        // given - bank accounts
        AccountId cashAccount = createAssetAccount("Cash");
        AccountId principalReceivable = createAssetAccount("Principal Receivable");
        AccountId interestReceivable = createAssetAccount("Interest Receivable");
        AccountId feeReceivable = createAssetAccount("Fee Receivable");

        // and - initial receivable balances (customer has debt)
        facade.transfer(cashAccount, principalReceivable, pln(10000), MONDAY_10_00, MONDAY_10_00);
        facade.transfer(cashAccount, interestReceivable, pln(500), MONDAY_10_00, MONDAY_10_00);
        facade.transfer(cashAccount, feeReceivable, pln(100), MONDAY_10_00, MONDAY_10_00);

        // when - customer pays 1060 PLN (installment payment)
        // Breakdown: 1000 PLN principal, 50 PLN interest, 10 PLN fees
        Transaction repayment =
                facade.transaction()
                        .occurredAt(TUESDAY_10_00)
                        .appliesAt(TUESDAY_10_00)
                        .withTypeOf("loan_repayment")
                        .executing()
                        .creditTo(cashAccount, pln(1060))
                        .debitFrom(principalReceivable, pln(1000))
                        .debitFrom(interestReceivable, pln(50))
                        .debitFrom(feeReceivable, pln(10))
                        .build();

        Result<String, TransactionId> result = facade.execute(repayment);

        // then
        assertTrue(result.success());
        assertThat(facade.balance(cashAccount)).hasValue(pln(-9540));
        assertThat(facade.balance(principalReceivable)).hasValue(pln(9000));
        assertThat(facade.balance(interestReceivable)).hasValue(pln(450));
        assertThat(facade.balance(feeReceivable)).hasValue(pln(90));
    }

    @Test
    void loan_repayment_with_off_balance_tracking_per_customer() {
        // given - main accounts
        AccountId cashAccount = createAssetAccount("Cash");
        AccountId principalReceivable = createAssetAccount("Principal Receivable");

        // and - off-balance accounts for per-customer tracking
        AccountId mariaPrincipal = createOffBalanceAccount("Maria - Principal");
        AccountId janPrincipal = createOffBalanceAccount("Jan - Principal");

        // when - Maria repays 500 PLN principal
        Transaction mariaRepayment =
                facade.transaction()
                        .occurredAt(TUESDAY_10_00)
                        .appliesAt(TUESDAY_10_00)
                        .withTypeOf("loan_repayment")
                        .executing()
                        .creditTo(cashAccount, pln(500))
                        .debitFrom(principalReceivable, pln(500))
                        .debitFrom(mariaPrincipal, pln(500))
                        .build();

        // and - Jan repays 300 PLN principal
        Transaction janRepayment =
                facade.transaction()
                        .occurredAt(TUESDAY_10_00)
                        .appliesAt(TUESDAY_10_00)
                        .withTypeOf("loan_repayment")
                        .executing()
                        .creditTo(cashAccount, pln(300))
                        .debitFrom(principalReceivable, pln(300))
                        .debitFrom(janPrincipal, pln(300))
                        .build();

        // then
        assertTrue(facade.execute(mariaRepayment, janRepayment).success());
        assertThat(facade.balance(cashAccount)).hasValue(pln(800));
        assertThat(facade.balance(principalReceivable)).hasValue(pln(-800));
        assertThat(facade.balance(mariaPrincipal)).hasValue(pln(-500));
        assertThat(facade.balance(janPrincipal)).hasValue(pln(-300));
    }

    private AccountId createAssetAccount(String name) {
        AccountId id = AccountId.generate();
        facade.createAccount(CreateAccount.generateAssetAccount(id, name));
        return id;
    }

    private AccountId createOffBalanceAccount(String name) {
        AccountId id = AccountId.generate();
        facade.createAccount(CreateAccount.generateOffBalanceAccount(id, name));
        return id;
    }
}
