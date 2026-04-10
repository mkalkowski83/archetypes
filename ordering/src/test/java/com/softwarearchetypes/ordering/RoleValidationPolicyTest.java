package com.softwarearchetypes.ordering;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RoleValidationPolicyTest {

    private final PartySnapshot customer =
            PartySnapshot.of(PartyId.of("customer-1"), "Customer", "c@test.com");
    private final PartySnapshot executor =
            PartySnapshot.of(PartyId.of("executor-1"), "Executor", "e@test.com");
    private final PartySnapshot branch =
            PartySnapshot.of(PartyId.of("branch-1"), "Branch", "b@test.com");

    @Nested
    class OrderLevel {

        @Test
        void shouldAcceptValidOrderWithAllRequiredRoles() {
            // when/then
            assertDoesNotThrow(
                    () ->
                            OrderParties.forOrder(
                                    List.of(
                                            PartyInOrder.of(
                                                    customer,
                                                    RoleInOrder.ORDERER,
                                                    RoleInOrder.PAYER,
                                                    RoleInOrder.RECEIVER),
                                            PartyInOrder.of(executor, RoleInOrder.EXECUTOR))));
        }

        @Test
        void shouldAcceptOrderWithoutPayer() {
            // when/then
            assertDoesNotThrow(
                    () ->
                            OrderParties.forOrder(
                                    List.of(
                                            PartyInOrder.of(
                                                    customer,
                                                    RoleInOrder.ORDERER,
                                                    RoleInOrder.RECEIVER),
                                            PartyInOrder.of(executor, RoleInOrder.EXECUTOR))));
        }

        @Test
        void shouldAcceptOrderWithoutReceiver() {
            // when/then
            assertDoesNotThrow(
                    () ->
                            OrderParties.forOrder(
                                    List.of(
                                            PartyInOrder.of(
                                                    customer,
                                                    RoleInOrder.ORDERER,
                                                    RoleInOrder.PAYER),
                                            PartyInOrder.of(executor, RoleInOrder.EXECUTOR))));
        }

        @Test
        void shouldFailWhenOrdererMissing() {
            // when/then
            assertThrows(
                    IllegalStateException.class,
                    () ->
                            OrderParties.forOrder(
                                    List.of(
                                            PartyInOrder.of(
                                                    customer,
                                                    RoleInOrder.PAYER,
                                                    RoleInOrder.RECEIVER),
                                            PartyInOrder.of(executor, RoleInOrder.EXECUTOR))));
        }

        @Test
        void shouldFailWhenExecutorMissing() {
            // when/then
            assertThrows(
                    IllegalStateException.class,
                    () ->
                            OrderParties.forOrder(
                                    List.of(
                                            PartyInOrder.of(
                                                    customer,
                                                    RoleInOrder.ORDERER,
                                                    RoleInOrder.PAYER,
                                                    RoleInOrder.RECEIVER))));
        }

        @Test
        void shouldFailWhenMultiplePayers() {
            // given
            PartySnapshot payer2 =
                    PartySnapshot.of(PartyId.of("payer-2"), "Payer Two", "p2@test.com");

            // when/then
            assertThrows(
                    IllegalStateException.class,
                    () ->
                            OrderParties.forOrder(
                                    List.of(
                                            PartyInOrder.of(
                                                    customer,
                                                    RoleInOrder.ORDERER,
                                                    RoleInOrder.PAYER),
                                            PartyInOrder.of(executor, RoleInOrder.EXECUTOR),
                                            PartyInOrder.of(payer2, RoleInOrder.PAYER))));
        }

        @Test
        void shouldFailWhenDeliveryContactAtOrderLevel() {
            // when/then
            assertThrows(
                    IllegalStateException.class,
                    () ->
                            OrderParties.forOrder(
                                    List.of(
                                            PartyInOrder.of(
                                                    customer,
                                                    RoleInOrder.ORDERER,
                                                    RoleInOrder.PAYER),
                                            PartyInOrder.of(executor, RoleInOrder.EXECUTOR),
                                            PartyInOrder.of(
                                                    branch, RoleInOrder.DELIVERY_CONTACT))));
        }

        @Test
        void shouldFailWhenPickupAuthorizedAtOrderLevel() {
            // when/then
            assertThrows(
                    IllegalStateException.class,
                    () ->
                            OrderParties.forOrder(
                                    List.of(
                                            PartyInOrder.of(
                                                    customer,
                                                    RoleInOrder.ORDERER,
                                                    RoleInOrder.PAYER),
                                            PartyInOrder.of(executor, RoleInOrder.EXECUTOR),
                                            PartyInOrder.of(
                                                    branch, RoleInOrder.PICKUP_AUTHORIZED))));
        }
    }

    @Nested
    class LineLevel {

        @Test
        void shouldAcceptReceiverAtLineLevel() {
            // when/then
            assertDoesNotThrow(
                    () ->
                            OrderParties.forOrderLine(
                                    List.of(PartyInOrder.of(branch, RoleInOrder.RECEIVER))));
        }

        @Test
        void shouldAcceptDeliveryContactAtLineLevel() {
            // when/then
            assertDoesNotThrow(
                    () ->
                            OrderParties.forOrderLine(
                                    List.of(
                                            PartyInOrder.of(
                                                    branch, RoleInOrder.DELIVERY_CONTACT))));
        }

        @Test
        void shouldAcceptPickupAuthorizedAtLineLevel() {
            // when/then
            assertDoesNotThrow(
                    () ->
                            OrderParties.forOrderLine(
                                    List.of(
                                            PartyInOrder.of(
                                                    branch, RoleInOrder.PICKUP_AUTHORIZED))));
        }

        @Test
        void shouldAcceptEmptyLineLevel() {
            // when/then
            assertDoesNotThrow(() -> OrderParties.forOrderLine(List.of()));
        }

        @Test
        void shouldFailWhenOrdererAtLineLevel() {
            // when/then
            assertThrows(
                    IllegalStateException.class,
                    () ->
                            OrderParties.forOrderLine(
                                    List.of(PartyInOrder.of(customer, RoleInOrder.ORDERER))));
        }

        @Test
        void shouldFailWhenExecutorAtLineLevel() {
            // when/then
            assertThrows(
                    IllegalStateException.class,
                    () ->
                            OrderParties.forOrderLine(
                                    List.of(PartyInOrder.of(executor, RoleInOrder.EXECUTOR))));
        }

        @Test
        void shouldFailWhenPayerAtLineLevel() {
            // when/then
            assertThrows(
                    IllegalStateException.class,
                    () ->
                            OrderParties.forOrderLine(
                                    List.of(PartyInOrder.of(customer, RoleInOrder.PAYER))));
        }

        @Test
        void shouldAcceptMultipleRolesAtLineLevel() {
            // when/then
            assertDoesNotThrow(
                    () ->
                            OrderParties.forOrderLine(
                                    List.of(
                                            PartyInOrder.of(
                                                    branch,
                                                    RoleInOrder.RECEIVER,
                                                    RoleInOrder.DELIVERY_CONTACT,
                                                    RoleInOrder.PICKUP_AUTHORIZED))));
        }
    }
}
