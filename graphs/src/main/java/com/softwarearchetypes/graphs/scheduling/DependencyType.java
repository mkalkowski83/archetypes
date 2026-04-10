package com.softwarearchetypes.graphs.scheduling;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

record DependencyType(String name, Map<String, Object> features) {
    DependencyType {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Dependency type name cannot be null or blank");
        }
        features = Collections.unmodifiableMap(new HashMap<>(features));
    }

    DependencyType(String name) {
        this(name, Map.of());
    }

    static DependencyType finishToStart(String description) {
        return new DependencyType("FINISH_TO_START", Map.of("description", description));
    }

    static DependencyType requiredResource(String resourceName) {
        return new DependencyType("REQUIRED_RESOURCE", Map.of("resource", resourceName));
    }

    static DependencyType dataFlow(String dataType) {
        return new DependencyType("DATA_FLOW", Map.of("dataType", dataType));
    }

    static DependencyType custom(String name, Map<String, Object> features) {
        return new DependencyType(name, features);
    }
}
