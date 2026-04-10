package com.softwarearchetypes.accounting;

import static com.softwarearchetypes.quantity.money.Money.pln;
import static com.softwarearchetypes.quantity.money.Money.zeroPln;
import static java.time.Clock.fixed;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softwarearchetypes.common.Result;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

/** E-commerce - Customer Refund with VAT. Customer refund with proper VAT handling. */
class EcommerceScenarios {

    static final Instant MONDAY_10_00 =
            LocalDateTime.of(2022, 2, 1, 10, 0).atZone(ZoneId.systemDefault()).toInstant();
    static final Instant TUESDAY_10_00 =
            LocalDateTime.of(2022, 2, 2, 10, 0).atZone(ZoneId.systemDefault()).toInstant();
    static final Instant NOW =
            LocalDateTime.of(2022, 2, 2, 12, 50).atZone(ZoneId.systemDefault()).toInstant();

    AccountingFacade facade =
            AccountingConfiguration.inMemory(fixed(NOW, ZoneId.systemDefault())).facade();

    @Test
    void customer_refund_should_properly_handle_vat() {
        // given - e-commerce accounts
        AccountId cashAccount = createAssetAccount("Cash");
        AccountId revenueAccount = createAccount("Sales Revenue", "REVENUE");
        AccountId vatPayable = createAccount("VAT Payable", "LIABILITY");

        // and - previous sale: 123 PLN gross (100 PLN net + 23 PLN VAT)
        Transaction sale =
                facade.transaction()
                        .occurredAt(MONDAY_10_00)
                        .appliesAt(MONDAY_10_00)
                        .withTypeOf("sale")
                        .executing()
                        .creditTo(cashAccount, pln(123))
                        .debitFrom(revenueAccount, pln(100))
                        .debitFrom(vatPayable, pln(23))
                        .build();
        facade.execute(sale);

        // when - full order refund
        Transaction refund =
                facade.transaction()
                        .occurredAt(TUESDAY_10_00)
                        .appliesAt(TUESDAY_10_00)
                        .withTypeOf("refund")
                        .executing()
                        .debitFrom(cashAccount, pln(123))
                        .creditTo(revenueAccount, pln(100))
                        .creditTo(vatPayable, pln(23))
                        .build();

        Result<String, TransactionId> result = facade.execute(refund);

        // then
        assertTrue(result.success());
        assertThat(facade.balance(cashAccount)).hasValue(zeroPln());
        assertThat(facade.balance(revenueAccount)).hasValue(zeroPln());
        assertThat(facade.balance(vatPayable)).hasValue(zeroPln());
    }

    @Test
    void partial_refund_should_correctly_split_vat() {
        // given - accounts
        AccountId cashAccount = createAssetAccount("Cash");
        AccountId revenueAccount = createAccount("Sales Revenue", "REVENUE");
        AccountId vatPayable = createAccount("VAT Payable", "LIABILITY");

        // and - sale 246 PLN gross (200 PLN net + 46 PLN VAT)
        Transaction sale =
                facade.transaction()
                        .occurredAt(MONDAY_10_00)
                        .appliesAt(MONDAY_10_00)
                        .withTypeOf("sale")
                        .executing()
                        .creditTo(cashAccount, pln(246))
                        .debitFrom(revenueAccount, pln(200))
                        .debitFrom(vatPayable, pln(46))
                        .build();
        facade.execute(sale);

        // when - partial refund 123 PLN (half of order)
        Transaction partialRefund =
                facade.transaction()
                        .occurredAt(TUESDAY_10_00)
                        .appliesAt(TUESDAY_10_00)
                        .withTypeOf("partial_refund")
                        .executing()
                        .debitFrom(cashAccount, pln(123))
                        .creditTo(revenueAccount, pln(100))
                        .creditTo(vatPayable, pln(23))
                        .build();

        Result<String, TransactionId> result = facade.execute(partialRefund);

        // then
        assertTrue(result.success());
        assertThat(facade.balance(cashAccount)).hasValue(pln(123));
        assertThat(facade.balance(revenueAccount)).hasValue(pln(-100));
        assertThat(facade.balance(vatPayable)).hasValue(pln(-23));
    }

    private AccountId createAssetAccount(String name) {
        AccountId id = AccountId.generate();
        facade.createAccount(CreateAccount.generateAssetAccount(id, name));
        return id;
    }

    private AccountId createAccount(String name, String type) {
        AccountId id = AccountId.generate();
        facade.createAccount(new CreateAccount(id, name, type));
        return id;
    }
}
