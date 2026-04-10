package com.softwarearchetypes.graphs.userjourney;

import static com.softwarearchetypes.graphs.userjourney.Condition.ConditionType.*;
import static com.softwarearchetypes.graphs.userjourney.Product.ProductType.DISCOUNT;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class WeightedPathsTest {

    @Test
    void shouldFindCheapestPathByMinimizingCost() {
        // given
        State newLoan = State.of(Product.newLoan());
        State intermediate = State.of(Product.penalty());
        State discount = State.of(Product.discount(10));
        Condition directExpensive = Condition.withCost(PAYMENT_ON_TIME, 100.0);
        Condition cheapStep1 = Condition.withCost(LATE_PAYMENT, 30.0);
        Condition cheapStep2 = Condition.withCost(RESTRUCTURING, 20.0);

        UserJourney journey =
                UserJourney.builder(UserJourneyId.of("user-1"))
                        .from(newLoan)
                        .on(directExpensive)
                        .goto_(discount)
                        .from(newLoan)
                        .on(cheapStep1)
                        .goto_(intermediate)
                        .from(intermediate)
                        .on(cheapStep2)
                        .goto_(discount)
                        .withCurrentState(newLoan)
                        .build();

        // when
        Optional<CustomerPath> cheapestPath =
                journey.optimizedWayToAchieve(DISCOUNT, Condition::getCost);

        // then
        assertTrue(cheapestPath.isPresent());
        CustomerPath path = cheapestPath.get();
        assertEquals(2, path.length(), "Najtańsza ścieżka powinna mieć 2 kroki");
        assertTrue(path.conditions().contains(cheapStep1));
        assertTrue(path.conditions().contains(cheapStep2));
    }

    @Test
    void shouldFindFastestPathByMinimizingTime() {
        // given
        State newLoan = State.of(Product.newLoan());
        State intermediate1 = State.of(Product.penalty());
        State intermediate2 = State.of(Product.newLoan(), Product.penalty());
        State discount = State.of(Product.discount(10));

        Condition fastPath = Condition.withTime(PAYMENT_ON_TIME, 5);

        Condition slowStep1 = Condition.withTime(LATE_PAYMENT, 15);
        Condition slowStep2 = Condition.withTime(RESTRUCTURING, 10);

        Condition mediumStep1 = Condition.withTime(PROMOTION_APPROVED, 7);
        Condition mediumStep2 = Condition.withTime(PAYMENT_ON_TIME, 4);

        UserJourney journey =
                UserJourney.builder(UserJourneyId.of("user-2"))
                        .from(newLoan)
                        .on(fastPath)
                        .goto_(discount)
                        .from(newLoan)
                        .on(slowStep1)
                        .goto_(intermediate1)
                        .from(intermediate1)
                        .on(slowStep2)
                        .goto_(discount)
                        .from(newLoan)
                        .on(mediumStep1)
                        .goto_(intermediate2)
                        .from(intermediate2)
                        .on(mediumStep2)
                        .goto_(discount)
                        .withCurrentState(newLoan)
                        .build();

        // when
        Optional<CustomerPath> fastestPath =
                journey.optimizedWayToAchieve(DISCOUNT, c -> (double) c.getTime());

        // then
        assertTrue(fastestPath.isPresent());
        CustomerPath path = fastestPath.get();
        assertEquals(1, path.length(), "Najszybsza ścieżka to bezpośrednia (5 dni)");
        assertTrue(path.conditions().contains(fastPath));
    }

    @Test
    void shouldDemonstrateTradeoffBetweenCostAndTime() {
        // given
        State newLoan = State.of(Product.newLoan());
        State expressRoute = State.of(Product.penalty());
        State economyRoute = State.of(Product.discount(5));
        State discount10 = State.of(Product.discount(10));

        Condition expressStep1 = Condition.withAttributes(PAYMENT_ON_TIME, 150.0, 3, 0.0);

        Condition economyStep1 = Condition.withAttributes(LATE_PAYMENT, 20.0, 30, 0.0);

        Condition toDiscount1 = Condition.withAttributes(PROMOTION_APPROVED, 10.0, 1, 0.0);
        Condition toDiscount2 = Condition.withAttributes(RESTRUCTURING, 10.0, 1, 0.0);

        UserJourney journey =
                UserJourney.builder(UserJourneyId.of("user-4"))
                        .from(newLoan)
                        .on(expressStep1)
                        .goto_(expressRoute)
                        .from(expressRoute)
                        .on(toDiscount1)
                        .goto_(discount10)
                        .from(newLoan)
                        .on(economyStep1)
                        .goto_(economyRoute)
                        .from(economyRoute)
                        .on(toDiscount2)
                        .goto_(discount10)
                        .withCurrentState(newLoan)
                        .build();

        // when
        Optional<CustomerPath> cheapest =
                journey.optimizedWayToAchieve(DISCOUNT, Condition::getCost);
        Optional<CustomerPath> fastest =
                journey.optimizedWayToAchieve(DISCOUNT, c -> (double) c.getTime());

        // then
        assertTrue(cheapest.isPresent());
        assertTrue(fastest.isPresent());

        assertTrue(
                cheapest.get().conditions().contains(economyStep1),
                "Najtańsza ścieżka powinna używać economy route");
        assertTrue(
                fastest.get().conditions().contains(expressStep1),
                "Najszybsza ścieżka powinna używać express route");
    }
}
