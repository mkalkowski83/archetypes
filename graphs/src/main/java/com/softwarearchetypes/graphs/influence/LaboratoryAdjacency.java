package com.softwarearchetypes.graphs.influence;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

class LaboratoryAdjacency {
    private final Graph<Laboratory, DefaultEdge> graph;

    private LaboratoryAdjacency(Graph<Laboratory, DefaultEdge> graph) {
        this.graph = graph;
    }

    static Builder builder() {
        return new Builder();
    }

    Graph<Laboratory, DefaultEdge> asGraph() {
        return graph;
    }

    static class Builder {
        private final Graph<Laboratory, DefaultEdge> graph =
                new DefaultDirectedGraph<>(DefaultEdge.class);

        Builder adjacent(Laboratory from, Laboratory to) {
            graph.addVertex(from);
            graph.addVertex(to);
            graph.addEdge(from, to);
            return this;
        }

        LaboratoryAdjacency build() {
            return new LaboratoryAdjacency(graph);
        }
    }
}
