package com.softwarearchetypes.graphs.scheduling.concurrency;

import com.softwarearchetypes.graphs.scheduling.ProcessStep;
import java.util.*;
import org.jgrapht.Graph;
import org.jgrapht.alg.color.GreedyColoring;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

class Concurrency {

    private final Graph<ProcessStep, DefaultEdge> conflictGraph;

    private Concurrency(Graph<ProcessStep, DefaultEdge> conflictGraph) {
        this.conflictGraph = conflictGraph;
    }

    static Builder builder() {
        return new Builder();
    }

    static class Builder {
        private final Set<ProcessStep> steps = new HashSet<>();
        private final Graph<ProcessStep, DefaultEdge> graph = new SimpleGraph<>(DefaultEdge.class);

        Builder addStep(ProcessStep step) {
            steps.add(step);
            graph.addVertex(step);
            return this;
        }

        Builder addConflict(ProcessStep step1, ProcessStep step2) {
            steps.add(step1);
            steps.add(step2);
            graph.addVertex(step1);
            graph.addVertex(step2);
            graph.addEdge(step1, step2);
            return this;
        }

        ExecutionEnvironments build() {
            GreedyColoring<ProcessStep, DefaultEdge> coloring = new GreedyColoring<>(graph);
            org.jgrapht.alg.interfaces.VertexColoringAlgorithm.Coloring<ProcessStep> result =
                    coloring.getColoring();

            Map<ProcessStep, Integer> stepToEnvironment = new HashMap<>();
            for (ProcessStep step : graph.vertexSet()) {
                stepToEnvironment.put(step, result.getColors().get(step));
            }

            return new ExecutionEnvironments(stepToEnvironment);
        }
    }
}
