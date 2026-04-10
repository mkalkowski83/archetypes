package com.softwarearchetypes.graphs.influence;

import java.util.Set;
import java.util.stream.Collectors;
import org.jgrapht.Graph;
import org.jgrapht.alg.connectivity.BiconnectivityInspector;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

class InfluanceAnalyzer {

    private final InfluenceMap influenceMap;

    InfluanceAnalyzer(InfluenceMap influenceMap) {
        this.influenceMap = influenceMap;
    }

    int countConflicts(Reservation newReservation, Set<Reservation> existingReservations) {
        int conflicts = 0;
        for (Reservation existing : existingReservations) {
            if (influenceMap.influences(newReservation, existing)) {
                conflicts++;
            }
        }
        return conflicts;
    }

    Set<InfluenceZone> analyzeInfluenceZones(Set<Reservation> reservations) {
        Graph<Reservation, DefaultEdge> graph = buildInfluenceGraph(reservations);
        ConnectivityInspector<Reservation, DefaultEdge> inspector =
                new ConnectivityInspector<>(graph);

        return inspector.connectedSets().stream()
                .map(InfluenceZone::new)
                .collect(Collectors.toSet());
    }

    InfluenceZone findInfluenceZone(Reservation reservation, Set<Reservation> allReservations) {
        Graph<Reservation, DefaultEdge> graph = buildInfluenceGraph(allReservations);
        ConnectivityInspector<Reservation, DefaultEdge> inspector =
                new ConnectivityInspector<>(graph);
        Set<Reservation> connectedComponent = inspector.connectedSetOf(reservation);
        return new InfluenceZone(connectedComponent);
    }

    BridgingReservations identifyCriticalReservations(Set<Reservation> reservations) {
        Graph<Reservation, DefaultEdge> graph = buildInfluenceGraph(reservations);
        BiconnectivityInspector<Reservation, DefaultEdge> inspector =
                new BiconnectivityInspector<>(graph);
        Set<Reservation> criticalReservations = inspector.getCutpoints();
        return new BridgingReservations(criticalReservations);
    }

    private Graph<Reservation, DefaultEdge> buildInfluenceGraph(Set<Reservation> reservations) {
        Graph<Reservation, DefaultEdge> graph = new SimpleGraph<>(DefaultEdge.class);
        for (Reservation reservation : reservations) {
            graph.addVertex(reservation);
        }
        for (Reservation r1 : reservations) {
            for (Reservation r2 : reservations) {
                if (!r1.equals(r2) && influenceMap.influences(r1, r2)) {
                    graph.addEdge(r1, r2);
                }
            }
        }
        return graph;
    }
}
