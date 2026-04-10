package com.softwarearchetypes.graphs.cycles;

import static com.softwarearchetypes.graphs.cycles.BatchReservationResult.Status.FAILURE;
import static com.softwarearchetypes.graphs.cycles.BatchReservationResult.Status.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BatchReservationUseCaseTest {

    SlotRepository slotRepository = new InMemorySlotRepository();
    BatchReservationUseCase batchReservationUseCase = new BatchReservationUseCase(slotRepository);

    @Test
    @DisplayName("executes dependent reservation changes when slots remain valid")
    void executesDependentReservationChangesWhenSlotsRemainValid() {
        // given
        SlotId slotA = SlotId.of("SlotA");
        SlotId slotB = SlotId.of("SlotB");
        OwnerId userX = OwnerId.of("UserX");
        OwnerId userY = OwnerId.of("UserY");

        thereIsSlotOwnedBy(slotA, userX);
        thereIsSlotOwnedBy(slotB, userY);

        // when
        BatchReservationResult result =
                batchReservationUseCase.execute(
                        List.of(
                                new ReservationChangeRequest(
                                        slotA, slotB,
                                        userX), // UserX chce przenieść się ze SlotA do SlotB
                                new ReservationChangeRequest(
                                        slotB, slotA,
                                        userY) // UserY chce przenieść się ze SlotB do SlotA
                                ));

        // then
        assertEquals(SUCCESS, result.status());
        assertEquals(2, result.executedRequests().size());

        // and
        assertEquals(userY, findSlotOwner(slotA));
        assertEquals(userX, findSlotOwner(slotB));
    }

    @Test
    @DisplayName("executes dependent reservation changes and skips invalid ones")
    void executesDependentReservationChangesAndSkipsInvalidOnes() {
        // given
        SlotId slotA = SlotId.of("SlotA");
        SlotId slotB = SlotId.of("SlotB");
        SlotId slotD = SlotId.of("SlotD");
        OwnerId userX = OwnerId.of("UserX");
        OwnerId userY = OwnerId.of("UserY");
        OwnerId userZ = OwnerId.of("UserZ");

        thereIsSlotOwnedBy(slotA, userX);
        thereIsSlotOwnedBy(slotB, userY);
        thereIsFreeSlot(slotD);

        // when
        BatchReservationResult result =
                batchReservationUseCase.execute(
                        List.of(
                                new ReservationChangeRequest(
                                        slotB, slotA, userY), // UserY chce zająć slot UserX
                                new ReservationChangeRequest(
                                        slotA, slotB,
                                        userX), // UserX chce zająć slot UserY (kompensuje)
                                new ReservationChangeRequest(
                                        slotD, slotA,
                                        userZ) // UserZ chce zająć slot A (nie ma swojego slotu -
                                // invalid)
                                ));

        // then
        assertEquals(SUCCESS, result.status());
        assertEquals(2, result.executedRequests().size());

        assertEquals(userY, findSlotOwner(slotA));
        assertEquals(userX, findSlotOwner(slotB));
        assertEquals(OwnerId.empty(), findSlotOwner(slotD));
    }

    @Test
    @DisplayName("executes complex dependent reservation changes with multiple slots")
    void executesComplexDependentReservationChangesWithMultipleSlots() {
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

        // when
        BatchReservationResult result =
                batchReservationUseCase.execute(
                        List.of(
                                new ReservationChangeRequest(
                                        slotA, slotB, userAlice), // Alice chce slot Boba
                                new ReservationChangeRequest(
                                        slotB, slotC, userBob), // Bob chce slot Charlie
                                new ReservationChangeRequest(
                                        slotC, slotD, userCharlie), // Charlie chce slot Diany
                                new ReservationChangeRequest(
                                        slotD, slotE, userDiana), // Diana chce slot Eve
                                new ReservationChangeRequest(
                                        slotE, slotA, userEve) // Eve chce slot Alice - zamyka cykl!
                                ));

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
    void returnsFailureWhenNoCycle() {
        // given
        SlotId slotA = SlotId.of("SlotA");
        SlotId slotB = SlotId.of("SlotB");
        OwnerId userX = OwnerId.of("UserX");
        OwnerId userY = OwnerId.of("UserY");

        thereIsSlotOwnedBy(slotA, userX);

        // when
        BatchReservationResult result =
                batchReservationUseCase.execute(
                        List.of(new ReservationChangeRequest(slotA, slotB, userX)));

        // then
        assertEquals(FAILURE, result.status());
        assertEquals(0, result.executedRequests().size());

        // and
        assertEquals(userX, findSlotOwner(slotA));
    }

    Slot thereIsSlotOwnedBy(SlotId slotId, OwnerId owner) {
        Slot slot = Slot.create(slotId, owner);
        slotRepository.save(slot);
        return slot;
    }

    Slot thereIsFreeSlot(SlotId slotId) {
        Slot slot = Slot.create(slotId, OwnerId.empty());
        slotRepository.save(slot);
        return slot;
    }

    OwnerId findSlotOwner(SlotId slotId) {
        return slotRepository.findById(slotId).orElseThrow().getOwner();
    }
}
