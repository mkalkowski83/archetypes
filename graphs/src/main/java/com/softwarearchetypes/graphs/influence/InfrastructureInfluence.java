package com.softwarearchetypes.graphs.influence;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

class InfrastructureInfluence {
    private final Graph<InfluenceUnit, DefaultEdge> graph;

    private InfrastructureInfluence(Graph<InfluenceUnit, DefaultEdge> graph) {
        this.graph = graph;
    }

    static Builder builder() {
        return new Builder();
    }

    Graph<InfluenceUnit, DefaultEdge> asGraph() {
        return graph;
    }

    static class Builder {
        private final Graph<InfluenceUnit, DefaultEdge> graph =
                new DefaultDirectedGraph<>(DefaultEdge.class);

        Builder addConstraint(
                PhysicsProcess fromProcess,
                Laboratory fromLab,
                PhysicsProcess toProcess,
                Laboratory toLab) {
            InfluenceUnit from = new InfluenceUnit(fromProcess, fromLab);
            InfluenceUnit to = new InfluenceUnit(toProcess, toLab);
            graph.addVertex(from);
            graph.addVertex(to);
            graph.addEdge(from, to);
            return this;
        }

        InfrastructureInfluence build() {
            return new InfrastructureInfluence(graph);
        }
    }
}
