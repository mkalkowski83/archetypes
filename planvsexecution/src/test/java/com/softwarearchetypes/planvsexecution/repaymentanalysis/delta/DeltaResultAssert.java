package com.softwarearchetypes.planvsexecution.repaymentanalysis.delta;

import com.softwarearchetypes.quantity.money.Money;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

public class DeltaResultAssert extends AbstractAssert<DeltaResultAssert, DeltaResult> {

    public DeltaResultAssert(DeltaResult actual) {
        super(actual, DeltaResultAssert.class);
    }

    public static DeltaResultAssert assertThat(DeltaResult actual) {
        return new DeltaResultAssert(actual);
    }

    public DeltaResultAssert hasMatchedCount(int expected) {
        isNotNull();
        Assertions.assertThat(actual.matched())
                .as("Expected %d matched payments but found %d", expected, actual.matched().size())
                .hasSize(expected);
        return this;
    }

    public DeltaResultAssert hasUnderpaidCount(int expected) {
        isNotNull();
        Assertions.assertThat(actual.unmatchedPlanned())
                .as(
                        "Expected %d underpaid payments but found %d",
                        expected, actual.unmatchedPlanned().size())
                .hasSize(expected);
        return this;
    }

    public DeltaResultAssert hasOverpaidCount(int expected) {
        isNotNull();
        Assertions.assertThat(actual.unmatchedActual())
                .as(
                        "Expected %d overpaid payments but found %d",
                        expected, actual.unmatchedActual().size())
                .hasSize(expected);
        return this;
    }

    public DeltaResultAssert isPerfectMatch() {
        isNotNull();
        Assertions.assertThat(actual.isPerfectMatch())
                .as(
                        "Expected perfect match but found %d unmatched planned and %d unmatched actual",
                        actual.unmatchedPlanned().size(), actual.unmatchedActual().size())
                .isTrue();
        return this;
    }

    public DeltaResultAssert hasMatchRate(double expectedRate) {
        isNotNull();
        Assertions.assertThat(actual.matchRate())
                .as(
                        "Expected match rate %.2f%% but found %.2f%%",
                        expectedRate * 100, actual.matchRate() * 100)
                .isEqualTo(expectedRate, Assertions.within(0.01));
        return this;
    }

    public DeltaResultAssert hasTotalUnderpaidAmount(Money expected) {
        isNotNull();
        Assertions.assertThat(actual.statistics().totalUnderpaidAmount())
                .as(
                        "Expected total underpaid %s but found %s",
                        expected, actual.statistics().totalUnderpaidAmount())
                .isEqualTo(expected);
        return this;
    }

    public DeltaResultAssert hasNetDifference(Money expected) {
        isNotNull();
        Assertions.assertThat(actual.statistics().netDifference())
                .as(
                        "Expected net difference %s but found %s",
                        expected, actual.statistics().netDifference())
                .isEqualTo(expected);
        return this;
    }

    public DeltaResultAssert hasTotalOverpaidAmount(Money expected) {
        isNotNull();
        Assertions.assertThat(actual.statistics().totalOverpaidAmount())
                .as(
                        "Expected total overpaid %s but found %s",
                        expected, actual.statistics().totalOverpaidAmount())
                .isEqualTo(expected);
        return this;
    }

    public DeltaResultAssert hasNoUnderpayments() {
        isNotNull();
        Assertions.assertThat(actual.hasUnderpayments())
                .as("Expected no underpayments but found %d", actual.unmatchedPlanned().size())
                .isFalse();
        return this;
    }

    public DeltaResultAssert hasNoOverpayments() {
        isNotNull();
        Assertions.assertThat(actual.hasOverpayments())
                .as("Expected no overpayments but found %d", actual.unmatchedActual().size())
                .isFalse();
        return this;
    }
}
