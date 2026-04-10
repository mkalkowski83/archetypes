package com.softwarearchetypes.planvsexecution.repaymentanalysis;

import static com.softwarearchetypes.planvsexecution.repaymentanalysis.TestInstants.instantUTC;
import static com.softwarearchetypes.quantity.money.Money.pln;
import static org.assertj.core.api.Assertions.assertThat;

import com.softwarearchetypes.planvsexecution.repaymentanalysis.delta.DeltaResult;
import com.softwarearchetypes.planvsexecution.repaymentanalysis.modification.ModificationRule;
import com.softwarearchetypes.planvsexecution.repaymentanalysis.modification.OnTimePaymentCondition;
import com.softwarearchetypes.planvsexecution.repaymentanalysis.modification.RemoveInstallmentModifier;
import com.softwarearchetypes.planvsexecution.repaymentanalysis.modification.SpreadRemainingAmountModifier;
import com.softwarearchetypes.planvsexecution.repaymentanalysis.tolerance.ToleranceBuilder;
import com.softwarearchetypes.planvsexecution.repaymentanalysis.tolerance.ToleranceStrategy;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class PaymentSimulationScenarios {

    ScheduleAnalysisFacade facade = ScheduleAnalysisConfiguration.facade();
    ScheduleModificationOrchestrator orchestrator = new ScheduleModificationOrchestrator(facade);

    @Test
    void simulation_three_on_time_payments_removes_one_installment() {
        // given
        PaymentSchedule planned =
                PaymentSchedule.of(
                        List.of(
                                Payment.of(instantUTC(2024, 1, 15, 0, 0), pln(100.00)),
                                Payment.of(instantUTC(2024, 2, 15, 0, 0), pln(100.00)),
                                Payment.of(instantUTC(2024, 3, 15, 0, 0), pln(100.00)),
                                Payment.of(instantUTC(2024, 4, 15, 0, 0), pln(100.00)),
                                Payment.of(instantUTC(2024, 5, 15, 0, 0), pln(100.00))));

        List<PaymentProcessed> execution =
                List.of(
                        PaymentProcessed.of(
                                instantUTC(2024, 1, 15, 0, 0), pln(100.00), Instant.now()),
                        PaymentProcessed.of(
                                instantUTC(2024, 2, 15, 0, 0), pln(100.00), Instant.now()),
                        PaymentProcessed.of(
                                instantUTC(2024, 3, 15, 0, 0), pln(100.00), Instant.now()));

        ConfigurablePaymentSchedule configurable =
                ConfigurablePaymentSchedule.builder()
                        .initialSchedule(planned)
                        .onOnTimePayment(3, new RemoveInstallmentModifier(3))
                        .build();

        ToleranceStrategy strategy = ToleranceBuilder.exact();

        // when
        orchestrator.analyzeAndApply(configurable, execution, strategy);

        // then
        assertThat(configurable.activeSchedule().size()).isEqualTo(4);
        assertThat(configurable.activeSchedule().totalAmount()).isEqualTo(pln(400.00));
    }

    @Test
    void simulation_one_late_payment_shortens_remaining_schedule() {
        // given
        PaymentSchedule planned =
                PaymentSchedule.of(
                        List.of(
                                Payment.of(instantUTC(2024, 1, 15, 0, 0), pln(100.00)),
                                Payment.of(instantUTC(2024, 2, 15, 0, 0), pln(100.00)),
                                Payment.of(instantUTC(2024, 3, 15, 0, 0), pln(100.00)),
                                Payment.of(instantUTC(2024, 4, 15, 0, 0), pln(100.00)),
                                Payment.of(instantUTC(2024, 5, 15, 0, 0), pln(100.00)),
                                Payment.of(instantUTC(2024, 6, 15, 0, 0), pln(100.00))));

        List<PaymentProcessed> events =
                List.of(
                        PaymentProcessed.of(
                                instantUTC(2024, 1, 20, 0, 0), pln(100.00), Instant.now()));

        ConfigurablePaymentSchedule configurable =
                ConfigurablePaymentSchedule.builder()
                        .initialSchedule(planned)
                        .onLatePayment(1, new SpreadRemainingAmountModifier(3))
                        .build();

        ToleranceStrategy strategy = ToleranceBuilder.tolerance().money(pln(0.01)).days(10).build();

        // when
        DeltaResult result = orchestrator.analyzeAndApply(configurable, events, strategy);

        // then
        assertThat(configurable.activeSchedule().size()).isEqualTo(4);
        assertThat(configurable.activeSchedule().totalAmount()).isEqualTo(pln(600.00));
        assertThat(configurable.activeSchedule().payments().get(0).amount()).isEqualTo(pln(100.00));
    }

    @Test
    void simulation_partial_payments_count_as_on_time() {
        // given
        PaymentSchedule planned =
                PaymentSchedule.of(
                        List.of(
                                Payment.of(instantUTC(2024, 1, 15, 0, 0), pln(100.00)),
                                Payment.of(instantUTC(2024, 2, 15, 0, 0), pln(100.00)),
                                Payment.of(instantUTC(2024, 3, 15, 0, 0), pln(100.00)),
                                Payment.of(instantUTC(2024, 4, 15, 0, 0), pln(100.00))));

        List<PaymentProcessed> events =
                List.of(
                        PaymentProcessed.of(
                                instantUTC(2024, 1, 10, 0, 0), pln(50.00), Instant.now()),
                        PaymentProcessed.of(
                                instantUTC(2024, 1, 14, 0, 0), pln(50.00), Instant.now()));

        ConfigurablePaymentSchedule configurable =
                ConfigurablePaymentSchedule.builder()
                        .initialSchedule(planned)
                        .onOnTimePayment(1, new RemoveInstallmentModifier(1))
                        .build();

        ToleranceStrategy strategy =
                ToleranceBuilder.partialPayments(pln(0.05), instantUTC(2024, 1, 15, 0, 0));

        // when
        DeltaResult result = orchestrator.analyzeAndApply(configurable, events, strategy);

        // then
        assertThat(result.statistics().onTimeCount()).isEqualTo(1);
        assertThat(configurable.activeSchedule().size()).isEqualTo(3);
        assertThat(configurable.activeSchedule().totalAmount()).isEqualTo(pln(300.00));
    }

    @Test
    void simulation_5_gross_tolerance_counts_as_paid() {
        // given
        PaymentSchedule planned =
                PaymentSchedule.of(
                        List.of(
                                Payment.of(instantUTC(2024, 1, 15, 0, 0), pln(100.00)),
                                Payment.of(instantUTC(2024, 2, 15, 0, 0), pln(100.00)),
                                Payment.of(instantUTC(2024, 3, 15, 0, 0), pln(100.00)),
                                Payment.of(instantUTC(2024, 4, 15, 0, 0), pln(100.00))));

        List<PaymentProcessed> events =
                List.of(
                        PaymentProcessed.of(
                                instantUTC(2024, 1, 14, 0, 0), pln(99.96), Instant.now()),
                        PaymentProcessed.of(
                                instantUTC(2024, 2, 14, 0, 0), pln(99.99), Instant.now()));

        ConfigurablePaymentSchedule configurable =
                ConfigurablePaymentSchedule.builder()
                        .initialSchedule(planned)
                        .onOnTimePayment(1, new RemoveInstallmentModifier(1))
                        .build();

        ToleranceStrategy strategy = ToleranceBuilder.moneyTolerance(pln(0.05));

        // when
        DeltaResult result = orchestrator.analyzeAndApply(configurable, events, strategy);

        // then
        assertThat(result.statistics().onTimeCount()).isEqualTo(2);
    }

    @Test
    void simulation_mixed_payments_trigger_multiple_rules() {
        // given
        PaymentSchedule planned =
                PaymentSchedule.of(
                        List.of(
                                Payment.of(instantUTC(2024, 1, 15, 0, 0), pln(100.00)),
                                Payment.of(instantUTC(2024, 2, 15, 0, 0), pln(100.00)),
                                Payment.of(instantUTC(2024, 3, 15, 0, 0), pln(100.00)),
                                Payment.of(instantUTC(2024, 4, 15, 0, 0), pln(100.00)),
                                Payment.of(instantUTC(2024, 5, 15, 0, 0), pln(100.00)),
                                Payment.of(instantUTC(2024, 6, 15, 0, 0), pln(100.00))));

        List<PaymentProcessed> events =
                List.of(
                        PaymentProcessed.of(
                                instantUTC(2024, 1, 15, 0, 0), pln(100.00), Instant.now()),
                        PaymentProcessed.of(
                                instantUTC(2024, 2, 22, 0, 0), pln(100.00), Instant.now()),
                        PaymentProcessed.of(
                                instantUTC(2024, 3, 15, 0, 0), pln(100.00), Instant.now()));

        ConfigurablePaymentSchedule configurable =
                ConfigurablePaymentSchedule.builder()
                        .initialSchedule(planned)
                        .onOnTimePayment(2, new RemoveInstallmentModifier(3))
                        .onLatePayment(1, new SpreadRemainingAmountModifier(2))
                        .build();

        ToleranceStrategy strategy = ToleranceBuilder.tolerance().money(pln(0.01)).days(10).build();

        // when
        DeltaResult result = orchestrator.analyzeAndApply(configurable, events, strategy);

        // then
        assertThat(result.statistics().onTimeCount()).isEqualTo(2);
        assertThat(result.statistics().lateCount()).isEqualTo(1);
        assertThat(configurable.activeSchedule().size()).isEqualTo(5);
        assertThat(configurable.activeSchedule().totalAmount()).isEqualTo(pln(500.00));
    }

    @Test
    void simulation_no_payments_schedule_unchanged() {
        // given
        PaymentSchedule planned =
                PaymentSchedule.of(
                        List.of(
                                Payment.of(instantUTC(2024, 1, 15, 0, 0), pln(100.00)),
                                Payment.of(instantUTC(2024, 2, 15, 0, 0), pln(100.00)),
                                Payment.of(instantUTC(2024, 3, 15, 0, 0), pln(100.00))));

        List<PaymentProcessed> events = List.of();

        ConfigurablePaymentSchedule configurable =
                ConfigurablePaymentSchedule.builder()
                        .initialSchedule(planned)
                        .onOnTimePayment(1, new RemoveInstallmentModifier(1))
                        .onLatePayment(1, new SpreadRemainingAmountModifier(1))
                        .build();

        ToleranceStrategy strategy = ToleranceBuilder.exact();

        // when
        DeltaResult result = orchestrator.analyzeAndApply(configurable, events, strategy);

        // then
        assertThat(configurable.activeSchedule().size()).isEqualTo(3);
        assertThat(configurable.activeSchedule().totalAmount()).isEqualTo(pln(300.00));
        assertThat(configurable.activeSchedule()).isEqualTo(planned);
    }

    @Test
    void simulation_progressive_schedule_modification_over_time() {
        // given
        PaymentSchedule planned =
                PaymentSchedule.of(
                        List.of(
                                Payment.of(instantUTC(2024, 1, 15, 0, 0), pln(100.00)),
                                Payment.of(instantUTC(2024, 2, 15, 0, 0), pln(100.00)),
                                Payment.of(instantUTC(2024, 3, 15, 0, 0), pln(100.00)),
                                Payment.of(instantUTC(2024, 4, 15, 0, 0), pln(100.00)),
                                Payment.of(instantUTC(2024, 5, 15, 0, 0), pln(100.00))));

        ConfigurablePaymentSchedule configurable =
                ConfigurablePaymentSchedule.builder()
                        .initialSchedule(planned)
                        .onOnTimePayment(1, new RemoveInstallmentModifier(1))
                        .build();

        ToleranceStrategy strategy = ToleranceBuilder.exact();

        // when
        List<PaymentProcessed> firstPayment =
                List.of(
                        PaymentProcessed.of(
                                instantUTC(2024, 1, 15, 0, 0), pln(100.00), Instant.now()));
        DeltaResult result1 = orchestrator.analyzeAndApply(configurable, firstPayment, strategy);

        int sizeAfterFirst = configurable.activeSchedule().size();

        List<PaymentProcessed> secondPayment =
                List.of(
                        PaymentProcessed.of(
                                instantUTC(2024, 1, 15, 0, 0), pln(100.00), Instant.now()),
                        PaymentProcessed.of(
                                instantUTC(2024, 2, 15, 0, 0), pln(100.00), Instant.now()));
        DeltaResult result2 = orchestrator.analyzeAndApply(configurable, secondPayment, strategy);

        int sizeAfterSecond = configurable.activeSchedule().size();

        // then
        assertThat(sizeAfterFirst).isEqualTo(4);
        assertThat(sizeAfterSecond).isEqualTo(4);
        assertThat(configurable.activeSchedule().totalAmount()).isEqualTo(pln(400.00));
    }

    @Test
    void simulation_all_payments_on_time_removes_last_installment() {
        // given
        PaymentSchedule planned =
                PaymentSchedule.of(
                        List.of(
                                Payment.of(instantUTC(2024, 1, 15, 0, 0), pln(100.00)),
                                Payment.of(instantUTC(2024, 2, 15, 0, 0), pln(100.00)),
                                Payment.of(instantUTC(2024, 3, 15, 0, 0), pln(100.00)),
                                Payment.of(instantUTC(2024, 4, 15, 0, 0), pln(100.00))));

        List<PaymentProcessed> events =
                List.of(
                        PaymentProcessed.of(
                                instantUTC(2024, 1, 15, 0, 0), pln(100.00), Instant.now()),
                        PaymentProcessed.of(
                                instantUTC(2024, 2, 15, 0, 0), pln(100.00), Instant.now()),
                        PaymentProcessed.of(
                                instantUTC(2024, 3, 15, 0, 0), pln(100.00), Instant.now()),
                        PaymentProcessed.of(
                                instantUTC(2024, 4, 15, 0, 0), pln(100.00), Instant.now()));

        ConfigurablePaymentSchedule configurable =
                ConfigurablePaymentSchedule.builder()
                        .initialSchedule(planned)
                        .addRule(
                                ModificationRule.once(
                                        OnTimePaymentCondition.atLeast(4),
                                        new RemoveInstallmentModifier(3)))
                        .build();

        ToleranceStrategy strategy = ToleranceBuilder.exact();

        // when
        DeltaResult result = orchestrator.analyzeAndApply(configurable, events, strategy);

        // then
        assertThat(result.isPerfectMatch()).isTrue();
        assertThat(result.statistics().onTimeCount()).isEqualTo(4);
        assertThat(configurable.activeSchedule().size()).isEqualTo(3);
        assertThat(configurable.activeSchedule().totalAmount()).isEqualTo(pln(300.00));
    }
}
