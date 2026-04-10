package com.softwarearchetypes.graphs.influence;

import static com.softwarearchetypes.graphs.influence.Fixtures.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import org.junit.jupiter.api.Test;

class InfluenceAnalyzerTest {

    @Test
    void singleDirectConflict() {
        // given
        PhysicsInfluence physics =
                PhysicsInfluence.builder().addInfluence(THERMAL, CONDUCTIVITY).build();

        InfluenceMap influenceMap =
                InfluenceMap.builder()
                        .withPhysics(physics)
                        .withInfrastructure(emptyInfrastructure())
                        .withLaboratories(Set.of(LAB_A, LAB_B))
                        .build();

        Reservation existing = new Reservation(CONDUCTIVITY, LAB_B);
        Reservation newReservation = new Reservation(THERMAL, LAB_A);

        // when
        int conflicts =
                new InfluanceAnalyzer(influenceMap)
                        .countConflicts(newReservation, Set.of(existing));

        // then
        assertEquals(1, conflicts);
    }

    @Test
    void multipleDirectConflicts() {
        // given
        PhysicsInfluence physics =
                PhysicsInfluence.builder()
                        .addInfluence(THERMAL, CONDUCTIVITY)
                        .addInfluence(THERMAL, SPECTROSCOPY)
                        .build();

        InfluenceMap influenceMap =
                InfluenceMap.builder()
                        .withPhysics(physics)
                        .withInfrastructure(emptyInfrastructure())
                        .withLaboratories(Set.of(LAB_A, LAB_B, LAB_C))
                        .build();

        Reservation existing1 = new Reservation(CONDUCTIVITY, LAB_B);
        Reservation existing2 = new Reservation(SPECTROSCOPY, LAB_C);
        Reservation newReservation = new Reservation(THERMAL, LAB_A);

        // when
        int conflicts =
                new InfluanceAnalyzer(influenceMap)
                        .countConflicts(newReservation, Set.of(existing1, existing2));

        // then
        assertEquals(2, conflicts);
    }

    @Test
    void noDirectConflictsWhenNoInfluence() {
        // given
        PhysicsInfluence physics = PhysicsInfluence.builder().build();

        InfluenceMap influenceMap =
                InfluenceMap.builder()
                        .withPhysics(physics)
                        .withInfrastructure(emptyInfrastructure())
                        .withLaboratories(Set.of(LAB_A, LAB_B))
                        .build();

        Reservation existing = new Reservation(SPECTROSCOPY, LAB_B);
        Reservation newReservation = new Reservation(THERMAL, LAB_A);

        // when
        int conflicts =
                new InfluanceAnalyzer(influenceMap)
                        .countConflicts(newReservation, Set.of(existing));

        // then
        assertEquals(0, conflicts);
    }

    @Test
    void newReservationMergesTwoInfluenceZonesIntoOne() {
        // given
        PhysicsProcess processA = new PhysicsProcess("A");
        PhysicsProcess processB = new PhysicsProcess("B");
        PhysicsProcess processX = new PhysicsProcess("X");
        PhysicsProcess processC = new PhysicsProcess("C");
        PhysicsProcess processD = new PhysicsProcess("D");

        PhysicsInfluence physics =
                PhysicsInfluence.builder()
                        .addInfluence(processA, processB)
                        .addInfluence(processC, processD)
                        .addInfluence(processB, processX)
                        .addInfluence(processX, processC)
                        .build();

        LaboratoryAdjacency adjacency =
                LaboratoryAdjacency.builder()
                        .adjacent(LAB_A, LAB_A)
                        .adjacent(LAB_B, LAB_B)
                        .adjacent(LAB_A, LAB_C)
                        .adjacent(LAB_C, LAB_B)
                        .build();

        InfluenceMap influenceMap =
                InfluenceMap.builder()
                        .withPhysics(physics)
                        .withInfrastructure(emptyInfrastructure())
                        .withLaboratoryAdjacency(adjacency)
                        .build();

        Reservation existing1 = new Reservation(processA, LAB_A);
        Reservation existing2 = new Reservation(processB, LAB_A);
        Reservation existing3 = new Reservation(processC, LAB_B);
        Reservation existing4 = new Reservation(processD, LAB_B);
        Reservation newReservation = new Reservation(processX, LAB_C);

        // when
        Set<InfluenceZone> zonesBefore =
                new InfluanceAnalyzer(influenceMap)
                        .analyzeInfluenceZones(Set.of(existing1, existing2, existing3, existing4));
        InfluenceZone zoneAfter =
                new InfluanceAnalyzer(influenceMap)
                        .findInfluenceZone(
                                newReservation,
                                Set.of(existing1, existing2, existing3, existing4, newReservation));

        // then
        assertEquals(2, zonesBefore.size());
        assertEquals(5, zoneAfter.size());
        assertEquals(4, zoneAfter.countReservationsToNegotiateWith(newReservation));
        assertEquals(
                Set.of(existing1, existing2, existing3, existing4),
                zoneAfter.getReservationsToNegotiateWith(newReservation));
    }

    @Test
    void newReservationAddsThirdIndependentInfluenceZone() {
        // given
        PhysicsProcess processA = new PhysicsProcess("A");
        PhysicsProcess processB = new PhysicsProcess("B");
        PhysicsProcess processC = new PhysicsProcess("C");
        PhysicsProcess processD = new PhysicsProcess("D");
        PhysicsProcess processE = new PhysicsProcess("E");

        PhysicsInfluence physics =
                PhysicsInfluence.builder()
                        .addInfluence(processA, processB)
                        .addInfluence(processC, processD)
                        .build();

        InfluenceMap influenceMap =
                InfluenceMap.builder()
                        .withPhysics(physics)
                        .withInfrastructure(emptyInfrastructure())
                        .withLaboratories(Set.of(LAB_A, LAB_B, LAB_C))
                        .build();

        Reservation existing1 = new Reservation(processA, LAB_A);
        Reservation existing2 = new Reservation(processB, LAB_A);
        Reservation existing3 = new Reservation(processC, LAB_B);
        Reservation existing4 = new Reservation(processD, LAB_B);
        Reservation newReservation = new Reservation(processE, LAB_C);

        // when
        Set<InfluenceZone> zonesBefore =
                new InfluanceAnalyzer(influenceMap)
                        .analyzeInfluenceZones(Set.of(existing1, existing2, existing3, existing4));
        Set<InfluenceZone> zonesAfter =
                new InfluanceAnalyzer(influenceMap)
                        .analyzeInfluenceZones(
                                Set.of(existing1, existing2, existing3, existing4, newReservation));
        InfluenceZone independentZone =
                new InfluanceAnalyzer(influenceMap)
                        .findInfluenceZone(
                                newReservation,
                                Set.of(existing1, existing2, existing3, existing4, newReservation));

        // then
        assertEquals(2, zonesBefore.size());
        assertEquals(3, zonesAfter.size());
        assertEquals(0, independentZone.countReservationsToNegotiateWith(newReservation));
        assertEquals(1, independentZone.size());
    }
}
