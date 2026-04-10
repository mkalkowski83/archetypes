package com.softwarearchetypes.graphs.scheduling;

import static com.softwarearchetypes.graphs.scheduling.DependencyType.*;
import static com.softwarearchetypes.graphs.scheduling.Fixtures.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class ScheduleTest {

    @Test
    void simpleLinearProcess() {
        // when
        Schedule schedule =
                Process.builder()
                        .addDependency(DRYING, MEASUREMENT, finishToStart("Sample must be dry"))
                        .addDependency(MEASUREMENT, ANALYSIS, dataFlow("Spectrum"))
                        .build();

        // then
        assertEquals(List.of(DRYING, MEASUREMENT, ANALYSIS), schedule.steps());
        assertEquals(DRYING, schedule.first());
        assertEquals(ANALYSIS, schedule.last());
    }

    @Test
    void complexProcessOrder() {
        // when
        Schedule schedule =
                Process.builder()
                        .addDependency(DRYING, MEASUREMENT)
                        .addDependency(CALIBRATION, MEASUREMENT)
                        .addDependency(MEASUREMENT, ANALYSIS)
                        .addDependency(ANALYSIS, VALIDATION)
                        .build();

        // then
        assertEquals(5, schedule.size());
        assertEquals(VALIDATION, schedule.last());
        assertTrue(schedule.steps().indexOf(DRYING) < schedule.steps().indexOf(MEASUREMENT));
        assertTrue(schedule.steps().indexOf(CALIBRATION) < schedule.steps().indexOf(MEASUREMENT));
        assertTrue(schedule.steps().indexOf(MEASUREMENT) < schedule.steps().indexOf(ANALYSIS));
        assertTrue(schedule.steps().indexOf(ANALYSIS) < schedule.steps().indexOf(VALIDATION));
    }

    @Test
    void singleStepProcess() {
        // when
        Schedule schedule = Process.builder().addStep(DRYING).build();

        // then
        assertEquals(1, schedule.size());
        assertEquals(DRYING, schedule.first());
        assertEquals(DRYING, schedule.last());
    }

    @Test
    void diamondPattern() {
        // when
        Schedule schedule =
                Process.builder()
                        .addDependency(PREPARATION, PATH_1)
                        .addDependency(PREPARATION, PATH_2)
                        .addDependency(PATH_1, FINALIZATION)
                        .addDependency(PATH_2, FINALIZATION)
                        .build();

        // then
        assertEquals(4, schedule.size());
        assertEquals(PREPARATION, schedule.first());
        assertEquals(FINALIZATION, schedule.last());
        assertTrue(schedule.steps().indexOf(PREPARATION) < schedule.steps().indexOf(PATH_1));
        assertTrue(schedule.steps().indexOf(PREPARATION) < schedule.steps().indexOf(PATH_2));
        assertTrue(schedule.steps().indexOf(PATH_1) < schedule.steps().indexOf(FINALIZATION));
        assertTrue(schedule.steps().indexOf(PATH_2) < schedule.steps().indexOf(FINALIZATION));
    }

    @Test
    void cyclicDependencyIsDetected() {
        // when & then
        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    Process.builder()
                            .addDependency(STEP_1, STEP_2)
                            .addDependency(STEP_2, STEP_3)
                            .addDependency(STEP_3, STEP_1)
                            .build();
                });
    }
}
