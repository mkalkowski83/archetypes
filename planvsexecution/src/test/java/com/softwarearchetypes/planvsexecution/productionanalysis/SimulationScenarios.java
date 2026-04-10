package com.softwarearchetypes.planvsexecution.productionanalysis;

import static org.assertj.core.api.Assertions.assertThat;

import com.softwarearchetypes.planvsexecution.productionanalysis.delta.DeltaResult;
import com.softwarearchetypes.planvsexecution.productionanalysis.modification.IncreaseBufferModifier;
import com.softwarearchetypes.planvsexecution.productionanalysis.tolerance.ToleranceBuilder;
import com.softwarearchetypes.planvsexecution.productionanalysis.tolerance.ToleranceStrategy;
import java.util.List;
import org.junit.jupiter.api.Test;

class SimulationScenarios {

    ProductionAnalysisFacade facade = new ProductionAnalysisFacade();
    PlanModificationOrchestrator orchestrator = new PlanModificationOrchestrator(facade);

    @Test
    void under_production_triggers_buffer_increase_simulation() {
        // given
        ProductionPlan initialPlan =
                ProductionPlan.of(
                        List.of(
                                PlannedProduction.of("WIDGET-A", 100),
                                PlannedProduction.of("WIDGET-B", 200),
                                PlannedProduction.of("WIDGET-C", 150)));

        ConfigurableProductionPlan configurable =
                ConfigurableProductionPlan.builder()
                        .initialPlan(initialPlan)
                        .onUnderProduction(50, IncreaseBufferModifier.by(10.0))
                        .build();

        List<ActualProduction> actual =
                List.of(ActualProduction.of("WIDGET-A", 90), ActualProduction.of("WIDGET-B", 150));

        // when
        ToleranceStrategy tolerance = ToleranceBuilder.exact();
        DeltaResult result = orchestrator.analyzeAndApply(configurable, actual, tolerance);

        // then
        assertThat(result.statistics().totalUnderProducedQuantity()).isEqualTo(450);

        ProductionPlan modifiedPlan = configurable.activePlan();
        assertThat(modifiedPlan.targets()).hasSize(3);

        assertThat(modifiedPlan.targets().get(0).targetQuantity()).isEqualTo(110);
        assertThat(modifiedPlan.targets().get(1).targetQuantity()).isEqualTo(220);
        assertThat(modifiedPlan.targets().get(2).targetQuantity()).isEqualTo(165);
    }

    @Test
    void simulation_does_not_change_reality() {
        // given
        ProductionPlan originalPlan =
                ProductionPlan.of(List.of(PlannedProduction.of("WIDGET-A", 100)));

        ConfigurableProductionPlan configurable =
                ConfigurableProductionPlan.builder()
                        .initialPlan(originalPlan)
                        .onUnderProduction(30, IncreaseBufferModifier.by(20.0))
                        .build();

        List<ActualProduction> actual = List.of(ActualProduction.of("WIDGET-A", 70));

        ToleranceStrategy tolerance = ToleranceBuilder.exact();

        // when
        DeltaResult result = orchestrator.analyzeAndApply(configurable, actual, tolerance);

        // then
        assertThat(result.statistics().totalUnderProducedQuantity()).isEqualTo(100);
        assertThat(configurable.activePlan().targets().get(0).targetQuantity()).isEqualTo(120);
        assertThat(originalPlan.targets().get(0).targetQuantity()).isEqualTo(100);
    }

    @Test
    void modification_rule_applied_only_once() {
        // given
        ProductionPlan initialPlan =
                ProductionPlan.of(List.of(PlannedProduction.of("WIDGET-A", 100)));

        ConfigurableProductionPlan configurable =
                ConfigurableProductionPlan.builder()
                        .initialPlan(initialPlan)
                        .onUnderProduction(50, IncreaseBufferModifier.by(10.0))
                        .build();

        ToleranceStrategy tolerance = ToleranceBuilder.exact();

        List<ActualProduction> firstActual = List.of(ActualProduction.of("WIDGET-A", 50));

        // when
        orchestrator.analyzeAndApply(configurable, firstActual, tolerance);

        // then
        assertThat(configurable.activePlan().targets().get(0).targetQuantity()).isEqualTo(110);

        // when
        List<ActualProduction> secondActual = List.of(ActualProduction.of("WIDGET-A", 60));
        orchestrator.analyzeAndApply(configurable, secondActual, tolerance);

        // then
        assertThat(configurable.activePlan().targets().get(0).targetQuantity()).isEqualTo(110);
    }

    @Test
    void multiple_simulations_answer_what_if_questions() {
        // given
        ProductionPlan basePlan =
                ProductionPlan.of(
                        List.of(
                                PlannedProduction.of("WIDGET-A", 100),
                                PlannedProduction.of("WIDGET-B", 200)));

        ConfigurableProductionPlan scenario1 =
                ConfigurableProductionPlan.builder()
                        .initialPlan(basePlan)
                        .onUnderProduction(40, IncreaseBufferModifier.by(15.0))
                        .build();

        List<ActualProduction> underBy20Percent =
                List.of(ActualProduction.of("WIDGET-A", 80), ActualProduction.of("WIDGET-B", 160));

        // when
        orchestrator.analyzeAndApply(scenario1, underBy20Percent, ToleranceBuilder.exact());

        // then
        assertThat(scenario1.activePlan().targets().get(0).targetQuantity()).isEqualTo(115);
        assertThat(scenario1.activePlan().targets().get(1).targetQuantity()).isEqualTo(230);

        // given
        ConfigurableProductionPlan scenario2 =
                ConfigurableProductionPlan.builder()
                        .initialPlan(basePlan)
                        .onUnderProduction(60, IncreaseBufferModifier.by(25.0))
                        .build();

        List<ActualProduction> underBy30Percent =
                List.of(ActualProduction.of("WIDGET-A", 70), ActualProduction.of("WIDGET-B", 140));

        // when
        orchestrator.analyzeAndApply(scenario2, underBy30Percent, ToleranceBuilder.exact());

        // then
        assertThat(scenario2.activePlan().targets().get(0).targetQuantity()).isEqualTo(125);
        assertThat(scenario2.activePlan().targets().get(1).targetQuantity()).isEqualTo(250);
    }
}
