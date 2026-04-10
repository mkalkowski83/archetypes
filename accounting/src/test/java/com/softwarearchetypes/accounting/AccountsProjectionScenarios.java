package com.softwarearchetypes.accounting;

import static com.softwarearchetypes.accounting.EntryFilter.ENTRY_HAVING_METADATA;
import static com.softwarearchetypes.accounting.EntryFilter.ENTRY_OF_ACCOUNT;
import static com.softwarearchetypes.accounting.EntryFilter.HAVING_VALUES;
import static com.softwarearchetypes.accounting.RandomFixture.randomStringWithPrefixOf;
import static com.softwarearchetypes.quantity.money.Money.pln;
import static java.time.Clock.fixed;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softwarearchetypes.common.Result;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AccountsProjectionScenarios {

    static final Instant NOW =
            LocalDateTime.of(2022, 2, 2, 12, 50).atZone(ZoneId.systemDefault()).toInstant();

    AccountingFacade facade =
            AccountingConfiguration.inMemory(fixed(NOW, ZoneId.systemDefault())).facade();

    @Test
    void can_create_projecting_account() {
        // given
        AccountId cash = AccountId.generate();
        AccountId projected = AccountId.generate();
        AccountId projecting = AccountId.generate();
        // and
        assertTrue(
                facade.createAccount(
                                CreateAccount.generateAssetAccount(
                                        cash, randomStringWithPrefixOf("cash")))
                        .success());
        assertTrue(
                facade.createAccount(
                                CreateAccount.generateAssetAccount(
                                        projected, randomStringWithPrefixOf("projected")))
                        .success());
        // and
        Result projectingAccount =
                facade.createProjectingAccount(
                        projecting,
                        Filter.just(
                                ENTRY_OF_ACCOUNT(projected)
                                        .and(
                                                ENTRY_HAVING_METADATA(
                                                        HAVING_VALUES("initiator", "ewa")))),
                        "ewa-opis");
        assertTrue(projectingAccount.success());

        // when
        facade.transfer(
                projected, cash, pln(40), NOW, NOW, new MetaData(Map.of("initiator", "ewa")));
        facade.transfer(projected, cash, pln(10), NOW, NOW);
        facade.transfer(
                projected, cash, pln(30), NOW, NOW, new MetaData(Map.of("initiator", "jacek")));

        // then
        assertThat(facade.balance(projected)).hasValue(pln(-80));
        assertThat(facade.balance(projecting)).hasValue(pln(-40));

        Optional<AccountView> projectedView = facade.findAccount(projected);
        Optional<AccountView> projectingView = facade.findAccount(projecting);

        assertThat(projectedView).isPresent();
        assertThat(projectingView).isPresent();

        assertThat(projectedView.get().entries()).hasSize(3);
        assertThat(projectingView.get().entries()).hasSize(1);
        assertThat(projectingView.get().name()).isEqualTo("ewa-opis");
    }

    @Test
    void projecting_accounts_do_not_have_type() {
        // given
        AccountId projected = AccountId.generate();
        CreateAccount createAccountRequest =
                CreateAccount.generateAssetAccount(projected, randomStringWithPrefixOf("acc"));

        // and
        AccountId projecting = AccountId.generate();
        assertThat(facade.createAccount(createAccountRequest).success());

        // when
        Result projectingAccount =
                facade.createProjectingAccount(
                        projecting,
                        Filter.just(
                                ENTRY_OF_ACCOUNT(projected)
                                        .and(
                                                ENTRY_HAVING_METADATA(
                                                        HAVING_VALUES("initiator", "ewa")))),
                        "ewa-opis");
        assertThat(projectingAccount.success()).isTrue();

        // then
        Optional<AccountView> projectedView = facade.findAccount(projected);
        Optional<AccountView> projectingView = facade.findAccount(projecting);

        assertThat(projectedView).isPresent();
        assertThat(projectingView).isPresent();

        assertThat(projectedView.get().type()).isEqualTo("ASSET");
        assertThat(projectingView.get().type()).isNull();
    }
}
