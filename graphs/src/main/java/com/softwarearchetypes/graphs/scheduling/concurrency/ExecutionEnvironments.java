package com.softwarearchetypes.graphs.scheduling.concurrency;

import com.softwarearchetypes.graphs.scheduling.ProcessStep;
import java.util.*;

record ExecutionEnvironments(Map<ProcessStep, Integer> stepToEnvironment) {
    ExecutionEnvironments {
        stepToEnvironment = Collections.unmodifiableMap(new HashMap<>(stepToEnvironment));
    }

    int environmentCount() {
        return stepToEnvironment.values().stream()
                .max(Integer::compare)
                .map(max -> max + 1)
                .orElse(0);
    }

    Integer getEnvironment(ProcessStep step) {
        return stepToEnvironment.get(step);
    }

    Set<ProcessStep> getStepsInEnvironment(int environment) {
        Set<ProcessStep> steps = new HashSet<>();
        for (Map.Entry<ProcessStep, Integer> entry : stepToEnvironment.entrySet()) {
            if (entry.getValue() == environment) {
                steps.add(entry.getKey());
            }
        }
        return Collections.unmodifiableSet(steps);
    }

    boolean canRunConcurrently(ProcessStep step1, ProcessStep step2) {
        Integer env1 = stepToEnvironment.get(step1);
        Integer env2 = stepToEnvironment.get(step2);
        return env1 != null && env2 != null && env1.equals(env2);
    }
}
