package com.softwarearchetypes.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softwarearchetypes.quantity.Quantity;
import com.softwarearchetypes.quantity.Unit;
import java.util.List;
import org.junit.jupiter.api.Test;

class EscapeRoomPackageTest extends EscapeRoomBaseTest {

    @Test
    void shouldAllowExperienceWithRoomOnly() {
        List<SelectedProduct> selection =
                List.of(new SelectedProduct(labSzalonegoNaukowca.id(), 1));

        PackageValidationResult result = escapeRoomExperience.validateSelection(selection);
        assertTrue(result.isValid(), "Powinno akceptować rezerwację samego pokoju");
    }

    @Test
    void shouldAllowExperienceWithRoomAndOneAddOn() {
        List<SelectedProduct> selection =
                List.of(
                        new SelectedProduct(cyberpunk2077.id(), 1),
                        new SelectedProduct(actorInRoom.id(), 1));

        PackageValidationResult result = escapeRoomExperience.validateSelection(selection);
        assertTrue(result.isValid(), "Powinno akceptować pokój z jednym dodatkiem");
    }

    @Test
    void shouldAllowExperienceWithRoomAndThreeAddOns() {
        List<SelectedProduct> selection =
                List.of(
                        new SelectedProduct(wiezienieAlcatraz.id(), 1),
                        new SelectedProduct(photoAndVideoPackage.id(), 1),
                        new SelectedProduct(actorInRoom.id(), 1),
                        new SelectedProduct(dedicatedGameMaster.id(), 1));

        PackageValidationResult result = escapeRoomExperience.validateSelection(selection);
        assertTrue(result.isValid(), "Powinno akceptować pokój z trzema dodatkami");
    }

    @Test
    void shouldRejectExperienceWithoutRoom() {
        List<SelectedProduct> selection = List.of(new SelectedProduct(actorInRoom.id(), 1));

        PackageValidationResult result = escapeRoomExperience.validateSelection(selection);
        assertFalse(result.isValid(), "Powinno wymagać wyboru pokoju");
    }

    @Test
    void shouldHaveActorAddOnWithMarkupDescription() {
        assertTrue(
                actorInRoom.description().value().contains("+30% do ceny"),
                "Opis aktora powinien zawierać informację o dopłacie 30%");
    }

    @Test
    void shouldCreateExperienceInstanceWithAddOns() {
        ProductInstance roomInstance =
                new InstanceBuilder(InstanceId.newOne())
                        .withSerial(SerialNumber.of("BOOKING-LAB-2026-04-10-18:00"))
                        .asProductInstance(labSzalonegoNaukowca)
                        .withQuantity(Quantity.of(1, Unit.pieces()))
                        .withFeature(cityFeature, CITY_WARSAW)
                        .withFeature(participantsLab, 4)
                        .build();

        ProductInstance actorInstance =
                new InstanceBuilder(InstanceId.newOne())
                        .asProductInstance(actorInRoom)
                        .withQuantity(Quantity.of(1, Unit.pieces()))
                        .build();

        PackageInstance experienceInstance =
                new InstanceBuilder(InstanceId.newOne())
                        .withSerial(SerialNumber.of("EXP-2026-04-10-001"))
                        .asPackageInstance(escapeRoomExperience)
                        .withSelection(
                                List.of(
                                        new SelectedInstance(roomInstance, 1),
                                        new SelectedInstance(actorInstance, 1)))
                        .build();

        assertNotNull(experienceInstance);
        assertEquals(2, experienceInstance.selection().size());
        assertTrue(experienceInstance.serialNumber().isPresent());
    }

    @Test
    void shouldCreateCyberpunkExperienceInstance() {
        ProductInstance roomInstance =
                new InstanceBuilder(InstanceId.newOne())
                        .withSerial(SerialNumber.of("BOOKING-CYBER-2026-04-10-20:00"))
                        .asProductInstance(cyberpunk2077)
                        .withQuantity(Quantity.of(1, Unit.pieces()))
                        .withFeature(cityFeature, CITY_WARSAW)
                        .withFeature(participantsCyberpunk, 5)
                        .build();

        assertEquals(
                DIFFICULTY_EXTREME,
                roomInstance.product()
                        .metadata()
                        .get(DIFFICULTY_METADATA_KEY)
                        .orElseThrow());
        assertEquals(
                DURATION_90,
                roomInstance.product()
                        .metadata()
                        .get(DURATION_METADATA_KEY)
                        .orElseThrow());

        PackageInstance experienceInstance =
                new InstanceBuilder(InstanceId.newOne())
                        .withSerial(SerialNumber.of("EXP-2026-04-10-002"))
                        .asPackageInstance(escapeRoomExperience)
                        .withSelection(List.of(new SelectedInstance(roomInstance, 1)))
                        .build();

        assertNotNull(experienceInstance);
        assertEquals(1, experienceInstance.selection().size());
    }
}
