package com.softwarearchetypes.planvsexecution.repaymentanalysis;

import static com.softwarearchetypes.planvsexecution.repaymentanalysis.TestInstants.instantUTC;
import static com.softwarearchetypes.quantity.money.Money.pln;
import static org.assertj.core.api.Assertions.assertThat;

import com.softwarearchetypes.planvsexecution.repaymentanalysis.delta.DeltaResult;
import com.softwarearchetypes.planvsexecution.repaymentanalysis.modification.*;
import com.softwarearchetypes.planvsexecution.repaymentanalysis.tolerance.ToleranceBuilder;
import com.softwarearchetypes.planvsexecution.repaymentanalysis.tolerance.ToleranceStrategy;
import java.util.List;
import org.junit.jupiter.api.Test;

class ConfigurablePaymentScheduleScenarios {

    ScheduleAnalysisFacade facade = ScheduleAnalysisConfiguration.facade();
    ScheduleModificationOrchestrator orchestrator = new ScheduleModificationOrchestrator(facade);

    @Test
    void late_payment_triggers_schedule_shortening() {
        // given
        PaymentSchedule planned =
                PaymentSchedule.of(
                        List.of(
                                Payment.of(instantUTC(2024, 1, 15, 0, 0), pln(100.00)),
                                Payment.of(instantUTC(2024, 2, 15, 0, 0), pln(100.00)),
                                Payment.of(instantUTC(2024, 3, 15, 0, 0), pln(100.00)),
                                Payment.of(instantUTC(2024, 4, 15, 0, 0), pln(100.00)),
                                Payment.of(instantUTC(2024, 5, 15, 0, 0), pln(100.00))));

        PaymentSchedule actual =
                PaymentSchedule.of(List.of(Payment.of(instantUTC(2024, 1, 20, 0, 0), pln(100.00))));

        ConfigurablePaymentSchedule configurable =
                ConfigurablePaymentSchedule.builder()
                        .initialSchedule(planned)
                        .onLatePayment(1, new SpreadRemainingAmountModifier(3))
                        .build();

        // when
        ToleranceStrategy strategy = ToleranceBuilder.tolerance().money(pln(0.01)).days(10).build();
        DeltaResult result = orchestrator.analyzeAndApply(configurable, actual, strategy);

        // then
        assertThat(result.statistics().lateCount()).isEqualTo(1);
        assertThat(configurable.activeSchedule().size()).isEqualTo(4);
        assertThat(configurable.activeSchedule().totalAmount()).isEqualTo(pln(500.00));
    }

    @Test
    void on_time_payment_triggers_installment_removal() {
        // given
        PaymentSchedule planned =
                PaymentSchedule.of(
                        List.of(
                                Payment.of(instantUTC(2024, 1, 15, 0, 0), pln(100.00)),
                                Payment.of(instantUTC(2024, 2, 15, 0, 0), pln(100.00)),
                                Payment.of(instantUTC(2024, 3, 15, 0, 0), pln(100.00))));

        PaymentSchedule actual =
                PaymentSchedule.of(List.of(Payment.of(instantUTC(2024, 1, 15, 0, 0), pln(100.00))));

        ConfigurablePaymentSchedule configurable =
                ConfigurablePaymentSchedule.builder()
                        .initialSchedule(planned)
                        .onOnTimePayment(1, new RemoveInstallmentModifier(1))
                        .build();

        // when
        ToleranceStrategy strategy = ToleranceBuilder.exact();
        DeltaResult result = orchestrator.analyzeAndApply(configurable, actual, strategy);

        // then
        assertThat(result.statistics().onTimeCount()).isEqualTo(1);
        assertThat(configurable.activeSchedule().size()).isEqualTo(2);
        assertThat(configurable.activeSchedule().totalAmount()).isEqualTo(pln(200.00));
    }

    @Test
    void multiple_rules_can_be_applied() {
        // given
        PaymentSchedule planned =
                PaymentSchedule.of(
                        List.of(
                                Payment.of(instantUTC(2024, 1, 15, 0, 0), pln(100.00)),
                                Payment.of(instantUTC(2024, 2, 15, 0, 0), pln(100.00)),
                                Payment.of(instantUTC(2024, 3, 15, 0, 0), pln(100.00)),
                                Payment.of(instantUTC(2024, 4, 15, 0, 0), pln(100.00))));

        PaymentSchedule actual =
                PaymentSchedule.of(
                        List.of(
                                Payment.of(instantUTC(2024, 1, 15, 0, 0), pln(100.00)),
                                Payment.of(instantUTC(2024, 2, 20, 0, 0), pln(100.00))));

        ConfigurablePaymentSchedule configurable =
                ConfigurablePaymentSchedule.builder()
                        .initialSchedule(planned)
                        .addRule(
                                ModificationRule.once(
                                        OnTimePaymentCondition.atLeast(1),
                                        new RemoveInstallmentModifier(2)))
                        .addRule(
                                ModificationRule.once(
                                        LatePaymentCondition.atLeast(1),
                                        new SpreadRemainingAmountModifier(1)))
                        .build();

        // when
        ToleranceStrategy strategy = ToleranceBuilder.tolerance().money(pln(0.01)).days(10).build();
        DeltaResult result = orchestrator.analyzeAndApply(configurable, actual, strategy);

        // then
        assertThat(result.statistics().onTimeCount()).isEqualTo(1);
        assertThat(result.statistics().lateCount()).isEqualTo(1);
        assertThat(configurable.activeSchedule().size()).isEqualTo(3);
    }

    @Test
    void spread_remaining_amount_preserves_total() {
        // given
        PaymentSchedule planned =
                PaymentSchedule.of(
                        List.of(
                                Payment.of(instantUTC(2024, 1, 15, 0, 0), pln(100.00)),
                                Payment.of(instantUTC(2024, 2, 15, 0, 0), pln(100.00)),
                                Payment.of(instantUTC(2024, 3, 15, 0, 0), pln(100.00)),
                                Payment.of(instantUTC(2024, 4, 15, 0, 0), pln(100.00)),
                                Payment.of(instantUTC(2024, 5, 15, 0, 0), pln(100.00))));

        SpreadRemainingAmountModifier modifier = new SpreadRemainingAmountModifier(2);

        // Simulate 1 payment made to get DeltaResult with matched().size() == 1
        PaymentSchedule actual =
                PaymentSchedule.of(List.of(Payment.of(instantUTC(2024, 1, 15, 0, 0), pln(100.00))));
        DeltaResult deltaResult = facade.analyze(planned, actual, ToleranceBuilder.exact());

        // when
        PaymentSchedule modified = modifier.modify(planned, deltaResult);

        // then
        assertThat(modified.size()).isEqualTo(3);
        assertThat(modified.totalAmount()).isEqualTo(pln(500.00));
    }

    @Test
    void remove_installment_modifier_works() {
        // given
        PaymentSchedule planned =
                PaymentSchedule.of(
                        List.of(
                                Payment.of(instantUTC(2024, 1, 15, 0, 0), pln(100.00)),
                                Payment.of(instantUTC(2024, 2, 15, 0, 0), pln(100.00)),
                                Payment.of(instantUTC(2024, 3, 15, 0, 0), pln(100.00))));

        RemoveInstallmentModifier modifier = new RemoveInstallmentModifier(1);

        // Empty DeltaResult (not used by RemoveInstallmentModifier)
        PaymentSchedule actual = PaymentSchedule.empty();
        DeltaResult deltaResult = facade.analyze(planned, actual, ToleranceBuilder.exact());

        // when
        PaymentSchedule modified = modifier.modify(planned, deltaResult);

        // then
        assertThat(modified.size()).isEqualTo(2);
        assertThat(modified.payments().get(0).amount()).isEqualTo(pln(100.00));
        assertThat(modified.payments().get(1).amount()).isEqualTo(pln(100.00));
        assertThat(modified.payments().get(0).when()).isEqualTo(instantUTC(2024, 1, 15, 0, 0));
        assertThat(modified.payments().get(1).when()).isEqualTo(instantUTC(2024, 3, 15, 0, 0));
    }
}
