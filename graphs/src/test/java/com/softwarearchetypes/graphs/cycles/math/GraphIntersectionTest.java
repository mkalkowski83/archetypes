package com.softwarearchetypes.graphs.cycles.math;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GraphIntersectionTest {

    @Test
    @DisplayName("przecięcie dwóch grafów zawiera tylko wspólne krawędzie")
    void intersectionContainsOnlyCommonEdges() {
        // given
        Graph<String, String> graph1 = new Graph<>();
        graph1.addEdge(new Edge<>(new Node<>("A"), new Node<>("B"), "edge1"));
        graph1.addEdge(new Edge<>(new Node<>("B"), new Node<>("C"), "edge2"));
        graph1.addEdge(new Edge<>(new Node<>("C"), new Node<>("A"), "edge3"));

        Graph<String, String> graph2 = new Graph<>();
        graph2.addEdge(new Edge<>(new Node<>("A"), new Node<>("B"), "edge1"));
        graph2.addEdge(new Edge<>(new Node<>("B"), new Node<>("C"), "edge2"));

        // when
        Graph<String, String> intersection = graph1.intersection(graph2);

        // then
        assertTrue(intersection.hasEdge(new Node<>("A"), new Node<>("B")));
        assertTrue(intersection.hasEdge(new Node<>("B"), new Node<>("C")));
        assertFalse(intersection.hasEdge(new Node<>("C"), new Node<>("A")));
    }

    @Test
    @DisplayName("przecięcie pozwala znaleźć cykl tylko jeśli wszystkie krawędzie są w obu grafach")
    void intersectionFindsCycleOnlyWhenAllEdgesInBoth() {
        // given
        Graph<String, String> graph1 = new Graph<>();
        graph1.addEdge(new Edge<>(new Node<>("A"), new Node<>("B"), "edge1"));
        graph1.addEdge(new Edge<>(new Node<>("B"), new Node<>("C"), "edge2"));
        graph1.addEdge(new Edge<>(new Node<>("C"), new Node<>("A"), "edge3"));

        Graph<String, String> graph2 = new Graph<>();
        graph2.addEdge(new Edge<>(new Node<>("A"), new Node<>("B"), "edge1"));
        graph2.addEdge(new Edge<>(new Node<>("B"), new Node<>("C"), "edge2"));
        graph2.addEdge(new Edge<>(new Node<>("C"), new Node<>("A"), "edge3"));

        // when
        Graph<String, String> intersection = graph1.intersection(graph2);

        // then
        Optional<Path<String, String>> cycle = intersection.findFirstCycle();
        assertTrue(cycle.isPresent());
        assertEquals(3, cycle.get().edges().size());
    }

    @Test
    @DisplayName("przecięcie nie znajduje cyklu jeśli brakuje krawędzi w drugim grafie")
    void intersectionDoesNotFindCycleWhenEdgeMissingInSecondGraph() {
        // given
        Graph<String, String> graph1 = new Graph<>();
        graph1.addEdge(new Edge<>(new Node<>("A"), new Node<>("B"), "edge1"));
        graph1.addEdge(new Edge<>(new Node<>("B"), new Node<>("C"), "edge2"));
        graph1.addEdge(new Edge<>(new Node<>("C"), new Node<>("A"), "edge3"));

        Graph<String, String> graph2 = new Graph<>();
        graph2.addEdge(new Edge<>(new Node<>("A"), new Node<>("B"), "edge1"));
        graph2.addEdge(new Edge<>(new Node<>("B"), new Node<>("C"), "edge2"));

        // when
        Graph<String, String> intersection = graph1.intersection(graph2);

        // then
        Optional<Path<String, String>> cycle = intersection.findFirstCycle();
        assertFalse(cycle.isPresent());
    }

    @Test
    @DisplayName("przecięcie pustych grafów daje pusty graf")
    void intersectionOfEmptyGraphsIsEmpty() {
        // given
        Graph<String, String> graph1 = new Graph<>();
        Graph<String, String> graph2 = new Graph<>();

        // when
        Graph<String, String> intersection = graph1.intersection(graph2);

        // then
        Optional<Path<String, String>> cycle = intersection.findFirstCycle();
        assertFalse(cycle.isPresent());
    }

    @Test
    @DisplayName("przecięcie z pustym grafem daje pusty graf")
    void intersectionWithEmptyGraphIsEmpty() {
        // given
        Graph<String, String> graph1 = new Graph<>();
        graph1.addEdge(new Edge<>(new Node<>("A"), new Node<>("B"), "edge1"));
        graph1.addEdge(new Edge<>(new Node<>("B"), new Node<>("C"), "edge2"));

        Graph<String, String> graph2 = new Graph<>();

        // when
        Graph<String, String> intersection = graph1.intersection(graph2);

        // then
        assertFalse(intersection.hasEdge(new Node<>("A"), new Node<>("B")));
        assertFalse(intersection.hasEdge(new Node<>("B"), new Node<>("C")));
    }
}
