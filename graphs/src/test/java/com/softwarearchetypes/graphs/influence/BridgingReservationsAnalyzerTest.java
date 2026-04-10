package com.softwarearchetypes.graphs.influence;

import static com.softwarearchetypes.graphs.influence.Fixtures.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import org.junit.jupiter.api.Test;

class BridgingReservationsAnalyzerTest {

    public static final PhysicsProcess PROCESS_A = new PhysicsProcess("A");
    public static final PhysicsProcess PROCESS_B = new PhysicsProcess("B");
    public static final PhysicsProcess PROCESS_C = new PhysicsProcess("D");
    public static final PhysicsProcess PROCESS_D = new PhysicsProcess("C");

    @Test
    void independentReservationsAreNotCritical() {
        // given
        PhysicsProcess processA = new PhysicsProcess("A");
        PhysicsProcess processB = new PhysicsProcess("B");
        PhysicsProcess processC = new PhysicsProcess("C");

        PhysicsInfluence physics =
                PhysicsInfluence.builder().addInfluence(processA, processB).build();

        InfluenceMap influenceMap =
                InfluenceMap.builder()
                        .withPhysics(physics)
                        .withInfrastructure(emptyInfrastructure())
                        .withLaboratories(Set.of(LAB_A, LAB_B, LAB_C))
                        .build();

        Reservation r1 = new Reservation(processA, LAB_A);
        Reservation r2 = new Reservation(processB, LAB_B);
        Reservation r3 = new Reservation(processC, LAB_C);

        // when
        BridgingReservations bridging =
                new InfluanceAnalyzer(influenceMap)
                        .identifyCriticalReservations(Set.of(r1, r2, r3));

        // then
        assertTrue(bridging.isEmpty());
    }

    @Test
    void reservationConnectingTwoGroupsIsCritical() {
        // given - two triangle groups connected by X
        PhysicsProcess processA = new PhysicsProcess("A");
        PhysicsProcess processB = new PhysicsProcess("B");
        PhysicsProcess processC = new PhysicsProcess("C");
        PhysicsProcess processX = new PhysicsProcess("X");
        PhysicsProcess processD = new PhysicsProcess("D");
        PhysicsProcess processE = new PhysicsProcess("E");
        PhysicsProcess processF = new PhysicsProcess("F");

        PhysicsInfluence physics =
                PhysicsInfluence.builder()
                        // group 1: triangle A-B-C
                        .addInfluence(processA, processB)
                        .addInfluence(processB, processC)
                        .addInfluence(processC, processA)
                        // bridge: A connects to X
                        .addInfluence(processA, processX)
                        .addInfluence(processX, processD)
                        // group 2: triangle D-E-F
                        .addInfluence(processD, processE)
                        .addInfluence(processE, processF)
                        .addInfluence(processF, processD)
                        .build();

        InfluenceMap influenceMap =
                InfluenceMap.builder()
                        .withPhysics(physics)
                        .withInfrastructure(emptyInfrastructure())
                        .withLaboratories(Set.of(LAB_A, LAB_B, LAB_C))
                        .build();

        Reservation r1 = new Reservation(processA, LAB_A);
        Reservation r2 = new Reservation(processB, LAB_A);
        Reservation r3 = new Reservation(processC, LAB_A);
        Reservation r4 = new Reservation(processX, LAB_B);
        Reservation r5 = new Reservation(processD, LAB_C);
        Reservation r6 = new Reservation(processE, LAB_C);
        Reservation r7 = new Reservation(processF, LAB_C);

        // when
        BridgingReservations bridging =
                new InfluanceAnalyzer(influenceMap)
                        .identifyCriticalReservations(Set.of(r1, r2, r3, r4, r5, r6, r7));

        // then
        assertEquals(3, bridging.count());
        assertTrue(bridging.isBridging(r4));
        assertTrue(bridging.isBridging(r1));
        assertTrue(bridging.isBridging(r5));
    }

    @Test
    void fullyConnectedGroupHasNoCriticalReservations() {
        // given
        PhysicsProcess processA = new PhysicsProcess("A");
        PhysicsProcess processB = new PhysicsProcess("B");
        PhysicsProcess processC = new PhysicsProcess("C");

        PhysicsInfluence physics =
                PhysicsInfluence.builder()
                        .addInfluence(processA, processB)
                        .addInfluence(processB, processC)
                        .addInfluence(processC, processA)
                        .build();

        InfluenceMap influenceMap =
                InfluenceMap.builder()
                        .withPhysics(physics)
                        .withInfrastructure(emptyInfrastructure())
                        .withLaboratories(Set.of(LAB_A, LAB_B, LAB_C))
                        .build();

        Reservation r1 = new Reservation(processA, LAB_A);
        Reservation r2 = new Reservation(processB, LAB_B);
        Reservation r3 = new Reservation(processC, LAB_C);

        // when
        BridgingReservations bridging =
                new InfluanceAnalyzer(influenceMap)
                        .identifyCriticalReservations(Set.of(r1, r2, r3));

        // then
        assertTrue(bridging.isEmpty());
        assertEquals(0, bridging.count());
    }

    @Test
    void longChainHasMultipleCriticalReservations() {
        // given

        PhysicsInfluence physics =
                PhysicsInfluence.builder()
                        .addInfluence(PROCESS_A, PROCESS_B)
                        .addInfluence(PROCESS_B, PROCESS_C)
                        .addInfluence(PROCESS_C, PROCESS_D)
                        .build();

        InfluenceMap influenceMap =
                InfluenceMap.builder()
                        .withPhysics(physics)
                        .withLaboratories(Set.of(LAB_A, LAB_B, LAB_C))
                        .build();

        Reservation r1 = new Reservation(PROCESS_A, LAB_A);
        Reservation r2 = new Reservation(PROCESS_B, LAB_B);
        Reservation r3 = new Reservation(PROCESS_C, LAB_C);
        Reservation r4 = new Reservation(PROCESS_D, LAB_A);

        // when
        BridgingReservations bridging =
                new InfluanceAnalyzer(influenceMap)
                        .identifyCriticalReservations(Set.of(r1, r2, r3, r4));

        // then
        assertEquals(2, bridging.count());
        assertTrue(bridging.isBridging(r2));
        assertTrue(bridging.isBridging(r3));
    }

    @Test
    void emptySetHasNoCriticalReservations() {
        // given
        PhysicsInfluence physics = PhysicsInfluence.builder().build();

        InfluenceMap influenceMap =
                InfluenceMap.builder()
                        .withPhysics(physics)
                        .withInfrastructure(emptyInfrastructure())
                        .withLaboratories(Set.of(LAB_A))
                        .build();

        // when
        BridgingReservations bridging =
                new InfluanceAnalyzer(influenceMap).identifyCriticalReservations(Set.of());

        // then
        assertTrue(bridging.isEmpty());
        assertEquals(0, bridging.count());
    }
}
