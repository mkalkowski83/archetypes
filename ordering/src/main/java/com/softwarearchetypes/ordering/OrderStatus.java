package com.softwarearchetypes.ordering;

enum OrderStatus {
    DRAFT,
    CONFIRMED,
    PENDING_ALLOCATION,
    PROCESSING,
    FULFILLED,
    CLOSED,
    CANCELLED;

    public boolean canAddLines() {
        return this == DRAFT;
    }

    public boolean canModifyLines() {
        return this == DRAFT || this == CONFIRMED;
    }

    public boolean canCancel() {
        return this != CLOSED && this != CANCELLED;
    }

    public boolean requiresApprovalToModify() {
        return this == CONFIRMED || this == PENDING_ALLOCATION || this == PROCESSING;
    }
}
