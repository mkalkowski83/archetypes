package com.softwarearchetypes.graphs.cycles;

import com.softwarearchetypes.graphs.cycles.math.Edge;
import com.softwarearchetypes.graphs.cycles.math.Graph;
import com.softwarearchetypes.graphs.cycles.math.Node;

// limitReached - call this class
// departments changed - call this class
class Eligibility {

    private final Graph<OwnerId, Void> graph = new Graph<>();

    void markTransferEligible(OwnerId from, OwnerId to) {
        graph.addEdge(new Edge<>(new Node<>(from), new Node<>(to), null));
    }

    void markTransferIneligible(OwnerId from, OwnerId to) {
        graph.removeEdge(new Edge<>(new Node<>(from), new Node<>(to), null));
    }

    boolean isTransferEligible(OwnerId from, OwnerId to) {
        return graph.hasEdge(new Node<>(from), new Node<>(to));
    }

    Graph<OwnerId, Void> asGraph() {
        return graph;
    }
}
