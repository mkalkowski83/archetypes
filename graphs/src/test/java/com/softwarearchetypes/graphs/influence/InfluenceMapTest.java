package com.softwarearchetypes.graphs.influence;

import static com.softwarearchetypes.graphs.influence.Fixtures.*;
import static com.softwarearchetypes.graphs.influence.InfluenceMapAssert.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

class InfluenceMapTest {

    @Test
    void shouldCreateInfluenceGraphAsCartesianProductOfPhysicsAndLaboratories() {
        // given
        PhysicsInfluence physics =
                PhysicsInfluence.builder().addInfluence(THERMAL, CONDUCTIVITY).build();

        // and
        Set<Laboratory> labs = Set.of(LAB_A, LAB_B, LAB_C);

        // and
        InfrastructureInfluence infrastructureInfluence = emptyInfrastructure();

        // when
        InfluenceMap influence =
                InfluenceMap.builder()
                        .withPhysics(physics)
                        .withInfrastructure(infrastructureInfluence)
                        .withLaboratories(labs)
                        .build();

        // then
        assertThat(influence)
                .hasEdge(THERMAL, LAB_A, CONDUCTIVITY, LAB_A)
                .hasEdge(THERMAL, LAB_A, CONDUCTIVITY, LAB_B)
                .hasEdge(THERMAL, LAB_A, CONDUCTIVITY, LAB_C)
                .hasEdge(THERMAL, LAB_B, CONDUCTIVITY, LAB_A)
                .hasEdge(THERMAL, LAB_B, CONDUCTIVITY, LAB_B)
                .hasEdge(THERMAL, LAB_B, CONDUCTIVITY, LAB_C)
                .hasEdge(THERMAL, LAB_C, CONDUCTIVITY, LAB_A)
                .hasEdge(THERMAL, LAB_C, CONDUCTIVITY, LAB_B)
                .hasEdge(THERMAL, LAB_C, CONDUCTIVITY, LAB_C)
                .hasEdgeCount(9);
    }

    @Test
    void shouldCombinePhysicsCartesianProductWithInfrastructureConstraints() {
        // given
        PhysicsInfluence physics =
                PhysicsInfluence.builder().addInfluence(THERMAL, CONDUCTIVITY).build();

        // and
        Set<Laboratory> labs = Set.of(LAB_A, LAB_B);

        // and
        InfrastructureInfluence infrastructureInfluence =
                InfrastructureInfluence.builder()
                        .addConstraint(SPECTROSCOPY, LAB_A, THERMAL, LAB_B)
                        .build();

        // when
        InfluenceMap influence =
                InfluenceMap.builder()
                        .withPhysics(physics)
                        .withInfrastructure(infrastructureInfluence)
                        .withLaboratories(labs)
                        .build();

        // then
        assertThat(influence)
                .hasEdge(THERMAL, LAB_A, CONDUCTIVITY, LAB_A)
                .hasEdge(THERMAL, LAB_A, CONDUCTIVITY, LAB_B)
                .hasEdge(THERMAL, LAB_B, CONDUCTIVITY, LAB_A)
                .hasEdge(THERMAL, LAB_B, CONDUCTIVITY, LAB_B)
                .hasEdge(SPECTROSCOPY, LAB_A, THERMAL, LAB_B)
                .hasEdgeCount(5);
    }

    @Test
    void shouldCreateInfluenceGraphBasedOnLaboratoryAdjacency() {
        // given
        PhysicsInfluence physics =
                PhysicsInfluence.builder().addInfluence(THERMAL, CONDUCTIVITY).build();

        // and
        LaboratoryAdjacency adjacency =
                LaboratoryAdjacency.builder().adjacent(LAB_A, LAB_B).adjacent(LAB_B, LAB_C).build();

        // and
        InfrastructureInfluence infrastructureInfluence = emptyInfrastructure();

        // when
        InfluenceMap influence =
                InfluenceMap.builder()
                        .withPhysics(physics)
                        .withInfrastructure(infrastructureInfluence)
                        .withLaboratoryAdjacency(adjacency)
                        .build();

        // then
        assertThat(influence)
                .hasEdge(THERMAL, LAB_A, CONDUCTIVITY, LAB_B)
                .hasEdge(THERMAL, LAB_B, CONDUCTIVITY, LAB_C)
                .hasEdgeCount(2);
    }
}
