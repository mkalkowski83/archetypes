package com.softwarearchetypes.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.softwarearchetypes.quantity.Quantity;
import com.softwarearchetypes.quantity.Unit;
import org.junit.jupiter.api.Test;

class EscapeRoomDifficultyTest extends EscapeRoomBaseTest {

    @Test
    void shouldAssignMediumDifficultyToLab() {
        ProductInstance labInstance =
                new InstanceBuilder(InstanceId.newOne())
                        .withSerial(SerialNumber.of("BOOKING-LAB-001"))
                        .asProductInstance(labSzalonegoNaukowca)
                        .withQuantity(Quantity.of(1, Unit.pieces()))
                        .withFeature(difficultyFeature, DIFFICULTY_MEDIUM)
                        .withFeature(durationFeature, DURATION_60)
                        .withFeature(cityFeature, CITY_WARSAW)
                        .withFeature(participantsLab, 4)
                        .build();

        assertEquals(
                DIFFICULTY_MEDIUM,
                labInstance.features().get(difficultyFeature).orElseThrow().value());
        assertEquals(
                DURATION_60, labInstance.features().get(durationFeature).orElseThrow().value());
    }

    @Test
    void shouldAssignHardDifficultyToAlcatraz() {
        ProductInstance alcatrazInstance =
                new InstanceBuilder(InstanceId.newOne())
                        .withSerial(SerialNumber.of("BOOKING-ALC-001"))
                        .asProductInstance(wiezienieAlcatraz)
                        .withQuantity(Quantity.of(1, Unit.pieces()))
                        .withFeature(difficultyFeature, DIFFICULTY_HARD)
                        .withFeature(durationFeature, DURATION_75)
                        .withFeature(cityFeature, CITY_WARSAW)
                        .withFeature(participantsAlcatraz, 5)
                        .build();

        assertEquals(
                DIFFICULTY_HARD,
                alcatrazInstance.features().get(difficultyFeature).orElseThrow().value());
        assertEquals(
                DURATION_75,
                alcatrazInstance.features().get(durationFeature).orElseThrow().value());
    }

    @Test
    void shouldAssignEasyDifficultyToEgyptianTomb() {
        ProductInstance egyptInstance =
                new InstanceBuilder(InstanceId.newOne())
                        .withSerial(SerialNumber.of("BOOKING-EGY-001"))
                        .asProductInstance(egipskiGrobowiec)
                        .withQuantity(Quantity.of(1, Unit.pieces()))
                        .withFeature(difficultyFeature, DIFFICULTY_EASY)
                        .withFeature(durationFeature, DURATION_45)
                        .withFeature(cityFeature, CITY_WARSAW)
                        .withFeature(participantsEgypt, 3)
                        .build();

        assertEquals(
                DIFFICULTY_EASY,
                egyptInstance.features().get(difficultyFeature).orElseThrow().value());
        assertEquals(
                DURATION_45, egyptInstance.features().get(durationFeature).orElseThrow().value());
    }

    @Test
    void shouldAssignExtremeDifficultyToCyberpunk() {
        ProductInstance cyberInstance =
                new InstanceBuilder(InstanceId.newOne())
                        .withSerial(SerialNumber.of("BOOKING-CYB-001"))
                        .asProductInstance(cyberpunk2077)
                        .withQuantity(Quantity.of(1, Unit.pieces()))
                        .withFeature(difficultyFeature, DIFFICULTY_EXTREME)
                        .withFeature(durationFeature, DURATION_90)
                        .withFeature(cityFeature, CITY_WARSAW)
                        .withFeature(participantsCyberpunk, 5)
                        .build();

        assertEquals(
                DIFFICULTY_EXTREME,
                cyberInstance.features().get(difficultyFeature).orElseThrow().value());
        assertEquals(
                DURATION_90, cyberInstance.features().get(durationFeature).orElseThrow().value());
    }

    @Test
    void shouldRejectInvalidDifficultyValue() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new InstanceBuilder(InstanceId.newOne())
                                .withSerial(SerialNumber.of("BOOKING-INVALID"))
                                .asProductInstance(labSzalonegoNaukowca)
                                .withQuantity(Quantity.of(1, Unit.pieces()))
                                .withFeature(difficultyFeature, "niemożliwy")
                                .withFeature(durationFeature, DURATION_60)
                                .withFeature(cityFeature, CITY_WARSAW)
                                .withFeature(participantsLab, 3)
                                .build(),
                "Powinno odrzucić nieznany poziom trudności");
    }
}
