package com.softwarearchetypes.graphs.influence;

import static org.junit.jupiter.api.Assertions.*;

class InfluenceMapAssert {
    private final InfluenceMap actual;

    private InfluenceMapAssert(InfluenceMap actual) {
        this.actual = actual;
    }

    static InfluenceMapAssert assertThat(InfluenceMap actual) {
        return new InfluenceMapAssert(actual);
    }

    InfluenceMapAssert hasEdge(
            PhysicsProcess fromProcess,
            Laboratory fromLab,
            PhysicsProcess toProcess,
            Laboratory toLab) {
        assertTrue(
                actual.asGraph()
                        .containsEdge(
                                new InfluenceUnit(fromProcess, fromLab),
                                new InfluenceUnit(toProcess, toLab)),
                "Expected edge from ("
                        + fromProcess
                        + ", "
                        + fromLab
                        + ") to ("
                        + toProcess
                        + ", "
                        + toLab
                        + ")");
        return this;
    }

    InfluenceMapAssert hasEdgeCount(int expectedCount) {
        assertEquals(
                expectedCount,
                actual.asGraph().edgeSet().size(),
                "Expected "
                        + expectedCount
                        + " edges but found "
                        + actual.asGraph().edgeSet().size());
        return this;
    }
}
