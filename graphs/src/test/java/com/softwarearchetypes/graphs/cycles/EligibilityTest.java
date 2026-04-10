package com.softwarearchetypes.graphs.cycles;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EligibilityTest {

    @Test
    @DisplayName("można oznaczyć transfer jako możliwy")
    void canMarkTransferAsEligible() {
        // given
        Eligibility eligibility = new Eligibility();
        OwnerId alice = OwnerId.of("Alice");
        OwnerId bob = OwnerId.of("Bob");

        // when
        eligibility.markTransferEligible(alice, bob);

        // then
        assertTrue(eligibility.isTransferEligible(alice, bob));
    }

    @Test
    @DisplayName("transfer jest niemożliwy jeśli nie został oznaczony jako możliwy")
    void transferIsIneligibleByDefault() {
        // given
        Eligibility eligibility = new Eligibility();
        OwnerId alice = OwnerId.of("Alice");
        OwnerId bob = OwnerId.of("Bob");

        // then
        assertFalse(eligibility.isTransferEligible(alice, bob));
    }

    @Test
    @DisplayName("można oznaczyć transfer jako niemożliwy")
    void canMarkTransferAsIneligible() {
        // given
        Eligibility eligibility = new Eligibility();
        OwnerId alice = OwnerId.of("Alice");
        OwnerId bob = OwnerId.of("Bob");
        eligibility.markTransferEligible(alice, bob);

        // when
        eligibility.markTransferIneligible(alice, bob);

        // then
        assertFalse(eligibility.isTransferEligible(alice, bob));
    }

    @Test
    @DisplayName("transfer jest asymetryczny - Alice->Bob nie oznacza Bob->Alice")
    void transferIsAsymmetric() {
        // given
        Eligibility eligibility = new Eligibility();
        OwnerId alice = OwnerId.of("Alice");
        OwnerId bob = OwnerId.of("Bob");

        // when
        eligibility.markTransferEligible(alice, bob);

        // then
        assertTrue(eligibility.isTransferEligible(alice, bob));
        assertFalse(eligibility.isTransferEligible(bob, alice));
    }

    @Test
    @DisplayName("wiele transferów może być oznaczonych jako możliwe")
    void multipleTransfersCanBeEligible() {
        // given
        Eligibility eligibility = new Eligibility();
        OwnerId alice = OwnerId.of("Alice");
        OwnerId bob = OwnerId.of("Bob");
        OwnerId charlie = OwnerId.of("Charlie");

        // when
        eligibility.markTransferEligible(alice, bob);
        eligibility.markTransferEligible(bob, charlie);
        eligibility.markTransferEligible(charlie, alice);

        // then
        assertTrue(eligibility.isTransferEligible(alice, bob));
        assertTrue(eligibility.isTransferEligible(bob, charlie));
        assertTrue(eligibility.isTransferEligible(charlie, alice));
    }
}
