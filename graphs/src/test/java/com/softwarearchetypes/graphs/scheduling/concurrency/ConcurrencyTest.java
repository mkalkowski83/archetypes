package com.softwarearchetypes.graphs.scheduling.concurrency;

import static org.junit.jupiter.api.Assertions.*;

import com.softwarearchetypes.graphs.scheduling.ProcessStep;
import org.junit.jupiter.api.Test;

class ConcurrencyTest {

    private static final ProcessStep MEASUREMENT = new ProcessStep("Measurement");
    private static final ProcessStep CALIBRATION = new ProcessStep("Calibration");
    private static final ProcessStep VALIDATION = new ProcessStep("Validation");
    private static final ProcessStep FINAL_TEST = new ProcessStep("Final Test");

    @Test
    void laboratoryStepsRequireMinimal3Environments() {
        // when
        ExecutionEnvironments environments =
                Concurrency.builder()
                        .addConflict(MEASUREMENT, CALIBRATION)
                        .addConflict(MEASUREMENT, VALIDATION)
                        .addConflict(FINAL_TEST, MEASUREMENT)
                        .addConflict(FINAL_TEST, CALIBRATION)
                        .addConflict(FINAL_TEST, VALIDATION)
                        .build();

        // then
        assertEquals(3, environments.environmentCount());
        assertTrue(environments.canRunConcurrently(CALIBRATION, VALIDATION));
        assertFalse(environments.canRunConcurrently(MEASUREMENT, CALIBRATION));
        assertFalse(environments.canRunConcurrently(MEASUREMENT, VALIDATION));
        assertFalse(environments.canRunConcurrently(FINAL_TEST, MEASUREMENT));
        assertFalse(environments.canRunConcurrently(FINAL_TEST, CALIBRATION));
        assertFalse(environments.canRunConcurrently(FINAL_TEST, VALIDATION));
    }

    @Test
    void noConflictsRequiresOneEnvironment() {
        // given
        ProcessStep step1 = new ProcessStep("Step 1");
        ProcessStep step2 = new ProcessStep("Step 2");
        ProcessStep step3 = new ProcessStep("Step 3");

        // when
        ExecutionEnvironments environments =
                Concurrency.builder().addStep(step1).addStep(step2).addStep(step3).build();

        // then
        assertEquals(1, environments.environmentCount());
        assertTrue(environments.canRunConcurrently(step1, step2));
        assertTrue(environments.canRunConcurrently(step2, step3));
        assertTrue(environments.canRunConcurrently(step1, step3));
    }

    @Test
    void completeConflictGraphRequiresMaxEnvironments() {
        // given
        ProcessStep step1 = new ProcessStep("Step 1");
        ProcessStep step2 = new ProcessStep("Step 2");
        ProcessStep step3 = new ProcessStep("Step 3");

        // when
        ExecutionEnvironments environments =
                Concurrency.builder()
                        .addConflict(step1, step2)
                        .addConflict(step1, step3)
                        .addConflict(step2, step3)
                        .build();

        // then
        assertEquals(3, environments.environmentCount());
        assertFalse(environments.canRunConcurrently(step1, step2));
        assertFalse(environments.canRunConcurrently(step1, step3));
        assertFalse(environments.canRunConcurrently(step2, step3));
    }

    @Test
    void chainConflictsRequireTwoEnvironments() {
        // given
        ProcessStep stepA = new ProcessStep("Step A");
        ProcessStep stepB = new ProcessStep("Step B");
        ProcessStep stepC = new ProcessStep("Step C");

        // when
        ExecutionEnvironments environments =
                Concurrency.builder().addConflict(stepA, stepB).addConflict(stepB, stepC).build();

        // then
        assertEquals(2, environments.environmentCount());
        assertFalse(environments.canRunConcurrently(stepA, stepB));
        assertFalse(environments.canRunConcurrently(stepB, stepC));
        assertTrue(environments.canRunConcurrently(stepA, stepC));
    }
}
