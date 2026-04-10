package com.softwarearchetypes.planvsexecution.repaymentanalysis;

import static com.softwarearchetypes.planvsexecution.repaymentanalysis.TestInstants.instantUTC;
import static com.softwarearchetypes.quantity.money.Money.pln;
import static org.assertj.core.api.Assertions.assertThat;

import com.softwarearchetypes.planvsexecution.repaymentanalysis.delta.DeltaResult;
import com.softwarearchetypes.planvsexecution.repaymentanalysis.tolerance.ToleranceBuilder;
import com.softwarearchetypes.planvsexecution.repaymentanalysis.tolerance.ToleranceStrategy;
import java.util.List;
import org.junit.jupiter.api.Test;

class PaymentScheduleAnalysisScenarios {

    ScheduleAnalysisFacade facade = ScheduleAnalysisConfiguration.facade();

    @Test
    void exact_matching_identifies_penny_deviations() {
        // given
        PaymentSchedule planned =
                PaymentSchedule.of(
                        List.of(
                                Payment.of(instantUTC(2024, 1, 15, 0, 0), pln(100.00)),
                                Payment.of(instantUTC(2024, 2, 15, 0, 0), pln(100.00)),
                                Payment.of(instantUTC(2024, 3, 15, 0, 0), pln(100.00))));
        PaymentSchedule actual =
                PaymentSchedule.of(
                        List.of(
                                Payment.of(instantUTC(2024, 1, 15, 0, 0), pln(99.95)),
                                Payment.of(instantUTC(2024, 2, 15, 0, 0), pln(100.03)),
                                Payment.of(instantUTC(2024, 3, 15, 0, 0), pln(100.00))));

        // when
        ToleranceStrategy exactMatch = ToleranceBuilder.exact();
        DeltaResult result = facade.analyze(planned, actual, exactMatch);

        // then
        assertThat(result.matched()).hasSize(1);
        assertThat(result.unmatchedPlanned()).hasSize(2);
        assertThat(result.statistics().totalUnderpaidAmount()).isEqualTo(pln(200.00));
    }

    @Test
    void five_groszy_tolerance_matches_penny_deviations() {
        // given
        PaymentSchedule planned =
                PaymentSchedule.of(
                        List.of(
                                Payment.of(instantUTC(2024, 1, 15, 0, 0), pln(100.00)),
                                Payment.of(instantUTC(2024, 2, 15, 0, 0), pln(100.00)),
                                Payment.of(instantUTC(2024, 3, 15, 0, 0), pln(100.00))));
        PaymentSchedule actual =
                PaymentSchedule.of(
                        List.of(
                                Payment.of(instantUTC(2024, 1, 15, 0, 0), pln(99.95)),
                                Payment.of(instantUTC(2024, 2, 15, 0, 0), pln(100.03)),
                                Payment.of(instantUTC(2024, 3, 15, 0, 0), pln(100.00))));

        // when
        ToleranceStrategy lenient = ToleranceBuilder.fiveGroszy();
        DeltaResult result = facade.analyze(planned, actual, lenient);

        // then
        assertThat(result.matched()).hasSize(3);
        assertThat(result.unmatchedPlanned()).isEmpty();
        assertThat(result.isPerfectMatch()).isTrue();
    }

    @Test
    void partial_payments_sum_to_installment() {
        // given
        PaymentSchedule planned =
                PaymentSchedule.of(List.of(Payment.of(instantUTC(2024, 1, 15, 0, 0), pln(100.00))));
        PaymentSchedule actual =
                PaymentSchedule.of(
                        List.of(
                                Payment.of(instantUTC(2024, 1, 10, 0, 0), pln(40.00)),
                                Payment.of(instantUTC(2024, 1, 12, 0, 0), pln(30.00)),
                                Payment.of(instantUTC(2024, 1, 14, 0, 0), pln(30.00))));

        // when
        ToleranceStrategy partials =
                ToleranceBuilder.partialPayments(pln(0.05), instantUTC(2024, 1, 15, 0, 0));
        DeltaResult result = facade.analyze(planned, actual, partials);

        // then
        assertThat(result.matched()).hasSize(1);
        assertThat(result.matched().get(0).actual()).hasSize(3);
        assertThat(result.matched().get(0).totalActualAmount()).isEqualTo(pln(100.00));
        assertThat(result.isPerfectMatch()).isTrue();
    }

    @Test
    void on_time_and_late_payments_tracked() {
        // given
        PaymentSchedule planned =
                PaymentSchedule.of(
                        List.of(
                                Payment.of(instantUTC(2024, 1, 15, 0, 0), pln(100.00)),
                                Payment.of(instantUTC(2024, 2, 15, 0, 0), pln(100.00))));
        PaymentSchedule actual =
                PaymentSchedule.of(
                        List.of(
                                Payment.of(instantUTC(2024, 1, 14, 0, 0), pln(100.00)),
                                Payment.of(instantUTC(2024, 2, 20, 0, 0), pln(100.00))));

        // when
        ToleranceStrategy strategy = ToleranceBuilder.tolerance().money(pln(0.01)).days(10).build();
        DeltaResult result = facade.analyze(planned, actual, strategy);

        // then
        assertThat(result.statistics().onTimeCount()).isEqualTo(1);
        assertThat(result.statistics().lateCount()).isEqualTo(1);
    }
}
