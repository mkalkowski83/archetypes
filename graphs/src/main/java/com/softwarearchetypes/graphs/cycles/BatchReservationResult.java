package com.softwarearchetypes.graphs.cycles;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

record BatchReservationResult(Status status, List<ReservationChangeRequest> executedRequests) {

    enum Status {
        SUCCESS,
        FAILURE
    }

    static BatchReservationResult success(Set<ReservationChangeRequest> executed) {
        return new BatchReservationResult(Status.SUCCESS, new ArrayList<>(executed));
    }

    static BatchReservationResult none() {
        return new BatchReservationResult(Status.FAILURE, List.of());
    }
}
