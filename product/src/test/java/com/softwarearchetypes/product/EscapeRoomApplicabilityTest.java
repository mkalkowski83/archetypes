package com.softwarearchetypes.product;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class EscapeRoomApplicabilityTest extends EscapeRoomBaseTest {

    @Test
    void labShouldBeApplicableForTwoToFiveParticipants() {
        assertTrue(
                labSzalonegoNaukowca.isApplicableFor(
                        ApplicabilityContext.of(Map.of("participants", "2", "city", CITY_WARSAW))));
        assertTrue(
                labSzalonegoNaukowca.isApplicableFor(
                        ApplicabilityContext.of(Map.of("participants", "5", "city", CITY_WARSAW))));
        assertFalse(
                labSzalonegoNaukowca.isApplicableFor(
                        ApplicabilityContext.of(Map.of("participants", "1", "city", CITY_WARSAW))));
        assertFalse(
                labSzalonegoNaukowca.isApplicableFor(
                        ApplicabilityContext.of(Map.of("participants", "6", "city", CITY_WARSAW))));
    }

    @Test
    void alcatrazShouldBeApplicableForThreeToSixParticipants() {
        assertTrue(
                wiezienieAlcatraz.isApplicableFor(
                        ApplicabilityContext.of(Map.of("participants", "3", "city", CITY_WARSAW))));
        assertTrue(
                wiezienieAlcatraz.isApplicableFor(
                        ApplicabilityContext.of(Map.of("participants", "6", "city", CITY_WARSAW))));
        assertFalse(
                wiezienieAlcatraz.isApplicableFor(
                        ApplicabilityContext.of(Map.of("participants", "2", "city", CITY_WARSAW))));
        assertFalse(
                wiezienieAlcatraz.isApplicableFor(
                        ApplicabilityContext.of(Map.of("participants", "7", "city", CITY_WARSAW))));
    }

    @Test
    void egyptShouldBeApplicableForTwoToFourParticipants() {
        assertTrue(
                egipskiGrobowiec.isApplicableFor(
                        ApplicabilityContext.of(Map.of("participants", "2", "city", CITY_WARSAW))));
        assertTrue(
                egipskiGrobowiec.isApplicableFor(
                        ApplicabilityContext.of(Map.of("participants", "4", "city", CITY_WARSAW))));
        assertFalse(
                egipskiGrobowiec.isApplicableFor(
                        ApplicabilityContext.of(Map.of("participants", "1", "city", CITY_WARSAW))));
        assertFalse(
                egipskiGrobowiec.isApplicableFor(
                        ApplicabilityContext.of(Map.of("participants", "5", "city", CITY_WARSAW))));
    }

    @Test
    void cyberpunkShouldBeApplicableForFourToSixParticipants() {
        assertTrue(
                cyberpunk2077.isApplicableFor(
                        ApplicabilityContext.of(
                                Map.of(
                                        "participants",
                                        "4",
                                        "city",
                                        CITY_WARSAW,
                                        "VR",
                                        VR_YES))));
        assertTrue(
                cyberpunk2077.isApplicableFor(
                        ApplicabilityContext.of(
                                Map.of(
                                        "participants",
                                        "6",
                                        "city",
                                        CITY_WARSAW,
                                        "VR",
                                        VR_YES))));
        assertFalse(
                cyberpunk2077.isApplicableFor(
                        ApplicabilityContext.of(
                                Map.of(
                                        "participants",
                                        "3",
                                        "city",
                                        CITY_WARSAW,
                                        "VR",
                                        VR_YES))));
        assertFalse(
                cyberpunk2077.isApplicableFor(
                        ApplicabilityContext.of(
                                Map.of(
                                        "participants",
                                        "7",
                                        "city",
                                        CITY_WARSAW,
                                        "VR",
                                        VR_YES))));
    }

    @Test
    void labShouldBeAvailableInAllThreeCities() {
        assertTrue(
                labSzalonegoNaukowca.isApplicableFor(
                        ApplicabilityContext.of(Map.of("city", CITY_WARSAW, "participants", "3"))));
        assertTrue(
                labSzalonegoNaukowca.isApplicableFor(
                        ApplicabilityContext.of(Map.of("city", CITY_LODZ, "participants", "3"))));
        assertTrue(
                labSzalonegoNaukowca.isApplicableFor(
                        ApplicabilityContext.of(
                                Map.of("city", CITY_WROCLAW, "participants", "3"))));
    }

    @Test
    void cyberpunkShouldBeAvailableOnlyInWarsaw() {
        assertTrue(
                cyberpunk2077.isApplicableFor(
                        ApplicabilityContext.of(
                                Map.of(
                                        "city",
                                        CITY_WARSAW,
                                        "participants",
                                        "5",
                                        "VR",
                                        VR_YES))));
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
        assertFalse(
                cyberpunk2077.isApplicableFor(
                        ApplicabilityContext.of(
                                Map.of(
                                        "city",
                                        CITY_WROCLAW,
                                        "participants",
                                        "5",
                                        "VR",
                                        VR_YES))));
    }

    @Test
    void roomsShouldNotBeAvailableInOtherCities() {
        assertFalse(
                labSzalonegoNaukowca.isApplicableFor(
                        ApplicabilityContext.of(Map.of("city", "Kraków", "participants", "3"))));
        assertFalse(
                cyberpunk2077.isApplicableFor(
                        ApplicabilityContext.of(
                                Map.of("city", "Gdańsk", "participants", "5", "VR", VR_YES))));
    }
}
