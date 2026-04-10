package com.softwarearchetypes.pricing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ValidityTest {

    @Test
    void shouldCreateValidityFromDate() {
        // given
        LocalDateTime from = LocalDateTime.of(2024, 1, 1, 0, 0);

        // when
        Validity validity = Validity.from(from);

        // then
        assertThat(validity.validFrom()).isEqualTo(from);
        assertThat(validity.validTo()).isEqualTo(LocalDateTime.MAX);
    }

    @Test
    void shouldCreateValidityBetweenDates() {
        // given
        LocalDateTime from = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2024, 2, 1, 0, 0);

        // when
        Validity validity = Validity.between(from, to);

        // then
        assertThat(validity.validFrom()).isEqualTo(from);
        assertThat(validity.validTo()).isEqualTo(to);
    }

    @Test
    void shouldRejectInvalidRange() {
        // given: from after to
        LocalDateTime from = LocalDateTime.of(2024, 2, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2024, 1, 1, 0, 0);

        // when & then
        assertThatThrownBy(() -> Validity.between(from, to))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("validFrom must be before validTo");
    }

    @Test
    void shouldCheckIfValidAt() {
        // given
        Validity validity =
                Validity.between(
                        LocalDateTime.of(2024, 2, 1, 0, 0), LocalDateTime.of(2024, 3, 1, 0, 0));

        // then
        assertThat(validity.isValidAt(LocalDateTime.of(2024, 1, 31, 23, 59))).isFalse(); // before
        assertThat(validity.isValidAt(LocalDateTime.of(2024, 2, 1, 0, 0))).isTrue(); // at start
        assertThat(validity.isValidAt(LocalDateTime.of(2024, 2, 15, 0, 0))).isTrue(); // middle
        assertThat(validity.isValidAt(LocalDateTime.of(2024, 2, 28, 23, 59)))
                .isTrue(); // before end
        assertThat(validity.isValidAt(LocalDateTime.of(2024, 3, 1, 0, 0)))
                .isFalse(); // at end (exclusive)
        assertThat(validity.isValidAt(LocalDateTime.of(2024, 3, 2, 0, 0))).isFalse(); // after
    }

    @Test
    void shouldDetectOverlappingPeriods() {
        // given
        Validity v1 =
                Validity.between(
                        LocalDateTime.of(2024, 1, 1, 0, 0), LocalDateTime.of(2024, 3, 1, 0, 0));
        Validity v2 =
                Validity.between(
                        LocalDateTime.of(2024, 2, 1, 0, 0), LocalDateTime.of(2024, 4, 1, 0, 0));

        // then
        assertThat(v1.overlaps(v2)).isTrue();
        assertThat(v2.overlaps(v1)).isTrue();
    }

    @Test
    void shouldDetectNonOverlappingPeriods() {
        // given
        Validity v1 =
                Validity.between(
                        LocalDateTime.of(2024, 1, 1, 0, 0), LocalDateTime.of(2024, 2, 1, 0, 0));
        Validity v2 =
                Validity.between(
                        LocalDateTime.of(2024, 2, 1, 0, 0), // starts where v1 ends
                        LocalDateTime.of(2024, 3, 1, 0, 0));

        // then
        assertThat(v1.overlaps(v2)).isFalse();
        assertThat(v2.overlaps(v1)).isFalse();
    }

    @Test
    void shouldHandleOpenEndedValidity() {
        // given
        Validity openEnded = Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0));
        Validity limited =
                Validity.between(
                        LocalDateTime.of(2024, 2, 1, 0, 0), LocalDateTime.of(2024, 3, 1, 0, 0));

        // then: open-ended overlaps with limited period
        assertThat(openEnded.overlaps(limited)).isTrue();
        assertThat(limited.overlaps(openEnded)).isTrue();

        // and: open-ended is valid far in the future
        assertThat(openEnded.isValidAt(LocalDateTime.of(2100, 1, 1, 0, 0))).isTrue();
    }

    @Test
    void shouldCheckIfExpired() {
        // given
        Validity validity =
                Validity.between(
                        LocalDateTime.of(2024, 1, 1, 0, 0), LocalDateTime.of(2024, 2, 1, 0, 0));

        // then
        assertThat(validity.hasExpired(LocalDateTime.of(2024, 1, 15, 0, 0))).isFalse();
        assertThat(validity.hasExpired(LocalDateTime.of(2024, 2, 1, 0, 0))).isTrue();
        assertThat(validity.hasExpired(LocalDateTime.of(2024, 3, 1, 0, 0))).isTrue();
    }

    @Test
    void shouldCheckIfNotStartedYet() {
        // given
        Validity validity = Validity.from(LocalDateTime.of(2024, 2, 1, 0, 0));

        // then
        assertThat(validity.hasNotStartedYet(LocalDateTime.of(2024, 1, 15, 0, 0))).isTrue();
        assertThat(validity.hasNotStartedYet(LocalDateTime.of(2024, 2, 1, 0, 0))).isFalse();
        assertThat(validity.hasNotStartedYet(LocalDateTime.of(2024, 3, 1, 0, 0))).isFalse();
    }

    @Test
    void shouldCreateAlwaysValidity() {
        // when
        Validity always = Validity.always();

        // then
        assertThat(always.isValidAt(LocalDateTime.MIN)).isTrue();
        assertThat(always.isValidAt(LocalDateTime.of(2024, 1, 1, 0, 0))).isTrue();
        assertThat(always.isValidAt(LocalDateTime.MAX.minusYears(1))).isTrue();
    }
}
