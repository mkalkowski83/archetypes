package com.softwarearchetypes.graphs.influence;

import java.util.Set;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

class InfluenceMap {
    private final Graph<InfluenceUnit, DefaultEdge> graph;

    private InfluenceMap(Graph<InfluenceUnit, DefaultEdge> graph) {
        this.graph = graph;
    }

    static InfluenceMap of(
            PhysicsInfluence physicsInfluence,
            InfrastructureInfluence infrastructureInfluence,
            Set<Laboratory> laboratories) {
        Graph<InfluenceUnit, DefaultEdge> result = cartesianOf(physicsInfluence, laboratories);
        addInfrastructure(infrastructureInfluence, result);
        return new InfluenceMap(result);
    }

    static InfluenceMap of(
            PhysicsInfluence physicsInfluence,
            InfrastructureInfluence infrastructureInfluence,
            LaboratoryAdjacency laboratoryAdjacency) {
        Graph<InfluenceUnit, DefaultEdge> result =
                adjacencyOf(physicsInfluence, laboratoryAdjacency);
        addInfrastructure(infrastructureInfluence, result);
        return new InfluenceMap(result);
    }

    private static void addInfrastructure(
            InfrastructureInfluence infrastructureInfluence,
            Graph<InfluenceUnit, DefaultEdge> result) {
        Graph<InfluenceUnit, DefaultEdge> infra = infrastructureInfluence.asGraph();
        for (DefaultEdge edge : infra.edgeSet()) {
            InfluenceUnit source = infra.getEdgeSource(edge);
            InfluenceUnit target = infra.getEdgeTarget(edge);
            result.addVertex(source);
            result.addVertex(target);
            result.addEdge(source, target);
        }
    }

    private static Graph<InfluenceUnit, DefaultEdge> cartesianOf(
            PhysicsInfluence physicsInfluence, Set<Laboratory> laboratories) {
        Graph<InfluenceUnit, DefaultEdge> result = new DefaultDirectedGraph<>(DefaultEdge.class);
        Graph<PhysicsProcess, DefaultEdge> physics = physicsInfluence.asGraph();
        for (DefaultEdge edge : physics.edgeSet()) {
            PhysicsProcess sourceProcess = physics.getEdgeSource(edge);
            PhysicsProcess targetProcess = physics.getEdgeTarget(edge);
            for (Laboratory lab1 : laboratories) {
                for (Laboratory lab2 : laboratories) {
                    InfluenceUnit from = new InfluenceUnit(sourceProcess, lab1);
                    InfluenceUnit to = new InfluenceUnit(targetProcess, lab2);
                    result.addVertex(from);
                    result.addVertex(to);
                    result.addEdge(from, to);
                }
            }
        }
        return result;
    }

    private static Graph<InfluenceUnit, DefaultEdge> adjacencyOf(
            PhysicsInfluence physicsInfluence, LaboratoryAdjacency laboratoryAdjacency) {
        Graph<InfluenceUnit, DefaultEdge> result = new DefaultDirectedGraph<>(DefaultEdge.class);
        Graph<PhysicsProcess, DefaultEdge> physics = physicsInfluence.asGraph();
        Graph<Laboratory, DefaultEdge> adjacency = laboratoryAdjacency.asGraph();

        for (DefaultEdge physicsEdge : physics.edgeSet()) {
            PhysicsProcess sourceProcess = physics.getEdgeSource(physicsEdge);
            PhysicsProcess targetProcess = physics.getEdgeTarget(physicsEdge);

            for (DefaultEdge adjacencyEdge : adjacency.edgeSet()) {
                Laboratory lab1 = adjacency.getEdgeSource(adjacencyEdge);
                Laboratory lab2 = adjacency.getEdgeTarget(adjacencyEdge);

                InfluenceUnit from = new InfluenceUnit(sourceProcess, lab1);
                InfluenceUnit to = new InfluenceUnit(targetProcess, lab2);
                result.addVertex(from);
                result.addVertex(to);
                result.addEdge(from, to);
            }
        }
        return result;
    }

    Graph<InfluenceUnit, DefaultEdge> asGraph() {
        return graph;
    }

    boolean influences(
            PhysicsProcess fromProcess,
            Laboratory fromLab,
            PhysicsProcess toProcess,
            Laboratory toLab) {
        return graph.containsEdge(
                new InfluenceUnit(fromProcess, fromLab), new InfluenceUnit(toProcess, toLab));
    }

    boolean influences(Reservation from, Reservation to) {
        return influences(from.process(), from.laboratory(), to.process(), to.laboratory());
    }

    static Builder builder() {
        return new Builder();
    }

    static class Builder {
        private PhysicsInfluence physicsInfluence;
        private InfrastructureInfluence infrastructureInfluence =
                InfrastructureInfluence.builder().build();
        private Set<Laboratory> laboratories;
        private LaboratoryAdjacency laboratoryAdjacency;

        Builder withPhysics(PhysicsInfluence physicsInfluence) {
            this.physicsInfluence = physicsInfluence;
            return this;
        }

        Builder withInfrastructure(InfrastructureInfluence infrastructureInfluence) {
            this.infrastructureInfluence = infrastructureInfluence;
            return this;
        }

        Builder withLaboratories(Set<Laboratory> laboratories) {
            this.laboratories = laboratories;
            return this;
        }

        Builder withLaboratoryAdjacency(LaboratoryAdjacency laboratoryAdjacency) {
            this.laboratoryAdjacency = laboratoryAdjacency;
            return this;
        }

        InfluenceMap build() {
            if (laboratoryAdjacency != null) {
                return of(physicsInfluence, infrastructureInfluence, laboratoryAdjacency);
            }
            return of(physicsInfluence, infrastructureInfluence, laboratories);
        }
    }
}
