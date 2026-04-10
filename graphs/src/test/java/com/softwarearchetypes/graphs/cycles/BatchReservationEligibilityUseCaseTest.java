package com.softwarearchetypes.graphs.cycles;

import static com.softwarearchetypes.graphs.cycles.BatchReservationResult.Status.FAILURE;
import static com.softwarearchetypes.graphs.cycles.BatchReservationResult.Status.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BatchReservationEligibilityUseCaseTest {

    SlotRepository slotRepository = new InMemorySlotRepository();
    BatchReservationUseCase batchReservationUseCase = new BatchReservationUseCase(slotRepository);

    @Test
    @DisplayName("wykonuje zamianę gdy wszyscy użytkownicy mają uprawnienia")
    void executesReservationChangesWhenAllUsersEligible() {
        // given
        SlotId slotA = SlotId.of("SlotA");
        SlotId slotB = SlotId.of("SlotB");
        OwnerId userX = OwnerId.of("UserX");
        OwnerId userY = OwnerId.of("UserY");

        thereIsSlotOwnedBy(slotA, userX);
        thereIsSlotOwnedBy(slotB, userY);

        Eligibility eligibility = new Eligibility();
        eligibility.markTransferEligible(userX, userY);
        eligibility.markTransferEligible(userY, userX);

        // when
        BatchReservationResult result =
                batchReservationUseCase.execute(
                        List.of(
                                new ReservationChangeRequest(slotA, slotB, userX),
                                new ReservationChangeRequest(slotB, slotA, userY)),
                        eligibility);

        // then
        assertEquals(SUCCESS, result.status());
        assertEquals(2, result.executedRequests().size());

        // and
        assertEquals(userY, findSlotOwner(slotA));
        assertEquals(userX, findSlotOwner(slotB));
    }

    @Test
    @DisplayName("nie wykonuje zamiany gdy jeden z użytkowników nie ma uprawnień")
    void doesNotExecuteWhenOneUserNotEligible() {
        // given
        SlotId slotA = SlotId.of("SlotA");
        SlotId slotB = SlotId.of("SlotB");
        OwnerId userX = OwnerId.of("UserX");
        OwnerId userY = OwnerId.of("UserY");

        thereIsSlotOwnedBy(slotA, userX);
        thereIsSlotOwnedBy(slotB, userY);

        Eligibility eligibility = new Eligibility();
        eligibility.markTransferEligible(userX, userY);

        // when
        BatchReservationResult result =
                batchReservationUseCase.execute(
                        List.of(
                                new ReservationChangeRequest(slotA, slotB, userX),
                                new ReservationChangeRequest(slotB, slotA, userY)),
                        eligibility);

        // then
        assertEquals(FAILURE, result.status());
        assertEquals(0, result.executedRequests().size());

        // and
        assertEquals(userX, findSlotOwner(slotA));
        assertEquals(userY, findSlotOwner(slotB));
    }

    @Test
    @DisplayName("wykonuje długi cykl zamian gdy wszyscy mają uprawnienia")
    void executesLongCycleWhenAllEligible() {
        // given
        SlotId slotA = SlotId.of("SlotA");
        SlotId slotB = SlotId.of("SlotB");
        SlotId slotC = SlotId.of("SlotC");
        SlotId slotD = SlotId.of("SlotD");
        SlotId slotE = SlotId.of("SlotE");

        OwnerId userAlice = OwnerId.of("Alice");
        OwnerId userBob = OwnerId.of("Bob");
        OwnerId userCharlie = OwnerId.of("Charlie");
        OwnerId userDiana = OwnerId.of("Diana");
        OwnerId userEve = OwnerId.of("Eve");

        thereIsSlotOwnedBy(slotA, userAlice);
        thereIsSlotOwnedBy(slotB, userBob);
        thereIsSlotOwnedBy(slotC, userCharlie);
        thereIsSlotOwnedBy(slotD, userDiana);
        thereIsSlotOwnedBy(slotE, userEve);

        Eligibility eligibility = new Eligibility();
        eligibility.markTransferEligible(userAlice, userBob);
        eligibility.markTransferEligible(userBob, userCharlie);
        eligibility.markTransferEligible(userCharlie, userDiana);
        eligibility.markTransferEligible(userDiana, userEve);
        eligibility.markTransferEligible(userEve, userAlice);

        // when
        BatchReservationResult result =
                batchReservationUseCase.execute(
                        List.of(
                                new ReservationChangeRequest(slotA, slotB, userAlice),
                                new ReservationChangeRequest(slotB, slotC, userBob),
                                new ReservationChangeRequest(slotC, slotD, userCharlie),
                                new ReservationChangeRequest(slotD, slotE, userDiana),
                                new ReservationChangeRequest(slotE, slotA, userEve)),
                        eligibility);

        // then
        assertEquals(SUCCESS, result.status());
        assertEquals(5, result.executedRequests().size());

        // and
        assertEquals(userEve, findSlotOwner(slotA));
        assertEquals(userAlice, findSlotOwner(slotB));
        assertEquals(userBob, findSlotOwner(slotC));
        assertEquals(userCharlie, findSlotOwner(slotD));
        assertEquals(userDiana, findSlotOwner(slotE));
    }

    @Test
    @DisplayName("nie wykonuje cyklu gdy jedna krawędź jest niemożliwa")
    void doesNotExecuteCycleWhenOneEdgeIneligible() {
        // given
        SlotId slotA = SlotId.of("SlotA");
        SlotId slotB = SlotId.of("SlotB");
        SlotId slotC = SlotId.of("SlotC");

        OwnerId userAlice = OwnerId.of("Alice");
        OwnerId userBob = OwnerId.of("Bob");
        OwnerId userCharlie = OwnerId.of("Charlie");

        thereIsSlotOwnedBy(slotA, userAlice);
        thereIsSlotOwnedBy(slotB, userBob);
        thereIsSlotOwnedBy(slotC, userCharlie);

        Eligibility eligibility = new Eligibility();
        eligibility.markTransferEligible(userAlice, userBob);
        eligibility.markTransferEligible(userBob, userCharlie);

        // when
        BatchReservationResult result =
                batchReservationUseCase.execute(
                        List.of(
                                new ReservationChangeRequest(slotA, slotB, userAlice),
                                new ReservationChangeRequest(slotB, slotC, userBob),
                                new ReservationChangeRequest(slotC, slotA, userCharlie)),
                        eligibility);

        // then
        assertEquals(FAILURE, result.status());
        assertEquals(0, result.executedRequests().size());

        //
        assertEquals(userAlice, findSlotOwner(slotA));
        assertEquals(userBob, findSlotOwner(slotB));
        assertEquals(userCharlie, findSlotOwner(slotC));
    }

    @Test
    @DisplayName(
            "można oznaczyć transfer jako niemożliwy po wcześniejszym zaznaczeniu jako możliwy")
    void canRevokeEligibility() {
        // given
        SlotId slotA = SlotId.of("SlotA");
        SlotId slotB = SlotId.of("SlotB");
        OwnerId userX = OwnerId.of("UserX");
        OwnerId userY = OwnerId.of("UserY");

        thereIsSlotOwnedBy(slotA, userX);
        thereIsSlotOwnedBy(slotB, userY);

        Eligibility eligibility = new Eligibility();
        eligibility.markTransferEligible(userX, userY);
        eligibility.markTransferEligible(userY, userX);

        // when
        eligibility.markTransferIneligible(userY, userX);

        BatchReservationResult result =
                batchReservationUseCase.execute(
                        List.of(
                                new ReservationChangeRequest(slotA, slotB, userX),
                                new ReservationChangeRequest(slotB, slotA, userY)),
                        eligibility);

        // then
        assertEquals(FAILURE, result.status());
        assertEquals(0, result.executedRequests().size());

        // and
        assertEquals(userX, findSlotOwner(slotA));
        assertEquals(userY, findSlotOwner(slotB));
    }

    Slot thereIsSlotOwnedBy(SlotId slotId, OwnerId owner) {
        Slot slot = Slot.create(slotId, owner);
        slotRepository.save(slot);
        return slot;
    }

    OwnerId findSlotOwner(SlotId slotId) {
        return slotRepository.findById(slotId).orElseThrow().getOwner();
    }
}
