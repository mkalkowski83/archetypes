package com.softwarearchetypes.product;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
                        .withFeature(cityFeature, CITY_WARSAW)
                        .withFeature(participantsLab, 4)
                        .build();

        assertEquals(
                DIFFICULTY_MEDIUM,
                labInstance.product()
                        .metadata()
                        .get(DIFFICULTY_METADATA_KEY)
                        .orElseThrow());
        assertEquals(
                DURATION_60,
                labInstance.product()
                        .metadata()
                        .get(DURATION_METADATA_KEY)
                        .orElseThrow());
    }

    @Test
    void shouldAssignHardDifficultyToAlcatraz() {
        ProductInstance alcatrazInstance =
                new InstanceBuilder(InstanceId.newOne())
                        .withSerial(SerialNumber.of("BOOKING-ALC-001"))
                        .asProductInstance(wiezienieAlcatraz)
                        .withQuantity(Quantity.of(1, Unit.pieces()))
                        .withFeature(cityFeature, CITY_WARSAW)
                        .withFeature(participantsAlcatraz, 5)
                        .build();

        assertEquals(
                DIFFICULTY_HARD,
                alcatrazInstance.product()
                        .metadata()
                        .get(DIFFICULTY_METADATA_KEY)
                        .orElseThrow());
        assertEquals(
                DURATION_75,
                alcatrazInstance.product()
                        .metadata()
                        .get(DURATION_METADATA_KEY)
                        .orElseThrow());
    }

    @Test
    void shouldAssignEasyDifficultyToEgyptianTomb() {
        ProductInstance egyptInstance =
                new InstanceBuilder(InstanceId.newOne())
                        .withSerial(SerialNumber.of("BOOKING-EGY-001"))
                        .asProductInstance(egipskiGrobowiec)
                        .withQuantity(Quantity.of(1, Unit.pieces()))
                        .withFeature(cityFeature, CITY_WARSAW)
                        .withFeature(participantsEgypt, 3)
                        .build();

        assertEquals(
                DIFFICULTY_EASY,
                egyptInstance.product()
                        .metadata()
                        .get(DIFFICULTY_METADATA_KEY)
                        .orElseThrow());
        assertEquals(
                DURATION_45,
                egyptInstance.product()
                        .metadata()
                        .get(DURATION_METADATA_KEY)
                        .orElseThrow());
    }

    @Test
    void shouldAssignExtremeDifficultyToCyberpunk() {
        ProductInstance cyberInstance =
                new InstanceBuilder(InstanceId.newOne())
                        .withSerial(SerialNumber.of("BOOKING-CYB-001"))
                        .asProductInstance(cyberpunk2077)
                        .withQuantity(Quantity.of(1, Unit.pieces()))
                        .withFeature(cityFeature, CITY_WARSAW)
                        .withFeature(participantsCyberpunk, 5)
                        .build();

        assertEquals(
                DIFFICULTY_EXTREME,
                cyberInstance.product()
                        .metadata()
                        .get(DIFFICULTY_METADATA_KEY)
                        .orElseThrow());
        assertEquals(
                DURATION_90,
                cyberInstance.product()
                        .metadata()
                        .get(DURATION_METADATA_KEY)
                        .orElseThrow());
    }
}
