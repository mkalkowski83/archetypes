package com.softwarearchetypes.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.softwarearchetypes.quantity.Quantity;
import com.softwarearchetypes.quantity.Unit;
import org.junit.jupiter.api.Test;

class EscapeRoomParticipantsTest extends EscapeRoomBaseTest {

    @Test
    void shouldAcceptMinParticipantsForLab() {
        ProductInstance labInstance =
                new InstanceBuilder(InstanceId.newOne())
                        .withSerial(SerialNumber.of("BOOKING-LAB-MIN"))
                        .asProductInstance(labSzalonegoNaukowca)
                        .withQuantity(Quantity.of(1, Unit.pieces()))
                        .withFeature(cityFeature, CITY_WARSAW)
                        .withFeature(participantsLab, 2)
                        .build();

        assertEquals(2, labInstance.features().get(participantsLab).orElseThrow().value());
    }

    @Test
    void shouldAcceptMaxParticipantsForLab() {
        ProductInstance labInstance =
                new InstanceBuilder(InstanceId.newOne())
                        .withSerial(SerialNumber.of("BOOKING-LAB-MAX"))
                        .asProductInstance(labSzalonegoNaukowca)
                        .withQuantity(Quantity.of(1, Unit.pieces()))
                        .withFeature(cityFeature, CITY_WARSAW)
                        .withFeature(participantsLab, 5)
                        .build();

        assertEquals(5, labInstance.features().get(participantsLab).orElseThrow().value());
    }

    @Test
    void shouldRejectTooManyParticipantsForLab() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new InstanceBuilder(InstanceId.newOne())
                                .withSerial(SerialNumber.of("BOOKING-LAB-OVER"))
                                .asProductInstance(labSzalonegoNaukowca)
                                .withQuantity(Quantity.of(1, Unit.pieces()))
                                .withFeature(participantsLab, 6)
                                .build(),
                "Laboratorium szalonego naukowca nie powinno akceptować więcej niż 5 uczestników");
    }

    @Test
    void shouldRejectTooFewParticipantsForLab() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new InstanceBuilder(InstanceId.newOne())
                                .withSerial(SerialNumber.of("BOOKING-LAB-UNDER"))
                                .asProductInstance(labSzalonegoNaukowca)
                                .withQuantity(Quantity.of(1, Unit.pieces()))
                                .withFeature(participantsLab, 1)
                                .build(),
                "Laboratorium szalonego naukowca wymaga minimum 2 uczestników");
    }

    @Test
    void shouldAcceptMinParticipantsForAlcatraz() {
        ProductInstance alcatrazInstance =
                new InstanceBuilder(InstanceId.newOne())
                        .withSerial(SerialNumber.of("BOOKING-ALC-MIN"))
                        .asProductInstance(wiezienieAlcatraz)
                        .withQuantity(Quantity.of(1, Unit.pieces()))
                        .withFeature(cityFeature, CITY_WARSAW)
                        .withFeature(participantsAlcatraz, 3)
                        .build();

        assertEquals(
                3, alcatrazInstance.features().get(participantsAlcatraz).orElseThrow().value());
    }

    @Test
    void shouldRejectTooFewParticipantsForAlcatraz() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new InstanceBuilder(InstanceId.newOne())
                                .withSerial(SerialNumber.of("BOOKING-ALC-UNDER"))
                                .asProductInstance(wiezienieAlcatraz)
                                .withQuantity(Quantity.of(1, Unit.pieces()))
                                .withFeature(participantsAlcatraz, 2)
                                .build(),
                "Więzienie Alcatraz wymaga minimum 3 uczestników");
    }

    @Test
    void shouldRejectTooManyParticipantsForAlcatraz() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new InstanceBuilder(InstanceId.newOne())
                                .withSerial(SerialNumber.of("BOOKING-ALC-OVER"))
                                .asProductInstance(wiezienieAlcatraz)
                                .withQuantity(Quantity.of(1, Unit.pieces()))
                                .withFeature(participantsAlcatraz, 7)
                                .build(),
                "Więzienie Alcatraz nie powinno akceptować więcej niż 6 uczestników");
    }

    @Test
    void shouldAcceptMaxParticipantsForEgypt() {
        ProductInstance egyptInstance =
                new InstanceBuilder(InstanceId.newOne())
                        .withSerial(SerialNumber.of("BOOKING-EGY-MAX"))
                        .asProductInstance(egipskiGrobowiec)
                        .withQuantity(Quantity.of(1, Unit.pieces()))
                        .withFeature(cityFeature, CITY_WARSAW)
                        .withFeature(participantsEgypt, 4)
                        .build();

        assertEquals(4, egyptInstance.features().get(participantsEgypt).orElseThrow().value());
    }

    @Test
    void shouldRejectTooManyParticipantsForEgypt() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new InstanceBuilder(InstanceId.newOne())
                                .withSerial(SerialNumber.of("BOOKING-EGY-OVER"))
                                .asProductInstance(egipskiGrobowiec)
                                .withQuantity(Quantity.of(1, Unit.pieces()))
                                .withFeature(participantsEgypt, 5)
                                .build(),
                "Egipski grobowiec nie powinien akceptować więcej niż 4 uczestników");
    }

    @Test
    void shouldAcceptMinParticipantsForCyberpunk() {
        ProductInstance cyberInstance =
                new InstanceBuilder(InstanceId.newOne())
                        .withSerial(SerialNumber.of("BOOKING-CYB-MIN"))
                        .asProductInstance(cyberpunk2077)
                        .withQuantity(Quantity.of(1, Unit.pieces()))
                        .withFeature(cityFeature, CITY_WARSAW)
                        .withFeature(participantsCyberpunk, 4)
                        .build();

        assertEquals(4, cyberInstance.features().get(participantsCyberpunk).orElseThrow().value());
    }

    @Test
    void shouldRejectTooFewParticipantsForCyberpunk() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new InstanceBuilder(InstanceId.newOne())
                                .withSerial(SerialNumber.of("BOOKING-CYB-UNDER"))
                                .asProductInstance(cyberpunk2077)
                                .withQuantity(Quantity.of(1, Unit.pieces()))
                                .withFeature(participantsCyberpunk, 3)
                                .build(),
                "Cyberpunk 2077 wymaga minimum 4 uczestników");
    }

    @Test
    void shouldRequireParticipantCountFeatureOnRoomInstance() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new InstanceBuilder(InstanceId.newOne())
                                .withSerial(SerialNumber.of("BOOKING-NO-PART"))
                                .asProductInstance(labSzalonegoNaukowca)
                                .withQuantity(Quantity.of(1, Unit.pieces()))
                                .withFeature(cityFeature, CITY_WARSAW)
                                .build(),
                "Powinno wymagać podania liczby uczestników");
    }

}
