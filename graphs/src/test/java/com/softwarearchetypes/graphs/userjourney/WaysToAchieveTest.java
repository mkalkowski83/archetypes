package com.softwarearchetypes.graphs.userjourney;

import static com.softwarearchetypes.graphs.userjourney.Product.ProductType.DISCOUNT;
import static com.softwarearchetypes.graphs.userjourney.Product.ProductType.PENALTY;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class WaysToAchieveTest {

    @Test
    void shouldFindSimplePath() {
        // given
        State newLoan = State.of(Product.newLoan());
        State penalty = State.of(Product.penalty());
        Condition payOnTime6Times = Condition.latePayments(6);

        UserJourney journey =
                UserJourney.builder(UserJourneyId.of("user-1"))
                        .from(newLoan)
                        .on(payOnTime6Times)
                        .goto_(penalty)
                        .withCurrentState(newLoan)
                        .build();

        // when
        Set<CustomerPath> paths = journey.waysToAchieve(PENALTY);

        // then
        assertEquals(1, paths.size());
        CustomerPath path = paths.iterator().next();
        assertEquals(1, path.length());
        assertTrue(path.conditions().contains(Condition.latePayments(6)));
    }

    @Test
    void shouldFindMultiplePathsToDiscount() {
        // given
        State newLoan = State.of(Product.newLoan());
        State afterPayments = State.of(Product.penalty());
        State discount10 = State.of(Product.discount(10));

        UserJourney journey =
                UserJourney.builder(UserJourneyId.of("user-4"))
                        .from(newLoan)
                        .on(Condition.paymentOnTime())
                        .goto_(discount10)
                        .from(newLoan)
                        .on(Condition.latePayments(3))
                        .goto_(afterPayments)
                        .from(afterPayments)
                        .on(Condition.promotionApproved())
                        .goto_(discount10)
                        .withCurrentState(newLoan)
                        .build();

        // when
        Set<CustomerPath> paths = journey.waysToAchieve(DISCOUNT);

        // then
        assertEquals(2, paths.size());
        assertContainsPath(paths, Condition.paymentOnTime());
        assertContainsPath(paths, Condition.latePayments(3), Condition.promotionApproved());
    }

    @Test
    void shouldReturnEmptyWhenRequestedStateIsNotPresent() {
        // given
        State newLoan = State.of(Product.newLoan());
        State penalty = State.of(Product.penalty());

        Condition latePayment = Condition.latePayments(1);

        UserJourney journey =
                UserJourney.builder(UserJourneyId.of("user-5"))
                        .from(newLoan)
                        .on(latePayment)
                        .goto_(penalty)
                        .withCurrentState(newLoan)
                        .build();

        // when
        Set<CustomerPath> paths = journey.waysToAchieve(DISCOUNT);

        // then
        assertTrue(paths.isEmpty());
    }

    @Test
    void shouldReturnEmptyWhenStateUnreachable() {
        // given
        State newLoan = State.of(Product.newLoan());
        State penalty = State.of(Product.penalty());
        State discount10 = State.of(Product.discount(10));

        Condition latePayment = Condition.latePayments(1);
        Condition payOnTime = Condition.paymentOnTime();

        UserJourney journey =
                UserJourney.builder(UserJourneyId.of("user-7"))
                        .from(newLoan)
                        .on(latePayment)
                        .goto_(penalty)
                        .from(newLoan)
                        .on(payOnTime)
                        .goto_(discount10)
                        .withCurrentState(penalty)
                        .build();

        // when
        Set<CustomerPath> paths = journey.waysToAchieve(DISCOUNT);

        // then
        assertTrue(paths.isEmpty());
    }

    @Test
    void shouldFindComplexPathThroughMultipleStates() {
        // given
        State newLoan = State.of(Product.newLoan());
        State afterPayment1 = State.of(Product.newLoan(), Product.penalty());
        State afterPayment2 = State.of(Product.penalty());
        State discount10 = State.of(Product.discount(10));

        Condition step1 = Condition.paymentOnTime();
        Condition step2 = Condition.latePayments(1);
        Condition step3 = Condition.promotionApproved();

        UserJourney journey =
                UserJourney.builder(UserJourneyId.of("user-9"))
                        .from(newLoan)
                        .on(step1)
                        .goto_(afterPayment1)
                        .from(afterPayment1)
                        .on(step2)
                        .goto_(afterPayment2)
                        .from(afterPayment2)
                        .on(step3)
                        .goto_(discount10)
                        .withCurrentState(newLoan)
                        .build();

        // when
        Set<CustomerPath> paths = journey.waysToAchieve(DISCOUNT);

        // then
        assertEquals(1, paths.size());
        CustomerPath path = paths.iterator().next();
        assertEquals(3, path.length());
    }

    void assertContainsPath(Set<CustomerPath> paths, Condition... expectedConditions) {
        List<Condition> expected = List.of(expectedConditions);

        boolean found = paths.stream().anyMatch(path -> pathMatches(path, expected));

        if (!found) {
            String expectedPath = formatConditions(expected);
            String actualPaths =
                    paths.stream()
                            .map(this::formatPath)
                            .collect(Collectors.joining("\n  ", "\n  ", ""));

            fail(
                    String.format(
                            "Expected to find path: %s\nBut found paths:%s",
                            expectedPath, actualPaths));
        }
    }

    private boolean pathMatches(CustomerPath path, List<Condition> expectedConditions) {
        if (path.conditions().size() != expectedConditions.size()) {
            return false;
        }

        List<Condition> actualConditions = path.conditions();
        for (int i = 0; i < expectedConditions.size(); i++) {
            if (!actualConditions.get(i).equals(expectedConditions.get(i))) {
                return false;
            }
        }
        return true;
    }

    private String formatPath(CustomerPath path) {
        return formatConditions(path.conditions());
    }

    private String formatConditions(List<Condition> conditions) {
        return conditions.stream().map(this::formatCondition).collect(Collectors.joining(" -> "));
    }

    private String formatCondition(Condition condition) {
        if (condition.attributes().isEmpty()) {
            return condition.type().toString();
        }
        return String.format("%s%s", condition.type(), condition.attributes());
    }
}
