package com.softwarearchetypes.ordering;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OrderStatusTest {

    @Test
    void onlyDraftCanAddLines() {
        // then
        assertTrue(OrderStatus.DRAFT.canAddLines());

        assertFalse(OrderStatus.CONFIRMED.canAddLines());
        assertFalse(OrderStatus.PROCESSING.canAddLines());
        assertFalse(OrderStatus.FULFILLED.canAddLines());
        assertFalse(OrderStatus.CANCELLED.canAddLines());
        assertFalse(OrderStatus.CLOSED.canAddLines());
    }

    @Test
    void draftAndConfirmedCanModifyLines() {
        // then
        assertTrue(OrderStatus.DRAFT.canModifyLines());
        assertTrue(OrderStatus.CONFIRMED.canModifyLines());

        assertFalse(OrderStatus.PROCESSING.canModifyLines());
        assertFalse(OrderStatus.FULFILLED.canModifyLines());
        assertFalse(OrderStatus.CANCELLED.canModifyLines());
        assertFalse(OrderStatus.CLOSED.canModifyLines());
    }

    @Test
    void allExceptClosedAndCancelledCanCancel() {
        // then
        assertTrue(OrderStatus.DRAFT.canCancel());
        assertTrue(OrderStatus.CONFIRMED.canCancel());
        assertTrue(OrderStatus.PENDING_ALLOCATION.canCancel());
        assertTrue(OrderStatus.PROCESSING.canCancel());
        assertTrue(OrderStatus.FULFILLED.canCancel());

        assertFalse(OrderStatus.CANCELLED.canCancel());
        assertFalse(OrderStatus.CLOSED.canCancel());
    }

    @Test
    void confirmedAndProcessingRequireApprovalToModify() {
        // then
        assertTrue(OrderStatus.CONFIRMED.requiresApprovalToModify());
        assertTrue(OrderStatus.PENDING_ALLOCATION.requiresApprovalToModify());
        assertTrue(OrderStatus.PROCESSING.requiresApprovalToModify());

        assertFalse(OrderStatus.DRAFT.requiresApprovalToModify());
        assertFalse(OrderStatus.FULFILLED.requiresApprovalToModify());
        assertFalse(OrderStatus.CANCELLED.requiresApprovalToModify());
        assertFalse(OrderStatus.CLOSED.requiresApprovalToModify());
    }
}
