package com.softwarearchetypes.accounting;

import static com.softwarearchetypes.accounting.CreateAccount.generateAssetAccount;
import static com.softwarearchetypes.accounting.CreateAccount.generateOffBalanceAccount;
import static com.softwarearchetypes.quantity.money.Money.pln;
import static com.softwarearchetypes.quantity.money.Money.zeroPln;
import static java.time.Clock.fixed;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softwarearchetypes.common.Result;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AccountsCreatingScenarios {

    static final Instant NOW =
            LocalDateTime.of(2022, 2, 2, 12, 50).atZone(ZoneId.systemDefault()).toInstant();

    AccountingFacade facade =
            AccountingConfiguration.inMemory(fixed(NOW, ZoneId.systemDefault())).facade();

    // ========== Account Types Creation ==========

    @Test
    void can_create_asset_account() {
        // given
        AccountId accountId = AccountId.generate();
        CreateAccount request = generateAssetAccount(accountId, "Cash");

        // when
        Result<String, AccountId> result = facade.createAccount(request);

        // then
        assertTrue(result.success());
        Optional<AccountView> account = facade.findAccount(accountId);
        assertThat(account).isPresent();
        assertThat(account.get().type()).isEqualTo("ASSET");
        assertThat(account.get().name()).isEqualTo("Cash");
        assertThat(account.get().balance()).isEqualTo(zeroPln());
    }

    @Test
    void can_create_liability_account() {
        // given
        AccountId accountId = AccountId.generate();
        CreateAccount request = new CreateAccount(accountId, "Customer Deposits", "LIABILITY");

        // when
        Result<String, AccountId> result = facade.createAccount(request);

        // then
        assertTrue(result.success());
        Optional<AccountView> account = facade.findAccount(accountId);
        assertThat(account).isPresent();
        assertThat(account.get().type()).isEqualTo("LIABILITY");
        assertThat(account.get().name()).isEqualTo("Customer Deposits");
    }

    @Test
    void can_create_revenue_account() {
        // given
        AccountId accountId = AccountId.generate();
        CreateAccount request = new CreateAccount(accountId, "Sales Revenue", "REVENUE");

        // when
        Result<String, AccountId> result = facade.createAccount(request);

        // then
        assertTrue(result.success());
        Optional<AccountView> account = facade.findAccount(accountId);
        assertThat(account).isPresent();
        assertThat(account.get().type()).isEqualTo("REVENUE");
    }

    @Test
    void can_create_expense_account() {
        // given
        AccountId accountId = AccountId.generate();
        CreateAccount request = new CreateAccount(accountId, "Commission Expenses", "EXPENSE");

        // when
        Result<String, AccountId> result = facade.createAccount(request);

        // then
        assertTrue(result.success());
        Optional<AccountView> account = facade.findAccount(accountId);
        assertThat(account).isPresent();
        assertThat(account.get().type()).isEqualTo("EXPENSE");
    }

    @Test
    void can_create_off_balance_account() {
        // given
        AccountId accountId = AccountId.generate();
        CreateAccount request = generateOffBalanceAccount(accountId, "Fleet Card Limit");

        // when
        Result<String, AccountId> result = facade.createAccount(request);

        // then
        assertTrue(result.success());
        Optional<AccountView> account = facade.findAccount(accountId);
        assertThat(account).isPresent();
        assertThat(account.get().type()).isEqualTo("OFF_BALANCE");
    }

    // ========== Account Creation Errors ==========

    @Test
    void cannot_create_account_with_duplicate_id() {
        // given
        AccountId accountId = AccountId.generate();
        facade.createAccount(generateAssetAccount(accountId, "First Account"));

        // when
        Result<String, AccountId> result =
                facade.createAccount(generateAssetAccount(accountId, "Second Account"));

        // then
        assertThat(result.success()).isFalse();
        assertThat(result.getFailure()).contains("already exists");
    }

    @Test
    void can_create_multiple_accounts_at_once() {
        // given
        AccountId cash = AccountId.generate();
        AccountId receivables = AccountId.generate();
        AccountId payables = AccountId.generate();

        Set<CreateAccount> requests =
                Set.of(
                        generateAssetAccount(cash, "Cash"),
                        generateAssetAccount(receivables, "Receivables"),
                        new CreateAccount(payables, "Payables", "LIABILITY"));

        // when
        Result<String, Set<AccountId>> result = facade.createAccounts(requests);

        // then
        assertTrue(result.success());
        assertThat(facade.findAccount(cash)).isPresent();
        assertThat(facade.findAccount(receivables)).isPresent();
        assertThat(facade.findAccount(payables)).isPresent();
    }

    @Test
    void cannot_create_accounts_when_some_already_exist() {
        // given
        AccountId existing = AccountId.generate();
        facade.createAccount(generateAssetAccount(existing, "Existing"));

        // and
        AccountId newAccount = AccountId.generate();
        Set<CreateAccount> requests =
                Set.of(
                        generateAssetAccount(existing, "Existing Again"),
                        generateAssetAccount(newAccount, "New Account"));

        // when
        Result<String, Set<AccountId>> result = facade.createAccounts(requests);

        // then
        assertThat(result.success()).isFalse();
        assertThat(result.getFailure()).contains("already exists");
    }

    // ========== Account Creation with Initial Balances ==========

    @Test
    void can_create_accounts_with_initial_balances() {
        // given
        AccountId cash = AccountId.generate();
        AccountId receivables = AccountId.generate();

        Set<CreateAccount> requests =
                Set.of(
                        generateAssetAccount(cash, "Cash"),
                        generateAssetAccount(receivables, "Receivables"));

        AccountAmounts initialBalances =
                AccountAmounts.of(
                        Map.of(
                                cash, pln(1000),
                                receivables, pln(-1000)));

        // when
        Result<String, Set<AccountId>> result =
                facade.createAccountsWithInitialBalances(requests, initialBalances);

        // then
        assertTrue(result.success());
        assertThat(facade.balance(cash)).hasValue(pln(1000));
        assertThat(facade.balance(receivables)).hasValue(pln(-1000));
    }

    // ========== Finding Accounts ==========

    @Test
    void can_find_all_accounts() {
        // given
        AccountId acc1 = AccountId.generate();
        AccountId acc2 = AccountId.generate();
        facade.createAccount(generateAssetAccount(acc1, "Account 1"));
        facade.createAccount(generateAssetAccount(acc2, "Account 2"));

        // when
        List<AccountView> allAccounts = facade.findAll();

        // then
        assertThat(allAccounts).hasSizeGreaterThanOrEqualTo(2);
        assertThat(allAccounts).extracting(AccountView::id).contains(acc1, acc2);
    }

    @Test
    void can_find_multiple_accounts_by_ids() {
        // given
        AccountId acc1 = AccountId.generate();
        AccountId acc2 = AccountId.generate();
        AccountId acc3 = AccountId.generate();
        facade.createAccount(generateAssetAccount(acc1, "Account 1"));
        facade.createAccount(generateAssetAccount(acc2, "Account 2"));
        facade.createAccount(generateAssetAccount(acc3, "Account 3"));

        // when
        List<AccountView> accounts = facade.findAccounts(Set.of(acc1, acc3));

        // then
        assertThat(accounts).hasSize(2);
        assertThat(accounts).extracting(AccountView::id).containsExactlyInAnyOrder(acc1, acc3);
    }

    @Test
    void find_account_returns_empty_for_non_existing_account() {
        // given
        AccountId nonExisting = AccountId.generate();

        // when
        Optional<AccountView> account = facade.findAccount(nonExisting);

        // then
        assertThat(account).isEmpty();
    }

    // ========== Account Name Scenarios ==========

    @Test
    void account_name_is_preserved() {
        // given
        String expectedName = "My Special Account Name";
        AccountId accountId = AccountId.generate();

        // when
        facade.createAccount(generateAssetAccount(accountId, expectedName));

        // then
        Optional<AccountView> account = facade.findAccount(accountId);
        assertThat(account).isPresent();
        assertThat(account.get().name()).isEqualTo(expectedName);
    }
}
