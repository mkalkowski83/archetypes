package com.softwarearchetypes.graphs.cycles.math;

import java.util.*;

// AI-generated -> TODO replace with a library
public class Graph<T, P> {

    private final Map<Node<T>, List<Edge<T, P>>> adjacencyMatrix = new HashMap<>();

    public Graph<T, P> addEdge(Edge<T, P> edge) {
        adjacencyMatrix.computeIfAbsent(edge.from(), k -> new ArrayList<>()).add(edge);
        adjacencyMatrix.putIfAbsent(edge.to(), new ArrayList<>());
        return this;
    }

    public Optional<Path<T, P>> findFirstCycle() {
        Set<Node<T>> visited = new HashSet<>();
        Set<Node<T>> inStack = new HashSet<>();

        for (Node<T> node : adjacencyMatrix.keySet()) {
            if (!visited.contains(node)) {
                Optional<Path<T, P>> cycle =
                        findCycleDFS(node, visited, inStack, new ArrayList<>());
                if (cycle.isPresent()) {
                    return cycle;
                }
            }
        }
        return Optional.empty();
    }

    private Optional<Path<T, P>> findCycleDFS(
            Node<T> current, Set<Node<T>> visited, Set<Node<T>> inStack, List<Edge<T, P>> path) {
        visited.add(current);
        inStack.add(current);

        for (Edge<T, P> edge : adjacencyMatrix.getOrDefault(current, new ArrayList<>())) {
            Node<T> neighbor = edge.to();
            if (inStack.contains(neighbor)) {
                List<Edge<T, P>> cycle = new ArrayList<>();
                boolean foundStart = false;
                for (Edge<T, P> pathEdge : path) {
                    if (pathEdge.from().equals(neighbor) || foundStart) {
                        foundStart = true;
                        cycle.add(pathEdge);
                    }
                }
                cycle.add(edge);
                return Optional.of(new Path<>(cycle));
            }

            if (!visited.contains(neighbor)) {
                path.add(edge);
                Optional<Path<T, P>> cycle = findCycleDFS(neighbor, visited, inStack, path);
                if (cycle.isPresent()) {
                    return cycle;
                }
                path.remove(path.size() - 1);
            }
        }

        inStack.remove(current);
        return Optional.empty();
    }

    public boolean hasEdge(Node<T> from, Node<T> to) {
        List<Edge<T, P>> edges = adjacencyMatrix.get(from);
        if (edges == null) {
            return false;
        }
        return edges.stream().anyMatch(edge -> edge.to().equals(to));
    }

    public <P2> Graph<T, P> intersection(Graph<T, P2> other) {
        Graph<T, P> result = new Graph<>();

        for (Map.Entry<Node<T>, List<Edge<T, P>>> entry : adjacencyMatrix.entrySet()) {
            for (Edge<T, P> edge : entry.getValue()) {
                if (other.hasEdge(edge.from(), edge.to())) {
                    result.addEdge(edge);
                }
            }
        }

        return result;
    }

    public void removeEdge(Edge<T, P> edge) {
        List<Edge<T, P>> edges = adjacencyMatrix.get(edge.from());
        if (edges != null) {
            for (int i = 0; i < edges.size(); i++) {
                Edge<T, P> current = edges.get(i);
                if (current.to().equals(edge.to())) {
                    edges.remove(i);
                }
            }
        }
    }
}
