package com.softwarearchetypes.graphs.scheduling;

import java.util.*;

record Schedule(List<ProcessStep> steps) {
    Schedule {
        steps = Collections.unmodifiableList(new ArrayList<>(steps));
    }

    ProcessStep first() {
        return steps.isEmpty() ? null : steps.getFirst();
    }

    ProcessStep last() {
        return steps.isEmpty() ? null : steps.getLast();
    }

    int size() {
        return steps.size();
    }

    boolean isEmpty() {
        return steps.isEmpty();
    }
}
