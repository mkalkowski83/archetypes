package com.softwarearchetypes.product;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softwarearchetypes.quantity.Quantity;
import com.softwarearchetypes.quantity.Unit;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EscapeRoomVRTest extends EscapeRoomBaseTest {

    @Test
    void shouldCreateCyberpunkWithVRInWarsaw() {
        assertEquals(VR_YES, cyberpunk2077.metadata().get("VR").orElseThrow());
        assertEquals(
                DIFFICULTY_EXTREME,
                cyberpunk2077.metadata().get(DIFFICULTY_METADATA_KEY).orElseThrow());
        assertEquals(
                DURATION_90,
                cyberpunk2077.metadata().get(DURATION_METADATA_KEY).orElseThrow());
        assertDoesNotThrow(
                () ->
                        new InstanceBuilder(InstanceId.newOne())
                                .withSerial(SerialNumber.of("CP-VR-WAW"))
                                .asProductInstance(cyberpunk2077)
                                .withQuantity(Quantity.of(1, Unit.pieces()))
                                .withFeature(cityFeature, CITY_WARSAW)
                                .withFeature(participantsCyberpunk, 5)
                                .build());
    }

    @Test
    void shouldRejectCyberpunkOutsideWarsaw() {
        assertFalse(
                cyberpunk2077.isApplicableFor(
                        ApplicabilityContext.of(
                                Map.of(
                                        "city",
                                        CITY_LODZ,
                                        "participants",
                                        "5",
                                        "VR",
                                        VR_YES))));
    }

    @Test
    void shouldAllowLabInWarsawWithVrOptionInContext() {
        assertDoesNotThrow(
                () ->
                        new InstanceBuilder(InstanceId.newOne())
                                .withSerial(SerialNumber.of("LAB-VR-WAW"))
                                .asProductInstance(labSzalonegoNaukowca)
                                .withQuantity(Quantity.of(1, Unit.pieces()))
                                .withFeature(cityFeature, CITY_WARSAW)
                                .withFeature(participantsLab, 4)
                                .build());
        assertTrue(
                labSzalonegoNaukowca.isApplicableFor(
                        ApplicabilityContext.of(
                                Map.of(
                                        "city",
                                        CITY_WARSAW,
                                        "participants",
                                        "4",
                                        "VR",
                                        VR_YES))));
    }

    @Test
    void shouldRejectVrInOtherCitiesForLabInApplicabilityContext() {
        assertFalse(
                labSzalonegoNaukowca.isApplicableFor(
                        ApplicabilityContext.of(
                                Map.of(
                                        "city",
                                        CITY_LODZ,
                                        "participants",
                                        "4",
                                        "VR",
                                        VR_YES))));
    }

    @Test
    void shouldAllowLabInOtherCitiesWhenVrNotRequestedInContext() {
        assertDoesNotThrow(
                () ->
                        new InstanceBuilder(InstanceId.newOne())
                                .withSerial(SerialNumber.of("LAB-NOVR-LDZ"))
                                .asProductInstance(labSzalonegoNaukowca)
                                .withQuantity(Quantity.of(1, Unit.pieces()))
                                .withFeature(cityFeature, CITY_LODZ)
                                .withFeature(participantsLab, 4)
                                .build());
        assertTrue(
                labSzalonegoNaukowca.isApplicableFor(
                        ApplicabilityContext.of(Map.of("city", CITY_LODZ, "participants", "4"))));
    }
}
