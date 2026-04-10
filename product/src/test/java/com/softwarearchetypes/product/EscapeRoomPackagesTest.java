package com.softwarearchetypes.product;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class EscapeRoomPackagesTest extends EscapeRoomBaseTest {

    @Test
    void shouldAllowBirthdayPackage() {
        List<SelectedProduct> selection =
            List.of(
                new SelectedProduct(labSzalonegoNaukowca.id(), 1),
                new SelectedProduct(catering.id(), 1),
                new SelectedProduct(photoAndVideoPackage.id(), 1)
            );

        PackageValidationResult result = birthdayPackage.validateSelection(selection);
        assertTrue(result.isValid(), "Powinno akceptować poprawny pakiet urodzinowy");
    }

    @Test
    void shouldRejectBirthdayPackageWithoutPhoto() {
        List<SelectedProduct> selection =
                List.of(
                        new SelectedProduct(labSzalonegoNaukowca.id(), 1),
                        new SelectedProduct(catering.id(), 1));

        PackageValidationResult result = birthdayPackage.validateSelection(selection);
        assertFalse(result.isValid(), "Powinno wymagać pakietu zdjęć w pakiecie urodzinowym");
    }

    @Test
    void shouldAllowHardcorePackage() {
        List<SelectedProduct> selection =
                List.of(
                        new SelectedProduct(cyberpunk2077.id(), 1),
                        new SelectedProduct(actorInRoom.id(), 1),
                        new SelectedProduct(dedicatedGameMaster.id(), 1));

        PackageValidationResult result = hardcorePackage.validateSelection(selection);
        assertTrue(result.isValid(), "Powinno akceptować poprawny pakiet Hardcore");
    }

    @Test
    void shouldRejectHardcorePackageWithoutActor() {
        List<SelectedProduct> selection =
                List.of(
                        new SelectedProduct(cyberpunk2077.id(), 1),
                        new SelectedProduct(dedicatedGameMaster.id(), 1));

        PackageValidationResult result = hardcorePackage.validateSelection(selection);
        assertFalse(result.isValid(), "Powinno wymagać aktora w pakiecie Hardcore");
    }

    @Test
    void shouldAllowCorrectTeamBuildingPath() {
        // Egypt (Easy) -> Lab (Medium)
        List<SelectedProduct> selection =
                List.of(
                        new SelectedProduct(egipskiGrobowiec.id(), 1),
                        new SelectedProduct(labSzalonegoNaukowca.id(), 1),
                        new SelectedProduct(catering.id(), 1),
                        new SelectedProduct(dedicatedGameMaster.id(), 1));

        PackageValidationResult result = teamBuildingPackage.validateSelection(selection);
        assertTrue(result.isValid(), "Powinno akceptować poprawną ścieżkę Team Building (Egipt -> Lab)");
    }

    @Test
    void shouldAllowAnotherCorrectTeamBuildingPath() {
        // Lab (Medium) -> Alcatraz (Hard)
        List<SelectedProduct> selection =
            List.of(
                new SelectedProduct(labSzalonegoNaukowca.id(), 1),
                new SelectedProduct(wiezienieAlcatraz.id(), 1),
                new SelectedProduct(catering.id(), 1),
                new SelectedProduct(dedicatedGameMaster.id(), 1)
            );

        PackageValidationResult result = teamBuildingPackage.validateSelection(selection);
        assertTrue(result.isValid(), "Powinno akceptować poprawną ścieżkę Team Building (Lab -> Alcatraz)");
    }

    @Test
    void shouldRejectIncorrectTeamBuildingPath() {
        // Egypt (Easy) -> Alcatraz (Hard) - pominęliśmy Lab, nie ma takiej relacji w regułach
        List<SelectedProduct> selection =
                List.of(
                        new SelectedProduct(egipskiGrobowiec.id(), 1),
                        new SelectedProduct(wiezienieAlcatraz.id(), 1),
                        new SelectedProduct(catering.id(), 1),
                        new SelectedProduct(dedicatedGameMaster.id(), 1));

        PackageValidationResult result = teamBuildingPackage.validateSelection(selection);
        assertFalse(result.isValid(), "Powinno odrzucić niepoprawną ścieżkę Team Building (Egipt -> Alcatraz)");
    }

    @Test
    void shouldRejectReverseTeamBuildingPath() {
        // Lab (Medium) -> Egypt (Easy) - błąd progresji trudności
        List<SelectedProduct> selection =
                List.of(
                        new SelectedProduct(labSzalonegoNaukowca.id(), 1),
                        new SelectedProduct(egipskiGrobowiec.id(), 1),
                        new SelectedProduct(catering.id(), 1),
                        new SelectedProduct(dedicatedGameMaster.id(), 1));

        PackageValidationResult result = teamBuildingPackage.validateSelection(selection);
        assertFalse(result.isValid(), "Powinno odrzucić odwrotną ścieżkę Team Building (Lab -> Egipt)");
    }
}
