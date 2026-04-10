package com.softwarearchetypes.graphs.scheduling;

public record ProcessStep(String name) {
    public ProcessStep {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Process step name cannot be null or blank");
        }
    }
}
