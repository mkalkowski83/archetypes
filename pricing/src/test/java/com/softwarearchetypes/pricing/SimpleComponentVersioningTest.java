package com.softwarearchetypes.pricing;

import static com.softwarearchetypes.pricing.ClockFixture.someFixedClock;
import static java.time.LocalDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.softwarearchetypes.quantity.money.Money;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SimpleComponentVersioningTest {

    static final Clock clock = someFixedClock();

    @Test
    void shouldCreateComponentWithInitialVersion() {
        // given
        Calculator calculator = new SimpleFixedCalculator("fixed-100", Money.pln(100));
        Validity validity = Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0));

        // when
        Component component =
                SimpleComponent.withInitialVersion("Base Price", calculator, validity, clock);

        // then
        assertThat(component.name()).isEqualTo("Base Price");
        assertThat(component.id()).isNotNull();
    }

    @Test
    void shouldCalculateUsingVersionValidAtGivenTimestamp() {
        // given: component with version valid from Jan 1st
        Calculator calculator = new SimpleFixedCalculator("fixed-100", Money.pln(100));
        Validity validity = Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0));
        Component component =
                SimpleComponent.withInitialVersion("Base Price", calculator, validity, clock);

        // when: calculate at Jan 15th
        Parameters params = Parameters.of("timestamp", LocalDateTime.of(2024, 1, 15, 0, 0));
        Money result = component.calculate(params);

        // then
        assertThat(result).isEqualTo(Money.pln(100));
    }

    @Test
    void shouldAddNewVersionAndKeepOldOne() {
        // given: component with base price 100 PLN from Jan 1st
        Calculator baseCalculator = new SimpleFixedCalculator("fixed-100", Money.pln(100));
        Validity baseValidity = Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0));
        SimpleComponent component =
                SimpleComponent.withInitialVersion(
                        "Base Price", baseCalculator, baseValidity, clock);

        // when: add discount version for February
        Calculator discountCalculator = new SimpleFixedCalculator("fixed-80", Money.pln(80));
        Validity discountValidity =
                Validity.between(
                        LocalDateTime.of(2024, 2, 1, 0, 0), LocalDateTime.of(2024, 3, 1, 0, 0));
        SimpleComponentVersion discountVersion =
                new SimpleComponentVersion(
                        discountCalculator, Map.of(), discountValidity, now(clock));
        SimpleComponent updated = component.updateWith(discountVersion);

        // then: calculate at different points in time
        Parameters jan15 = Parameters.of("timestamp", LocalDateTime.of(2024, 1, 15, 0, 0));
        assertThat(updated.calculate(jan15)).isEqualTo(Money.pln(100)); // base price

        Parameters feb15 = Parameters.of("timestamp", LocalDateTime.of(2024, 2, 15, 0, 0));
        assertThat(updated.calculate(feb15)).isEqualTo(Money.pln(80)); // discount

        Parameters mar15 = Parameters.of("timestamp", LocalDateTime.of(2024, 3, 15, 0, 0));
        assertThat(updated.calculate(mar15)).isEqualTo(Money.pln(100)); // back to base
    }

    @Test
    void shouldUseYoungestValidFromWhenVersionsOverlap() {
        // given: base version from Jan 1st forever
        Calculator baseCalculator = new SimpleFixedCalculator("base", Money.pln(100));
        Validity baseValidity = Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0));
        SimpleComponent component =
                SimpleComponent.withInitialVersion("Price", baseCalculator, baseValidity, clock);

        // when: add two overlapping versions (both valid at Feb 15)
        // Version 1: valid from Feb 1st
        Calculator calc1 = new SimpleFixedCalculator("v1", Money.pln(80));
        Validity validity1 = Validity.from(LocalDateTime.of(2024, 2, 1, 0, 0));
        component =
                component.updateWith(
                        new SimpleComponentVersion(calc1, Map.of(), validity1, now(clock)));

        // Version 2: valid from Feb 10th (younger = should win)
        Calculator calc2 = new SimpleFixedCalculator("v2", Money.pln(90));
        Validity validity2 = Validity.from(LocalDateTime.of(2024, 2, 10, 0, 0));
        component =
                component.updateWith(
                        new SimpleComponentVersion(calc2, Map.of(), validity2, now(clock)));

        // then: at Feb 15, version 2 (youngest) should be used
        Parameters feb15 = Parameters.of("timestamp", LocalDateTime.of(2024, 2, 15, 0, 0));
        assertThat(component.calculate(feb15)).isEqualTo(Money.pln(90));
    }

    @Test
    void shouldThrowExceptionWhenNoVersionValidAtTimestamp() {
        // given: version valid from Feb 1st
        Calculator calculator = new SimpleFixedCalculator("fixed", Money.pln(100));
        Validity validity = Validity.from(LocalDateTime.of(2024, 2, 1, 0, 0));
        Component component =
                SimpleComponent.withInitialVersion("Price", calculator, validity, clock);

        // when: try to calculate at Jan 15 (before any version)
        Parameters jan15 = Parameters.of("timestamp", LocalDateTime.of(2024, 1, 15, 0, 0));

        // then
        assertThatThrownBy(() -> component.calculate(jan15))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No version of component")
                .hasMessageContaining("valid at 2024-01-15");
    }

    @Test
    void shouldFallbackToCurrentTimeWhenTimestampNotProvided() {
        // given: version valid from past
        Calculator calculator = new SimpleFixedCalculator("fixed", Money.pln(100));
        Validity validity = Validity.from(LocalDateTime.of(2020, 1, 1, 0, 0));
        Component component =
                SimpleComponent.withInitialVersion("Price", calculator, validity, clock);

        // when: calculate without timestamp (uses LocalDateTime.now())
        Parameters params = Parameters.empty();
        Money result = component.calculate(params);

        // then: should work (assuming current time is after 2020-01-01)
        assertThat(result).isEqualTo(Money.pln(100));
    }

    @Test
    void shouldRejectVersionWithIdenticalValidityByDefault() {
        // given: component with version from Jan 1st
        Calculator calculator1 = new SimpleFixedCalculator("v1", Money.pln(100));
        Validity validity = Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0));
        SimpleComponent component =
                SimpleComponent.withInitialVersion("Price", calculator1, validity, clock);

        // when: try to add another version with same validity
        Calculator calculator2 = new SimpleFixedCalculator("v2", Money.pln(200));
        SimpleComponentVersion duplicate =
                new SimpleComponentVersion(calculator2, Map.of(), validity, now(clock));

        // then
        assertThatThrownBy(() -> component.updateWith(duplicate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("identical validity period");
    }

    @Test
    void shouldAllowVersionWithIdenticalValidityWhenUsingALLOW_ALL() {
        // given: component with version from Jan 1st
        Calculator calculator1 = new SimpleFixedCalculator("v1", Money.pln(100));
        Validity validity = Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0));
        SimpleComponent component =
                SimpleComponent.withInitialVersion("Price", calculator1, validity, clock);

        // when: add another version with same validity using ALLOW_ALL
        Calculator calculator2 = new SimpleFixedCalculator("v2", Money.pln(200));
        SimpleComponentVersion duplicate =
                new SimpleComponentVersion(
                        calculator2, Map.of(), validity, now(clock).plusMinutes(10));
        SimpleComponent updated = component.updateWith(duplicate, VersionUpdateStrategy.ALLOW_ALL);

        // then: should succeed, and newest wins
        Parameters params = Parameters.of("timestamp", LocalDateTime.of(2024, 1, 15, 0, 0));
        assertThat(updated.calculate(params)).isEqualTo(Money.pln(200));
    }

    @Test
    void shouldRejectOverlappingVersionsWhenUsingREJECT_OVERLAPPING() {
        // given: component with version from Jan 1st forever
        Calculator calculator1 = new SimpleFixedCalculator("v1", Money.pln(100));
        Validity validity1 = Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0));
        SimpleComponent component =
                SimpleComponent.withInitialVersion("Price", calculator1, validity1, clock);

        // when: try to add overlapping version (Feb-Mar) with REJECT_OVERLAPPING
        Calculator calculator2 = new SimpleFixedCalculator("v2", Money.pln(80));
        Validity validity2 =
                Validity.between(
                        LocalDateTime.of(2024, 2, 1, 0, 0), LocalDateTime.of(2024, 3, 1, 0, 0));
        SimpleComponentVersion overlapping =
                new SimpleComponentVersion(calculator2, Map.of(), validity2, now(clock));

        // then
        assertThatThrownBy(
                        () ->
                                component.updateWith(
                                        overlapping, VersionUpdateStrategy.REJECT_OVERLAPPING))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("overlaps");
    }

    @Test
    void shouldWorkWithParameterMappings() {
        // given: component with parameter mapping
        Calculator calculator =
                new StepFunctionCalculator(
                        "step", Money.pln(100), BigDecimal.valueOf(10), BigDecimal.valueOf(5));
        Map<String, String> mappings = Map.of("kwh", "quantity");
        Validity validity = Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0));

        Component component =
                SimpleComponent.withInitialVersion(
                        "Energy Charge", calculator, mappings, validity, clock);

        // when: calculate with component parameter name
        Parameters params =
                Parameters.of(
                        "timestamp", LocalDateTime.of(2024, 1, 15, 0, 0),
                        "kwh", BigDecimal.valueOf(15));
        Money result = component.calculate(params);

        // then: should map "kwh" to "quantity" internally
        assertThat(result).isEqualTo(Money.pln(105)); // base 100 + 1 step * 5
    }
}
