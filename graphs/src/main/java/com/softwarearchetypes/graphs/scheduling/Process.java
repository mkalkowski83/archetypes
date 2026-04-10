package com.softwarearchetypes.graphs.scheduling;

import java.util.*;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.jgrapht.traverse.TopologicalOrderIterator;

record Process(
        Set<ProcessStep> steps,
        Graph<ProcessStep, DefaultEdge> dependencyGraph,
        Map<EdgeKey, DependencyType> edgeDependencyTypes) {

    static Builder builder() {
        return new Builder();
    }

    static class Builder {
        private final Set<ProcessStep> steps = new HashSet<>();
        private final DirectedAcyclicGraph<ProcessStep, DefaultEdge> graph =
                new DirectedAcyclicGraph<>(DefaultEdge.class);
        private final Map<EdgeKey, DependencyType> edgeDependencyTypes = new HashMap<>();

        Builder addStep(ProcessStep step) {
            steps.add(step);
            graph.addVertex(step);
            return this;
        }

        Builder addDependency(ProcessStep from, ProcessStep to) {
            steps.add(from);
            steps.add(to);
            graph.addVertex(from);
            graph.addVertex(to);
            graph.addEdge(from, to);
            return this;
        }

        Builder addDependency(ProcessStep from, ProcessStep to, DependencyType dependencyType) {
            steps.add(from);
            steps.add(to);
            graph.addVertex(from);
            graph.addVertex(to);
            graph.addEdge(from, to);
            edgeDependencyTypes.put(new EdgeKey(from, to), dependencyType);
            return this;
        }

        Schedule build() {
            Process process = new Process(steps, graph, edgeDependencyTypes);
            return calculateSchedule(process);
        }

        private Schedule calculateSchedule(Process process) {
            List<ProcessStep> steps = new ArrayList<>();
            TopologicalOrderIterator<ProcessStep, DefaultEdge> iterator =
                    new TopologicalOrderIterator<>(process.dependencyGraph);

            while (iterator.hasNext()) {
                steps.add(iterator.next());
            }

            return new Schedule(steps);
        }
    }

    private record EdgeKey(ProcessStep from, ProcessStep to) {}
}
