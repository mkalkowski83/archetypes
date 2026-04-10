package com.softwarearchetypes.pricing;

import static com.softwarearchetypes.pricing.ClockFixture.someFixedClock;
import static java.time.LocalDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;

import com.softwarearchetypes.quantity.money.Money;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class CompositeComponentVersioningTest {

    Clock clock = someFixedClock();

    @Test
    void shouldCreateCompositeWithInitialVersion() {
        // given: two child components
        Component basePrice =
                SimpleComponent.withInitialVersion(
                        "Base Price",
                        new SimpleFixedCalculator("base", Money.pln(100)),
                        Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0)),
                        clock);
        Component tax =
                SimpleComponent.withInitialVersion(
                        "Tax",
                        new SimpleFixedCalculator("tax", Money.pln(23)),
                        Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0)),
                        clock);

        // when: create composite
        Component total =
                CompositeComponent.withInitialVersion(
                        "Total Price",
                        List.of(basePrice, tax),
                        Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0)),
                        clock);

        // then
        assertThat(total.name()).isEqualTo("Total Price");
        Parameters params = Parameters.of("timestamp", LocalDateTime.of(2024, 1, 15, 0, 0));
        assertThat(total.calculate(params)).isEqualTo(Money.pln(123)); // 100 + 23
    }

    @Test
    void shouldChangeCompositionOverTime() {
        // given: initial composition with 2 children
        Component basePrice =
                SimpleComponent.withInitialVersion(
                        "Base Price",
                        new SimpleFixedCalculator("base", Money.pln(100)),
                        Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0)),
                        clock);
        Component tax =
                SimpleComponent.withInitialVersion(
                        "Tax",
                        new SimpleFixedCalculator("tax", Money.pln(23)),
                        Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0)),
                        clock);

        CompositeComponent total =
                CompositeComponent.withInitialVersion(
                        "Total Price",
                        List.of(basePrice, tax),
                        Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0)),
                        clock);

        // when: add new version with additional child (surcharge) from May 1st
        Component surcharge =
                SimpleComponent.withInitialVersion(
                        "Seasonal Surcharge",
                        new SimpleFixedCalculator("surcharge", Money.pln(10)),
                        Validity.from(LocalDateTime.of(2024, 5, 1, 0, 0)),
                        clock);

        CompositeComponentVersion newVersion =
                new CompositeComponentVersion(
                        List.of(basePrice, tax, surcharge),
                        java.util.Map.of(),
                        Validity.from(LocalDateTime.of(2024, 5, 1, 0, 0)),
                        now(clock).plusMinutes(10));
        total = total.updateWith(newVersion);

        // then: composition changes over time
        Parameters april = Parameters.of("timestamp", LocalDateTime.of(2024, 4, 15, 0, 0));
        assertThat(total.calculate(april)).isEqualTo(Money.pln(123)); // 100 + 23

        Parameters may = Parameters.of("timestamp", LocalDateTime.of(2024, 5, 15, 0, 0));
        assertThat(total.calculate(may)).isEqualTo(Money.pln(133)); // 100 + 23 + 10
    }

    @Test
    void shouldWorkWhenChildrenAreAlsoVersioned() {
        // given: child component with multiple versions
        Calculator baseCalc = new SimpleFixedCalculator("base", Money.pln(100));
        SimpleComponent basePrice =
                SimpleComponent.withInitialVersion(
                        "Base Price",
                        baseCalc,
                        Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0)),
                        clock);

        // Add discounted version in February
        Calculator discountCalc = new SimpleFixedCalculator("discount", Money.pln(80));
        SimpleComponentVersion discountVersion =
                new SimpleComponentVersion(
                        discountCalc,
                        java.util.Map.of(),
                        Validity.between(
                                LocalDateTime.of(2024, 2, 1, 0, 0),
                                LocalDateTime.of(2024, 3, 1, 0, 0)),
                        now(clock).plusMinutes(10));
        basePrice = basePrice.updateWith(discountVersion);

        Component tax =
                SimpleComponent.withInitialVersion(
                        "Tax",
                        new SimpleFixedCalculator("tax", Money.pln(23)),
                        Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0)),
                        clock);

        // when: create composite
        Component total =
                CompositeComponent.withInitialVersion(
                        "Total Price",
                        List.of(basePrice, tax),
                        Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0)),
                        clock);

        // then: both composite and child versioning work together
        Parameters jan15 = Parameters.of("timestamp", LocalDateTime.of(2024, 1, 15, 0, 0));
        assertThat(total.calculate(jan15)).isEqualTo(Money.pln(123)); // 100 + 23

        Parameters feb15 = Parameters.of("timestamp", LocalDateTime.of(2024, 2, 15, 0, 0));
        assertThat(total.calculate(feb15)).isEqualTo(Money.pln(103)); // 80 (discount) + 23

        Parameters mar15 = Parameters.of("timestamp", LocalDateTime.of(2024, 3, 15, 0, 0));
        assertThat(total.calculate(mar15)).isEqualTo(Money.pln(123)); // back to 100 + 23
    }

    @Test
    void shouldRemoveChildFromComposition() {
        // given: composition with 3 children
        Component basePrice =
                SimpleComponent.withInitialVersion(
                        "Base Price",
                        new SimpleFixedCalculator("base", Money.pln(100)),
                        Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0)),
                        clock);
        Component tax =
                SimpleComponent.withInitialVersion(
                        "Tax",
                        new SimpleFixedCalculator("tax", Money.pln(23)),
                        Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0)),
                        clock);
        Component surcharge =
                SimpleComponent.withInitialVersion(
                        "Surcharge",
                        new SimpleFixedCalculator("surcharge", Money.pln(10)),
                        Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0)),
                        clock);

        CompositeComponent total =
                CompositeComponent.withInitialVersion(
                        "Total Price",
                        List.of(basePrice, tax, surcharge),
                        Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0)),
                        clock);

        // when: remove surcharge from March 1st
        CompositeComponentVersion withoutSurcharge =
                new CompositeComponentVersion(
                        List.of(basePrice, tax), // only 2 children
                        java.util.Map.of(),
                        Validity.from(LocalDateTime.of(2024, 3, 1, 0, 0)),
                        now(clock).plusMinutes(10));
        total = total.updateWith(withoutSurcharge);

        // then
        Parameters feb = Parameters.of("timestamp", LocalDateTime.of(2024, 2, 15, 0, 0));
        assertThat(total.calculate(feb)).isEqualTo(Money.pln(133)); // 100 + 23 + 10

        Parameters mar = Parameters.of("timestamp", LocalDateTime.of(2024, 3, 15, 0, 0));
        assertThat(total.calculate(mar)).isEqualTo(Money.pln(123)); // 100 + 23 (no surcharge)
    }

    @Test
    void shouldReplaceChildInComposition() {
        // given: composition with base price and tax
        Component basePriceV1 =
                SimpleComponent.withInitialVersion(
                        "Base Price V1",
                        new SimpleFixedCalculator("base-v1", Money.pln(100)),
                        Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0)),
                        clock);
        Component tax =
                SimpleComponent.withInitialVersion(
                        "Tax",
                        new SimpleFixedCalculator("tax", Money.pln(23)),
                        Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0)),
                        clock);

        CompositeComponent total =
                CompositeComponent.withInitialVersion(
                        "Total Price",
                        List.of(basePriceV1, tax),
                        Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0)),
                        clock);

        // when: replace base price with completely different component from May
        Component basePriceV2 =
                SimpleComponent.withInitialVersion(
                        "Base Price V2",
                        new SimpleFixedCalculator("base-v2", Money.pln(150)),
                        Validity.from(LocalDateTime.of(2024, 5, 1, 0, 0)),
                        clock);

        CompositeComponentVersion newVersion =
                new CompositeComponentVersion(
                        List.of(basePriceV2, tax), // different base price component
                        java.util.Map.of(),
                        Validity.from(LocalDateTime.of(2024, 5, 1, 0, 0)),
                        now(clock).plusMinutes(10));
        total = total.updateWith(newVersion);

        // then
        Parameters april = Parameters.of("timestamp", LocalDateTime.of(2024, 4, 15, 0, 0));
        assertThat(total.calculate(april)).isEqualTo(Money.pln(123)); // 100 + 23

        Parameters may = Parameters.of("timestamp", LocalDateTime.of(2024, 5, 15, 0, 0));
        assertThat(total.calculate(may)).isEqualTo(Money.pln(173)); // 150 + 23
    }

    @Test
    void shouldWorkWithYoungestVersionWinningStrategy() {
        // given: composite with version from Jan 1st
        Component child =
                SimpleComponent.withInitialVersion(
                        "Child",
                        new SimpleFixedCalculator("child", Money.pln(100)),
                        Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0)),
                        clock);

        CompositeComponent composite =
                CompositeComponent.withInitialVersion(
                        "Composite",
                        List.of(child),
                        Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0)),
                        clock);

        // when: add two overlapping versions
        Component child2 =
                SimpleComponent.withInitialVersion(
                        "Child2",
                        new SimpleFixedCalculator("child2", Money.pln(200)),
                        Validity.from(LocalDateTime.of(2024, 2, 1, 0, 0)),
                        clock);
        CompositeComponentVersion version2 =
                new CompositeComponentVersion(
                        List.of(child2),
                        java.util.Map.of(),
                        Validity.from(LocalDateTime.of(2024, 2, 1, 0, 0)),
                        now(clock).plusMinutes(5));
        composite = composite.updateWith(version2);

        Component child3 =
                SimpleComponent.withInitialVersion(
                        "Child3",
                        new SimpleFixedCalculator("child3", Money.pln(300)),
                        Validity.from(LocalDateTime.of(2024, 2, 10, 0, 0)),
                        clock);
        CompositeComponentVersion version3 =
                new CompositeComponentVersion(
                        List.of(child3),
                        java.util.Map.of(),
                        Validity.from(LocalDateTime.of(2024, 2, 10, 0, 0)),
                        now(clock).plusMinutes(10));
        composite = composite.updateWith(version3);

        // then: youngest version wins at Feb 15
        Parameters feb15 = Parameters.of("timestamp", LocalDateTime.of(2024, 2, 15, 0, 0));
        assertThat(composite.calculate(feb15)).isEqualTo(Money.pln(300));
    }
}
