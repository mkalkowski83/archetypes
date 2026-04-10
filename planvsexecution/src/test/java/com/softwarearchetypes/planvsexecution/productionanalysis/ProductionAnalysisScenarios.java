package com.softwarearchetypes.planvsexecution.productionanalysis;

import static org.assertj.core.api.Assertions.assertThat;

import com.softwarearchetypes.planvsexecution.productionanalysis.delta.DeltaResult;
import com.softwarearchetypes.planvsexecution.productionanalysis.tolerance.ToleranceBuilder;
import com.softwarearchetypes.planvsexecution.productionanalysis.tolerance.ToleranceStrategy;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProductionAnalysisScenarios {

    ProductionAnalysisFacade facade = new ProductionAnalysisFacade();

    @Test
    void exact_matching_detects_quantity_deviations() {
        // given
        ProductionPlan planned =
                ProductionPlan.of(
                        List.of(
                                PlannedProduction.of("WIDGET-A", 100),
                                PlannedProduction.of("WIDGET-B", 200),
                                PlannedProduction.of("WIDGET-C", 150)));

        List<ActualProduction> actual =
                List.of(
                        ActualProduction.of("WIDGET-A", 98),
                        ActualProduction.of("WIDGET-B", 205),
                        ActualProduction.of("WIDGET-C", 150));

        // when
        ToleranceStrategy exactMatch = ToleranceBuilder.exact();
        DeltaResult result = facade.analyze(planned, actual, exactMatch);

        // then
        assertThat(result.matched()).hasSize(1);
        assertThat(result.unmatchedPlanned()).hasSize(2);
        assertThat(result.statistics().totalUnderProducedQuantity()).isEqualTo(300);
    }

    @Test
    void tolerance_strategy_allows_acceptable_deviations() {
        // given
        ProductionPlan planned =
                ProductionPlan.of(
                        List.of(
                                PlannedProduction.of("WIDGET-A", 100),
                                PlannedProduction.of("WIDGET-B", 200),
                                PlannedProduction.of("WIDGET-C", 150)));

        List<ActualProduction> actual =
                List.of(
                        ActualProduction.of("WIDGET-A", 98),
                        ActualProduction.of("WIDGET-B", 205),
                        ActualProduction.of("WIDGET-C", 148));

        // when
        ToleranceStrategy lenient = ToleranceBuilder.quantityTolerance(5.0, 10);
        DeltaResult result = facade.analyze(planned, actual, lenient);

        // then
        assertThat(result.matched()).hasSize(3);
        assertThat(result.unmatchedPlanned()).isEmpty();
        assertThat(result.isPerfectMatch()).isFalse();
    }

    @Test
    void split_production_aggregates_partial_batches() {
        // given
        ProductionPlan planned = ProductionPlan.of(List.of(PlannedProduction.of("WIDGET-A", 500)));

        List<ActualProduction> actual =
                List.of(
                        ActualProduction.of("WIDGET-A", 180),
                        ActualProduction.of("WIDGET-A", 170),
                        ActualProduction.of("WIDGET-A", 155));

        // when
        ToleranceStrategy tolerance = ToleranceBuilder.quantityTolerance(5.0, 10);
        DeltaResult result = facade.analyze(planned, actual, tolerance);

        // then
        assertThat(result.matched()).hasSize(1);
        assertThat(result.matched().get(0).actual()).hasSize(3);
        assertThat(result.matched().get(0).totalProducedQuantity()).isEqualTo(505);
        assertThat(result.isPerfectMatch()).isFalse();
    }

    @Test
    void under_production_identifies_missing_output() {
        // given
        ProductionPlan planned =
                ProductionPlan.of(
                        List.of(
                                PlannedProduction.of("WIDGET-A", 100),
                                PlannedProduction.of("WIDGET-B", 200)));

        List<ActualProduction> actual = List.of(ActualProduction.of("WIDGET-A", 100));

        // when
        ToleranceStrategy tolerance = ToleranceBuilder.exact();
        DeltaResult result = facade.analyze(planned, actual, tolerance);

        // then
        assertThat(result.hasUnderProduction()).isTrue();
        assertThat(result.unmatchedPlanned()).hasSize(1);
        assertThat(result.unmatchedPlanned().get(0).productId()).isEqualTo("WIDGET-B");
        assertThat(result.statistics().totalUnderProducedQuantity()).isEqualTo(200);
    }

    @Test
    void match_rate_tracks_execution_completeness() {
        // given
        ProductionPlan planned =
                ProductionPlan.of(
                        List.of(
                                PlannedProduction.of("WIDGET-A", 100),
                                PlannedProduction.of("WIDGET-B", 200),
                                PlannedProduction.of("WIDGET-C", 150),
                                PlannedProduction.of("WIDGET-D", 180),
                                PlannedProduction.of("WIDGET-E", 120)));

        List<ActualProduction> actual =
                List.of(
                        ActualProduction.of("WIDGET-A", 100),
                        ActualProduction.of("WIDGET-B", 200),
                        ActualProduction.of("WIDGET-C", 150));

        // when
        ToleranceStrategy tolerance = ToleranceBuilder.exact();
        DeltaResult result = facade.analyze(planned, actual, tolerance);

        // then
        assertThat(result.matchRate()).isEqualTo(0.6);
        assertThat(result.totalPlannedProducts()).isEqualTo(5);
        assertThat(result.matched()).hasSize(3);
        assertThat(result.unmatchedPlanned()).hasSize(2);
    }

    @Test
    void over_production_detected() {
        // given
        ProductionPlan planned = ProductionPlan.of(List.of(PlannedProduction.of("WIDGET-A", 100)));

        List<ActualProduction> actual = List.of(ActualProduction.of("WIDGET-A", 150));

        // when
        ToleranceStrategy tolerance = ToleranceBuilder.exact();
        DeltaResult result = facade.analyze(planned, actual, tolerance);

        // then
        assertThat(result.hasOverProduction()).isTrue();
        assertThat(result.statistics().totalOverProducedQuantity()).isEqualTo(150);
        assertThat(result.statistics().netQuantityDifference()).isEqualTo(50);
    }
}
