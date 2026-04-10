package com.softwarearchetypes.graphs.influence;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

class PhysicsInfluence {
    private final Graph<PhysicsProcess, DefaultEdge> graph;
    private final Map<EdgeKey, Map<String, Integer>> edgeFeatureRequirements;

    private PhysicsInfluence(
            Graph<PhysicsProcess, DefaultEdge> graph,
            Map<EdgeKey, Map<String, Integer>> edgeFeatureRequirements) {
        this.graph = graph;
        this.edgeFeatureRequirements = edgeFeatureRequirements;
    }

    static Builder builder() {
        return new Builder();
    }

    Graph<PhysicsProcess, DefaultEdge> asGraph() {
        return graph;
    }

    Map<String, Integer> getFeatureRequirements(PhysicsProcess from, PhysicsProcess to) {
        return edgeFeatureRequirements.getOrDefault(new EdgeKey(from, to), Collections.emptyMap());
    }

    static class Builder {
        private final Graph<PhysicsProcess, DefaultEdge> graph =
                new DefaultDirectedGraph<>(DefaultEdge.class);
        private final Map<EdgeKey, Map<String, Integer>> edgeFeatureRequirements = new HashMap<>();

        Builder addInfluence(PhysicsProcess from, PhysicsProcess to) {
            graph.addVertex(from);
            graph.addVertex(to);
            graph.addEdge(from, to);
            return this;
        }

        Builder addInfluence(
                PhysicsProcess from, PhysicsProcess to, Map<String, Integer> featureRequirements) {
            graph.addVertex(from);
            graph.addVertex(to);
            graph.addEdge(from, to);
            edgeFeatureRequirements.put(new EdgeKey(from, to), new HashMap<>(featureRequirements));
            return this;
        }

        PhysicsInfluence build() {
            return new PhysicsInfluence(graph, edgeFeatureRequirements);
        }
    }

    private record EdgeKey(Object from, Object to) {}
}
