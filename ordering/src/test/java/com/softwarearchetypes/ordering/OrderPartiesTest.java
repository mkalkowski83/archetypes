package com.softwarearchetypes.ordering;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class OrderPartiesTest {

    private final PartySnapshot customer =
            PartySnapshot.of(PartyId.of("customer-1"), "Customer", "c@test.com");
    private final PartySnapshot executor =
            PartySnapshot.of(PartyId.of("executor-1"), "Executor", "e@test.com");
    private final PartySnapshot branch =
            PartySnapshot.of(PartyId.of("branch-1"), "Branch", "b@test.com");
    private final PartySnapshot courier =
            PartySnapshot.of(PartyId.of("courier-1"), "Courier", "k@test.com");

    @Test
    void singlePartyShouldAssignAllCustomerRolesAndExecutor() {
        // when
        OrderParties parties = OrderParties.singleParty(customer, executor);

        // then
        assertEquals(customer.partyId(), parties.partyWithRole(RoleInOrder.ORDERER).partyId());
        assertEquals(customer.partyId(), parties.partyWithRole(RoleInOrder.PAYER).partyId());
        assertEquals(customer.partyId(), parties.partyWithRole(RoleInOrder.RECEIVER).partyId());
        assertEquals(executor.partyId(), parties.partyWithRole(RoleInOrder.EXECUTOR).partyId());
    }

    @Test
    void corporateShouldSplitRolesAcrossParties() {
        // when
        OrderParties parties = OrderParties.corporate(customer, executor, branch);

        // then
        assertEquals(customer.partyId(), parties.partyWithRole(RoleInOrder.ORDERER).partyId());
        assertEquals(customer.partyId(), parties.partyWithRole(RoleInOrder.PAYER).partyId());
        assertEquals(executor.partyId(), parties.partyWithRole(RoleInOrder.EXECUTOR).partyId());
        assertEquals(branch.partyId(), parties.partyWithRole(RoleInOrder.RECEIVER).partyId());
    }

    @Test
    void partiesWithRoleShouldReturnAllMatchingParties() {
        // given
        OrderParties parties = OrderParties.singleParty(customer, executor);

        // when
        List<PartyInOrder> receivers = parties.partiesWithRole(RoleInOrder.RECEIVER);

        // then
        assertEquals(1, receivers.size());
        assertEquals(customer.partyId(), receivers.get(0).partyId());
    }

    @Test
    void partyWithRoleShouldThrowWhenNoneFound() {
        // given
        OrderParties parties =
                OrderParties.forOrder(
                        List.of(
                                PartyInOrder.of(customer, RoleInOrder.ORDERER, RoleInOrder.PAYER),
                                PartyInOrder.of(executor, RoleInOrder.EXECUTOR)));

        // when/then
        assertThrows(
                IllegalStateException.class, () -> parties.partyWithRole(RoleInOrder.RECEIVER));
    }

    @Test
    void mergeShouldOverrideOrderLevelRolesWithLineLevelRoles() {
        // given
        OrderParties orderLevel = OrderParties.singleParty(customer, executor);
        OrderParties lineLevel =
                OrderParties.forOrderLine(List.of(PartyInOrder.of(branch, RoleInOrder.RECEIVER)));

        // when
        OrderParties merged = OrderParties.merge(orderLevel, lineLevel);

        // then
        assertEquals(branch.partyId(), merged.partyWithRole(RoleInOrder.RECEIVER).partyId());
        assertEquals(customer.partyId(), merged.partyWithRole(RoleInOrder.ORDERER).partyId());
        assertEquals(customer.partyId(), merged.partyWithRole(RoleInOrder.PAYER).partyId());
        assertEquals(executor.partyId(), merged.partyWithRole(RoleInOrder.EXECUTOR).partyId());
    }

    @Test
    void mergeShouldPreserveOrderLevelRolesNotOverridden() {
        // given
        OrderParties orderLevel = OrderParties.singleParty(customer, executor);
        OrderParties lineLevel =
                OrderParties.forOrderLine(
                        List.of(PartyInOrder.of(courier, RoleInOrder.DELIVERY_CONTACT)));

        // when
        OrderParties merged = OrderParties.merge(orderLevel, lineLevel);

        // then
        assertEquals(customer.partyId(), merged.partyWithRole(RoleInOrder.ORDERER).partyId());
        assertEquals(customer.partyId(), merged.partyWithRole(RoleInOrder.RECEIVER).partyId());
        assertEquals(
                courier.partyId(), merged.partyWithRole(RoleInOrder.DELIVERY_CONTACT).partyId());
    }

    @Test
    void mergeWithEmptyLineLevelShouldPreserveAllOrderLevelParties() {
        // given
        OrderParties orderLevel = OrderParties.singleParty(customer, executor);
        OrderParties lineLevel = OrderParties.forOrderLine(List.of());

        // when
        OrderParties merged = OrderParties.merge(orderLevel, lineLevel);

        // then
        assertEquals(customer.partyId(), merged.partyWithRole(RoleInOrder.ORDERER).partyId());
        assertEquals(executor.partyId(), merged.partyWithRole(RoleInOrder.EXECUTOR).partyId());
    }

    @Test
    void forOrderLineWithEmptyListShouldBeEmpty() {
        // when
        OrderParties parties = OrderParties.forOrderLine(List.of());

        // then
        assertTrue(parties.isEmpty());
    }

    @Test
    void isEmptyShouldReturnFalseWhenPartiesExist() {
        // when
        OrderParties parties = OrderParties.singleParty(customer, executor);

        // then
        assertFalse(parties.isEmpty());
    }
}
