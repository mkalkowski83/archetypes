package com.softwarearchetypes.accounting;

import static com.softwarearchetypes.accounting.RandomFixture.randomStringWithPrefixOf;
import static com.softwarearchetypes.quantity.money.Money.pln;
import static java.time.Clock.fixed;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AccountsFindScenarios {

    static final Instant NOW =
            LocalDateTime.of(2022, 2, 2, 12, 50).atZone(ZoneId.systemDefault()).toInstant();

    AccountingFacade facade =
            AccountingConfiguration.inMemory(fixed(NOW, ZoneId.systemDefault())).facade();

    // TODO: zweryfikować poprawność i kompletnosć testów

    @Test
    void should_find_existing_account() {
        // given
        AccountId accountId = AccountId.generate();

        // and
        facade.createAccount(
                CreateAccount.generateAssetAccount(accountId, randomStringWithPrefixOf("acc")));

        // when
        Optional<AccountView> result = facade.findAccount(accountId);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(accountId);
        assertThat(result.get().type()).isEqualTo(AccountType.ASSET.toString());
        assertThat(result.get().entries()).isEmpty();
    }

    @Test
    void should_return_empty_for_non_existing_account() {
        // given
        AccountId nonExistingAccountId = AccountId.generate();

        // when
        Optional<AccountView> result = facade.findAccount(nonExistingAccountId);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void should_find_account_with_transactions() {
        // given
        AccountId accountId = AccountId.generate();

        // and
        AccountId paymentAccountId = AccountId.generate();
        facade.createAccount(
                CreateAccount.generateAssetAccount(
                        paymentAccountId, randomStringWithPrefixOf("acc")));

        // and
        facade.createAccount(
                CreateAccount.generateAssetAccount(accountId, randomStringWithPrefixOf("acc")));

        // and - transfer some money from payment account
        facade.transfer(paymentAccountId, accountId, pln(100), NOW, NOW);

        // when
        Optional<AccountView> result = facade.findAccount(accountId);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(accountId);
        assertThat(result.get().type()).isEqualTo(AccountType.ASSET.toString());
        assertThat(result.get().entries()).hasSize(1);
        assertThat(result.get().entries().get(0).amount()).isEqualTo(pln(100));
    }

    @Test
    void should_find_account_with_name() {
        // given
        AccountId accountId = AccountId.generate();
        String name = randomStringWithPrefixOf("Test Account Name");

        // and
        facade.createAccount(new CreateAccount(accountId, name, "ASSET"));

        // when
        Optional<AccountView> result = facade.findAccount(accountId);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(accountId);
        assertThat(result.get().name()).isEqualTo(name);
        assertThat(result.get().type()).isEqualTo(AccountType.ASSET.toString());
    }

    @Test
    void should_find_all_accounts() {
        // given
        CreateAccount requestAcc1 =
                CreateAccount.generateAssetAccount(
                        AccountId.generate(), randomStringWithPrefixOf("acc"));
        CreateAccount requestAcc2 =
                CreateAccount.generateAssetAccount(
                        AccountId.generate(), randomStringWithPrefixOf("acc"));

        // and
        facade.createAccount(requestAcc1);
        facade.createAccount(requestAcc2);

        // when
        List<AccountView> result =
                facade.findAccounts(Set.of(requestAcc1.accountId(), requestAcc2.accountId()));

        // then
        assertThat(result).hasSize(2);
        assertThat(result.stream().map(AccountView::id).toList())
                .containsExactlyInAnyOrder(requestAcc1.accountId(), requestAcc2.accountId());
        assertThat(result.stream().map(AccountView::type).toList())
                .containsExactlyInAnyOrder(requestAcc1.type(), requestAcc2.type());
        assertThat(result.stream().map(AccountView::name).toList())
                .containsExactlyInAnyOrder(requestAcc1.name(), requestAcc2.name());
        assertThat(facade.findAccounts(Set.of(AccountId.generate(), AccountId.generate())))
                .hasSize(0);
    }

    @Test
    void should_find_some_accounts() {
        // given
        CreateAccount requestAcc1 =
                CreateAccount.generateAssetAccount(
                        AccountId.generate(), randomStringWithPrefixOf("acc"));
        CreateAccount requestAcc2 =
                CreateAccount.generateAssetAccount(
                        AccountId.generate(), randomStringWithPrefixOf("acc"));

        // and
        facade.createAccount(requestAcc1);
        facade.createAccount(requestAcc2);

        // when
        List<AccountView> result =
                facade.findAccounts(
                        Set.of(
                                requestAcc1.accountId(),
                                requestAcc2.accountId(),
                                AccountId.generate()));

        // then
        assertThat(result).hasSize(2);
        assertThat(result.stream().map(AccountView::id).toList())
                .containsExactlyInAnyOrder(requestAcc1.accountId(), requestAcc2.accountId());
        assertThat(result.stream().map(AccountView::type).toList())
                .containsExactlyInAnyOrder(requestAcc1.type(), requestAcc2.type());
        assertThat(result.stream().map(AccountView::name).toList())
                .containsExactlyInAnyOrder(requestAcc1.name(), requestAcc2.name());
        assertThat(facade.findAccounts(Set.of(AccountId.generate(), AccountId.generate())))
                .hasSize(0);
    }

    @Test
    void should_return_empty_list_when_accounts_not_present() {
        // given
        CreateAccount requestAcc1 =
                CreateAccount.generateAssetAccount(
                        AccountId.generate(), randomStringWithPrefixOf("acc"));
        CreateAccount requestAcc2 =
                CreateAccount.generateAssetAccount(
                        AccountId.generate(), randomStringWithPrefixOf("acc"));

        // and
        facade.createAccount(requestAcc1);
        facade.createAccount(requestAcc2);

        // when
        List<AccountView> result =
                facade.findAccounts(Set.of(AccountId.generate(), AccountId.generate()));

        // then
        assertThat(result).isEmpty();
    }
}
