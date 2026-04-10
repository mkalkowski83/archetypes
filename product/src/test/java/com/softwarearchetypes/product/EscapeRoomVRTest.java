package com.softwarearchetypes.product;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.softwarearchetypes.quantity.Quantity;
import com.softwarearchetypes.quantity.Unit;
import org.junit.jupiter.api.Test;

class EscapeRoomVRTest extends EscapeRoomBaseTest {

    @Test
    void shouldCreateCyberpunkWithVRInWarsaw() {
        assertDoesNotThrow(() ->
                new InstanceBuilder(InstanceId.newOne())
                        .withSerial(SerialNumber.of("CP-VR-WAW"))
                        .asProductInstance(cyberpunk2077)
                        .withQuantity(Quantity.of(1, Unit.pieces()))
                        .withFeature(difficultyFeature, DIFFICULTY_HARD)
                        .withFeature(durationFeature, DURATION_90)
                        .withFeature(cityFeature, CITY_WARSAW)
                        .withFeature(participantsCyberpunk, 5)
                        .withFeature(vrFeature, VR_YES)
                        .build()
        );
    }

    @Test
    void shouldRejectCyberpunkWithoutVR() {
        assertThrows(IllegalArgumentException.class, () ->
                new InstanceBuilder(InstanceId.newOne())
                        .withSerial(SerialNumber.of("CP-NOVR-WAW"))
                        .asProductInstance(cyberpunk2077)
                        .withQuantity(Quantity.of(1, Unit.pieces()))
                        .withFeature(difficultyFeature, DIFFICULTY_HARD)
                        .withFeature(durationFeature, DURATION_90)
                        .withFeature(cityFeature, CITY_WARSAW)
                        .withFeature(participantsCyberpunk, 5)
                        .withFeature(vrFeature, VR_NO)
                        .build(),
                "Cyberpunk powinien wymagać VR=tak"
        );
    }

    @Test
    void shouldRejectCyberpunkOutsideWarsaw() {
        // This is already partially covered by base constraints but good to verify with VR
        assertThrows(IllegalArgumentException.class, () ->
                new InstanceBuilder(InstanceId.newOne())
                        .withSerial(SerialNumber.of("CP-VR-LDZ"))
                        .asProductInstance(cyberpunk2077)
                        .withQuantity(Quantity.of(1, Unit.pieces()))
                        .withFeature(difficultyFeature, DIFFICULTY_HARD)
                        .withFeature(durationFeature, DURATION_90)
                        .withFeature(cityFeature, CITY_LODZ)
                        .withFeature(participantsCyberpunk, 5)
                        .withFeature(vrFeature, VR_YES)
                        .build()
        );
    }

    @Test
    void shouldAllowVRInWarsawForOtherRooms() {
        assertDoesNotThrow(() ->
                new InstanceBuilder(InstanceId.newOne())
                        .withSerial(SerialNumber.of("LAB-VR-WAW"))
                        .asProductInstance(labSzalonegoNaukowca)
                        .withQuantity(Quantity.of(1, Unit.pieces()))
                        .withFeature(difficultyFeature, DIFFICULTY_MEDIUM)
                        .withFeature(durationFeature, DURATION_60)
                        .withFeature(cityFeature, CITY_WARSAW)
                        .withFeature(participantsLab, 4)
                        .withFeature(vrFeature, VR_YES)
                        .build()
        );
    }

    @Test
    void shouldRejectVRInOtherCitiesForOtherRooms() {
        assertThrows(IllegalArgumentException.class, () ->
                new InstanceBuilder(InstanceId.newOne())
                        .withSerial(SerialNumber.of("LAB-VR-LDZ"))
                        .asProductInstance(labSzalonegoNaukowca)
                        .withQuantity(Quantity.of(1, Unit.pieces()))
                        .withFeature(difficultyFeature, DIFFICULTY_MEDIUM)
                        .withFeature(durationFeature, DURATION_60)
                        .withFeature(cityFeature, CITY_LODZ)
                        .withFeature(participantsLab, 4)
                        .withFeature(vrFeature, VR_YES)
                        .build(),
                "VR powinno być dostępne tylko w Warszawie dla innych pokoi"
        );
    }

    @Test
    void shouldAllowNoVRInOtherCitiesForOtherRooms() {
        assertDoesNotThrow(() ->
                new InstanceBuilder(InstanceId.newOne())
                        .withSerial(SerialNumber.of("LAB-NOVR-LDZ"))
                        .asProductInstance(labSzalonegoNaukowca)
                        .withQuantity(Quantity.of(1, Unit.pieces()))
                        .withFeature(difficultyFeature, DIFFICULTY_MEDIUM)
                        .withFeature(durationFeature, DURATION_60)
                        .withFeature(cityFeature, CITY_LODZ)
                        .withFeature(participantsLab, 4)
                        .withFeature(vrFeature, VR_NO)
                        .build()
        );
    }
}
