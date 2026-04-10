package com.softwarearchetypes.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.softwarearchetypes.quantity.Quantity;
import com.softwarearchetypes.quantity.Unit;
import org.junit.jupiter.api.Test;

class EscapeRoomCityTest extends EscapeRoomBaseTest {

    @Test
    void shouldAssignWarsawToRoom() {
        ProductInstance labInstance =
                new InstanceBuilder(InstanceId.newOne())
                        .withSerial(SerialNumber.of("BOOKING-LAB-WAW"))
                        .asProductInstance(labSzalonegoNaukowca)
                        .withQuantity(Quantity.of(1, Unit.pieces()))
                        .withFeature(cityFeature, CITY_WARSAW)
                        .withFeature(participantsLab, 4)
                        .build();

        assertEquals(CITY_WARSAW, labInstance.features().get(cityFeature).orElseThrow().value());
    }

    @Test
    void shouldAssignLodzToRoom() {
        ProductInstance alcatrazInstance =
                new InstanceBuilder(InstanceId.newOne())
                        .withSerial(SerialNumber.of("BOOKING-ALC-LDZ"))
                        .asProductInstance(wiezienieAlcatraz)
                        .withQuantity(Quantity.of(1, Unit.pieces()))
                        .withFeature(cityFeature, CITY_LODZ)
                        .withFeature(participantsAlcatraz, 5)
                        .build();

        assertEquals(CITY_LODZ, alcatrazInstance.features().get(cityFeature).orElseThrow().value());
    }

    @Test
    void shouldAssignWroclawToRoom() {
        ProductInstance egyptInstance =
                new InstanceBuilder(InstanceId.newOne())
                        .withSerial(SerialNumber.of("BOOKING-EGY-WRO"))
                        .asProductInstance(egipskiGrobowiec)
                        .withQuantity(Quantity.of(1, Unit.pieces()))
                        .withFeature(cityFeature, CITY_WROCLAW)
                        .withFeature(participantsEgypt, 3)
                        .build();

        assertEquals(CITY_WROCLAW, egyptInstance.features().get(cityFeature).orElseThrow().value());
    }

    @Test
    void shouldRejectInvalidCityValue() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new InstanceBuilder(InstanceId.newOne())
                                .withSerial(SerialNumber.of("BOOKING-KRK"))
                                .asProductInstance(labSzalonegoNaukowca)
                                .withQuantity(Quantity.of(1, Unit.pieces()))
                                .withFeature(cityFeature, "Kraków")
                                .withFeature(participantsLab, 3)
                                .build(),
                "Powinno odrzucić miasto spoza dozwolonej listy");
    }

    @Test
    void shouldRequireCityFeatureOnRoomInstance() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new InstanceBuilder(InstanceId.newOne())
                                .withSerial(SerialNumber.of("BOOKING-NO-CITY"))
                                .asProductInstance(labSzalonegoNaukowca)
                                .withQuantity(Quantity.of(1, Unit.pieces()))
                                .withFeature(participantsLab, 4)
                                .build(),
                "Powinno wymagać ustawienia miasta jako obowiązkowej cechy pokoju");
    }
}
