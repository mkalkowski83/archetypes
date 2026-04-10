package com.softwarearchetypes.graphs.cycles;

import static java.util.stream.Collectors.toSet;

import com.softwarearchetypes.graphs.cycles.math.Edge;
import com.softwarearchetypes.graphs.cycles.math.Graph;
import com.softwarearchetypes.graphs.cycles.math.Node;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class BatchReservationUseCase {

    private final SlotRepository slotRepository;

    BatchReservationUseCase(SlotRepository slotRepository) {
        this.slotRepository = slotRepository;
    }

    BatchReservationResult execute(List<ReservationChangeRequest> requests) {
        Graph<SlotId, ReservationChangeRequest> graph = buildGraph(requests);
        Set<ReservationChangeRequest> dependentRequests = findDependentRequests(graph, requests);
        if (!dependentRequests.isEmpty()) {
            Map<SlotId, Slot> slots = loadAllSlots(dependentRequests);
            // TODO w 1 pętli
            for (ReservationChangeRequest request : dependentRequests) {
                Slot fromSlot = slots.get(request.fromSlot());
                fromSlot.release();
            }
            for (ReservationChangeRequest request : dependentRequests) {
                Slot toSlot = slots.get(request.toSlot());
                toSlot.assignTo(request.userId());
            }
            slotRepository.saveAll(slots.values());
            return BatchReservationResult.success(dependentRequests);
        }
        return BatchReservationResult.none();
    }

    BatchReservationResult execute(
            List<ReservationChangeRequest> requests, Eligibility eligibility) {
        Graph<OwnerId, ReservationChangeRequest> intersection =
                buildOwnerGraph(requests).intersection(eligibility.asGraph());
        Set<ReservationChangeRequest> dependentRequests =
                findDependentRequestsInOwnerGraph(intersection);

        if (!dependentRequests.isEmpty()) {
            Map<SlotId, Slot> slots = loadAllSlots(dependentRequests);
            for (ReservationChangeRequest request : dependentRequests) {
                Slot fromSlot = slots.get(request.fromSlot());
                fromSlot.release();
            }
            for (ReservationChangeRequest request : dependentRequests) {
                Slot toSlot = slots.get(request.toSlot());
                toSlot.assignTo(request.userId());
            }
            slotRepository.saveAll(slots.values());
            return BatchReservationResult.success(dependentRequests);
        }
        return BatchReservationResult.none();
    }

    private Graph<SlotId, ReservationChangeRequest> buildGraph(
            List<ReservationChangeRequest> requests) {
        Graph<SlotId, ReservationChangeRequest> graph = new Graph<>();
        for (ReservationChangeRequest request : requests) {
            Node<SlotId> fromNode = new Node<>(request.fromSlot());
            Node<SlotId> toNode = new Node<>(request.toSlot());
            Edge<SlotId, ReservationChangeRequest> edge = new Edge<>(fromNode, toNode, request);
            graph.addEdge(edge);
        }
        return graph;
    }

    private Map<SlotId, Slot> loadAllSlots(Set<ReservationChangeRequest> dependentRequests) {
        Set<SlotId> allSlotIds =
                dependentRequests.stream()
                        .flatMap(r -> Stream.of(r.fromSlot(), r.toSlot()))
                        .collect(Collectors.toSet());
        return slotRepository.findAll(allSlotIds);
    }

    private Graph<OwnerId, ReservationChangeRequest> buildOwnerGraph(
            List<ReservationChangeRequest> requests) {
        Graph<OwnerId, ReservationChangeRequest> graph = new Graph<>();

        // Ładujemy sloty żeby poznać ich ownerów
        Set<SlotId> allSlotIds =
                requests.stream()
                        .flatMap(r -> Stream.of(r.fromSlot(), r.toSlot()))
                        .collect(Collectors.toSet());
        Map<SlotId, Slot> slots = slotRepository.findAll(allSlotIds);

        for (ReservationChangeRequest request : requests) {
            Slot fromSlot = slots.get(request.fromSlot());
            Slot toSlot = slots.get(request.toSlot());

            if (fromSlot != null && toSlot != null) {
                // Graf na OwnerId: od obecnego właściciela fromSlot do obecnego właściciela toSlot
                Node<OwnerId> fromOwner = new Node<>(fromSlot.getOwner());
                Node<OwnerId> toOwner = new Node<>(toSlot.getOwner());
                Edge<OwnerId, ReservationChangeRequest> edge =
                        new Edge<>(fromOwner, toOwner, request);
                graph.addEdge(edge);
            }
        }

        return graph;
    }

    private Set<ReservationChangeRequest> findDependentRequestsInOwnerGraph(
            Graph<OwnerId, ReservationChangeRequest> graph) {
        return graph.findFirstCycle()
                .map(path -> path.edges().stream().map(Edge::property).collect(toSet()))
                .orElseGet(Collections::emptySet);
    }

    private Set<ReservationChangeRequest> findDependentRequests(
            Graph<SlotId, ReservationChangeRequest> graph,
            List<ReservationChangeRequest> requests) {
        return graph.findFirstCycle()
                .map(path -> path.edges().stream().map(Edge::property).collect(toSet()))
                .orElseGet(Collections::emptySet);
    }
}
