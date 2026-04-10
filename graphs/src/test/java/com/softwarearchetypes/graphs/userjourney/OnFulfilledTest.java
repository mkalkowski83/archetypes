package com.softwarearchetypes.graphs.userjourney;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OnFulfilledTest {

    @Test
    void shouldTransitionToNewStateWhenConditionFulfilled() {
        // given
        State newLoan = State.of(Product.newLoan());
        State afterPayment = State.of(Product.penalty());
        Condition paymentOnTime = Condition.paymentOnTime();

        UserJourney journey =
                UserJourney.builder(UserJourneyId.of("user-1"))
                        .from(newLoan)
                        .on(paymentOnTime)
                        .goto_(afterPayment)
                        .withCurrentState(newLoan)
                        .build();

        // when
        UserJourney updatedJourney = journey.onFulfilled(paymentOnTime);

        // then
        assertEquals(afterPayment, updatedJourney.currentState());
    }

    @Test
    void shouldChainMultipleTransitions() {
        // given
        State state1 = State.of(Product.newLoan());
        State state2 = State.of(Product.penalty());
        State state3 = State.of(Product.discount(10));

        Condition step1 = Condition.paymentOnTime();
        Condition step2 = Condition.promotionApproved();

        UserJourney journey =
                UserJourney.builder(UserJourneyId.of("user-2"))
                        .from(state1)
                        .on(step1)
                        .goto_(state2)
                        .from(state2)
                        .on(step2)
                        .goto_(state3)
                        .withCurrentState(state1)
                        .build();

        // when
        UserJourney afterStep1 = journey.onFulfilled(step1);
        UserJourney afterStep2 = afterStep1.onFulfilled(step2);

        // then
        assertEquals(state2, afterStep1.currentState());
        assertEquals(state3, afterStep2.currentState());
    }

    @Test
    void shouldReturnSameJourneyWhenConditionNotFound() {
        // given
        State newLoan = State.of(Product.newLoan());
        State afterPayment = State.of(Product.penalty());
        Condition paymentOnTime = Condition.paymentOnTime();
        Condition nonExistentCondition = Condition.latePayments(5);

        UserJourney journey =
                UserJourney.builder(UserJourneyId.of("user-3"))
                        .from(newLoan)
                        .on(paymentOnTime)
                        .goto_(afterPayment)
                        .withCurrentState(newLoan)
                        .build();

        // when
        UserJourney result = journey.onFulfilled(nonExistentCondition);

        // then
        assertEquals(journey.currentState(), result.currentState());
    }

    @Test
    void shouldHandleMultipleOutgoingEdges() {
        // given
        State newLoan = State.of(Product.newLoan());
        State penaltyState = State.of(Product.penalty());
        State discountState = State.of(Product.discount(5));

        Condition latePayment = Condition.latePayments(1);
        Condition onTimePayment = Condition.paymentOnTime();

        UserJourney journey =
                UserJourney.builder(UserJourneyId.of("user-5"))
                        .from(newLoan)
                        .on(latePayment)
                        .goto_(penaltyState)
                        .from(newLoan)
                        .on(onTimePayment)
                        .goto_(discountState)
                        .withCurrentState(newLoan)
                        .build();

        // when
        UserJourney afterLatePayment = journey.onFulfilled(latePayment);
        UserJourney afterOnTimePayment = journey.onFulfilled(onTimePayment);

        // then
        assertEquals(penaltyState, afterLatePayment.currentState());
        assertEquals(discountState, afterOnTimePayment.currentState());
    }

    @Test
    void shouldBuildComplexJourneyWithTransitions() {
        // given
        State newLoan = State.of(Product.newLoan());
        State afterPayment1 = State.of(Product.newLoan(), Product.penalty());
        State afterPayment2 = State.of(Product.penalty());
        State discountState = State.of(Product.discount(10));

        Condition step1 = Condition.paymentOnTime();
        Condition step2 = Condition.latePayments(1);
        Condition step3 = Condition.promotionApproved();

        UserJourney journey =
                UserJourney.builder(UserJourneyId.of("user-7"))
                        .from(newLoan)
                        .on(step1)
                        .goto_(afterPayment1)
                        .from(afterPayment1)
                        .on(step2)
                        .goto_(afterPayment2)
                        .from(afterPayment2)
                        .on(step3)
                        .goto_(discountState)
                        .withCurrentState(newLoan)
                        .build();

        // when
        UserJourney finalJourney = journey.onFulfilled(step1).onFulfilled(step2).onFulfilled(step3);

        // then
        assertEquals(discountState, finalJourney.currentState());
    }
}
