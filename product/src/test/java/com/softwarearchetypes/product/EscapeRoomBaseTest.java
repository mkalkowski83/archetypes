package com.softwarearchetypes.product;

import static com.softwarearchetypes.product.ApplicabilityConstraint.and;
import static com.softwarearchetypes.product.ApplicabilityConstraint.between;
import static com.softwarearchetypes.product.ApplicabilityConstraint.equalsTo;
import static com.softwarearchetypes.product.ApplicabilityConstraint.in;
import static com.softwarearchetypes.product.ApplicabilityConstraint.not;
import static com.softwarearchetypes.product.ApplicabilityConstraint.or;
import static com.softwarearchetypes.product.ProductTrackingStrategy.INDIVIDUALLY_TRACKED;

import com.softwarearchetypes.quantity.Unit;
import org.junit.jupiter.api.BeforeEach;

public abstract class EscapeRoomBaseTest {

    static final String DIFFICULTY_EASY = "łatwy";
    static final String DIFFICULTY_MEDIUM = "średni";
    static final String DIFFICULTY_HARD = "trudny";
    static final String DIFFICULTY_EXTREME = "ekstremalny";

    static final String DURATION_45 = "45 min";
    static final String DURATION_60 = "60 min";
    static final String DURATION_75 = "75 min";
    static final String DURATION_90 = "90 min";

    static final String CATERING_PIZZA = "pizza";
    static final String CATERING_SUSHI = "sushi";
    static final String CATERING_VEGETARIAN = "wegetariański";

    static final String CITY_WARSAW = "Warszawa";
    static final String CITY_LODZ = "Łódź";
    static final String CITY_WROCLAW = "Wrocław";
    static final String VR_YES = "tak";
    static final String VR_NO = "nie";

    protected ProductFeatureType difficultyFeature;
    protected ProductFeatureType durationFeature;
    protected ProductFeatureType cityFeature;
    protected ProductFeatureType vrFeature;
    protected ProductFeatureType cateringVariantFeature;

    // Participant count features (each room has its own range)
    protected ProductFeatureType participantsLab;
    protected ProductFeatureType participantsAlcatraz;
    protected ProductFeatureType participantsEgypt;
    protected ProductFeatureType participantsCyberpunk;

    // Rooms
    protected ProductType labSzalonegoNaukowca;
    protected ProductType wiezienieAlcatraz;
    protected ProductType egipskiGrobowiec;
    protected ProductType cyberpunk2077;

    // Add-ons
    protected ProductType actorInRoom;
    protected ProductType dedicatedGameMaster;

    // Catering
    protected ProductType catering;

    // Souvenirs
    protected ProductType photoAndVideoPackage;

    // Packages
    protected PackageType escapeRoomExperience;
    protected PackageType partyPackage;
    protected PackageType birthdayPackage;
    protected PackageType teamBuildingPackage;
    protected PackageType hardcorePackage;

    @BeforeEach
    void setUp() {
        difficultyFeature =
                ProductFeatureType.withAllowedValues(
                        "poziom trudności",
                        DIFFICULTY_EASY,
                        DIFFICULTY_MEDIUM,
                        DIFFICULTY_HARD,
                        DIFFICULTY_EXTREME);

        durationFeature =
                ProductFeatureType.withAllowedValues(
                        "czas trwania", DURATION_45, DURATION_60, DURATION_75, DURATION_90);

        cityFeature =
                ProductFeatureType.withAllowedValues(
                        "miasto", CITY_WARSAW, CITY_LODZ, CITY_WROCLAW);

        vrFeature = ProductFeatureType.withAllowedValues("VR", VR_YES, VR_NO);

        participantsLab = ProductFeatureType.withNumericRange("liczba uczestników", 2, 5);
        participantsAlcatraz = ProductFeatureType.withNumericRange("liczba uczestników", 3, 6);
        participantsEgypt = ProductFeatureType.withNumericRange("liczba uczestników", 2, 4);
        participantsCyberpunk = ProductFeatureType.withNumericRange("liczba uczestników", 4, 6);

        cateringVariantFeature =
                ProductFeatureType.withAllowedValues(
                        "rodzaj cateringu",
                        CATERING_PIZZA,
                        CATERING_SUSHI,
                        CATERING_VEGETARIAN);

        // Rooms - individually tracked (each session is a unique booking)
        // Difficulty, duration and participant count are mandatory features of each room

        labSzalonegoNaukowca =
                ProductType.builder(
                                UuidProductIdentifier.random(),
                                ProductName.of("Laboratorium szalonego naukowca"),
                                ProductDescription.of(
                                        "Odkryj sekrety szalonego naukowca zanim jego eksperyment wymknie się spod kontroli. 2-5 graczy, 60 minut."),
                                Unit.pieces(),
                                INDIVIDUALLY_TRACKED)
                        .withMandatoryFeature(difficultyFeature)
                        .withMandatoryFeature(durationFeature)
                        .withMandatoryFeature(cityFeature)
                        .withMandatoryFeature(participantsLab)
                        .withOptionalFeature(vrFeature)
                        .withApplicabilityConstraint(
                                and(
                                        between("participants", 2, 5),
                                        in("city", CITY_WARSAW, CITY_LODZ, CITY_WROCLAW),
                                        or(not(equalsTo("VR", VR_YES)), equalsTo("city", CITY_WARSAW))))
                        .build();

        wiezienieAlcatraz =
                ProductType.builder(
                                UuidProductIdentifier.random(),
                                ProductName.of("Więzienie Alcatraz"),
                                ProductDescription.of(
                                        "Ucieknij z najsłynniejszego więzienia na świecie. 3-6 graczy, 75 minut."),
                                Unit.pieces(),
                                INDIVIDUALLY_TRACKED)
                        .withMandatoryFeature(difficultyFeature)
                        .withMandatoryFeature(durationFeature)
                        .withMandatoryFeature(cityFeature)
                        .withMandatoryFeature(participantsAlcatraz)
                        .withOptionalFeature(vrFeature)
                        .withApplicabilityConstraint(
                                and(
                                        between("participants", 3, 6),
                                        in("city", CITY_WARSAW, CITY_LODZ, CITY_WROCLAW),
                                        or(not(equalsTo("VR", VR_YES)), equalsTo("city", CITY_WARSAW)),
                                        not(equalsTo("claustrophobia", "yes"))))
                        .build();

        egipskiGrobowiec =
                ProductType.builder(
                                UuidProductIdentifier.random(),
                                ProductName.of("Egipski grobowiec"),
                                ProductDescription.of(
                                        "Odkryj skarby faraona i wydostań się z grobowca przed uruchomieniem pułapek. 2-4 graczy, 45 minut."),
                                Unit.pieces(),
                                INDIVIDUALLY_TRACKED)
                        .withMandatoryFeature(difficultyFeature)
                        .withMandatoryFeature(durationFeature)
                        .withMandatoryFeature(cityFeature)
                        .withMandatoryFeature(participantsEgypt)
                        .withOptionalFeature(vrFeature)
                        .withApplicabilityConstraint(
                                and(
                                        between("participants", 2, 4),
                                        in("city", CITY_WARSAW, CITY_LODZ, CITY_WROCLAW),
                                        or(not(equalsTo("VR", VR_YES)), equalsTo("city", CITY_WARSAW))))
                        .build();

        cyberpunk2077 =
                ProductType.builder(
                                UuidProductIdentifier.random(),
                                ProductName.of("Cyberpunk 2077"),
                                ProductDescription.of(
                                        "Zhakuj system korporacji i ucieknij z Night City. 4-6 graczy, 90 minut."),
                                Unit.pieces(),
                                INDIVIDUALLY_TRACKED)
                        .withMandatoryFeature(difficultyFeature)
                        .withMandatoryFeature(durationFeature)
                        .withMandatoryFeature(cityFeature)
                        .withMandatoryFeature(participantsCyberpunk)
                        .withMandatoryFeature(vrFeature)
                        .withApplicabilityConstraint(
                                and(
                                        between("participants", 4, 6),
                                        equalsTo("city", CITY_WARSAW),
                                        equalsTo("VR", VR_YES)))
                        .build();

        // Add-ons
        actorInRoom =
                ProductType.builder(
                                UuidProductIdentifier.random(),
                                ProductName.of("Aktor w pokoju"),
                                ProductDescription.of(
                                        "Profesjonalny aktor zwiększający immersję (+30% do ceny)"),
                                Unit.pieces(),
                                ProductTrackingStrategy.IDENTICAL)
                        .withApplicabilityConstraint(in("dayOfWeek", "Saturday", "Sunday"))
                        .build();

        dedicatedGameMaster =
                ProductType.identical(
                        UuidProductIdentifier.random(),
                        ProductName.of("Dedykowany game master"),
                        ProductDescription.of("Osobista opieka mistrza gry przez całą sesję"),
                        Unit.pieces());

        // Catering - single product with three variants (pizza, sushi, wegetariański)
        catering =
                ProductType.builder(
                                UuidProductIdentifier.random(),
                                ProductName.of("Catering"),
                                ProductDescription.of("Zestaw jedzenia dla całej grupy"),
                                Unit.pieces(),
                                ProductTrackingStrategy.IDENTICAL)
                        .withMandatoryFeature(cateringVariantFeature)
                        .build();

        // Souvenirs
        photoAndVideoPackage =
                ProductType.identical(
                        UuidProductIdentifier.random(),
                        ProductName.of("Pakiet zdjęć i video"),
                        ProductDescription.of(
                                "Profesjonalna sesja zdjęciowa oraz nagranie wideo z gry"),
                        Unit.pieces());

        // === Escape Room Experience ===
        // Room (choose one) + optional add-ons
        escapeRoomExperience =
                Product.builder(
                                UuidProductIdentifier.random(),
                                ProductName.of("Escape Room Experience"),
                                ProductDescription.of(
                                        "Kompletne doświadczenie escape room z wybranym pokojem"))
                        .asPackageType()
                        .withTrackingStrategy(INDIVIDUALLY_TRACKED)
                        .withSingleChoice(
                                "Pokój",
                                labSzalonegoNaukowca.id(),
                                wiezienieAlcatraz.id(),
                                egipskiGrobowiec.id(),
                                cyberpunk2077.id())
                        .withChoice(
                                "Dodatki",
                                0,
                                10,
                                actorInRoom.id(),
                                photoAndVideoPackage.id(),
                                dedicatedGameMaster.id(),
                                catering.id())
                        .build();

        // 1. „Urodziny” = dowolny pokój + catering + pakiet zdjęć
        birthdayPackage =
                Product.builder(
                                UuidProductIdentifier.random(),
                                ProductName.of("Urodziny"),
                                ProductDescription.of("dowolny pokój + catering + pakiet zdjęć"))
                        .asPackageType()
                        .withTrackingStrategy(INDIVIDUALLY_TRACKED)
                        .withSingleChoice(
                                "Pokój",
                                labSzalonegoNaukowca.id(),
                                wiezienieAlcatraz.id(),
                                egipskiGrobowiec.id(),
                                cyberpunk2077.id())
                        .withSingleChoice("Catering", catering.id())
                        .withSingleChoice("Pakiet zdjęć", photoAndVideoPackage.id())
                        .build();

        // 2. „Hardcore” = Cyberpunk 2077 + aktor + dedykowany GM (tylko dla osób 18+)
        hardcorePackage =
                Product.builder(
                                UuidProductIdentifier.random(),
                                ProductName.of("Hardcore"),
                                ProductDescription.of(
                                        "Cyberpunk 2077 + aktor + dedykowany GM (tylko dla osób 18+)"))
                        .asPackageType()
                        .withTrackingStrategy(INDIVIDUALLY_TRACKED)
                        .withSingleChoice("Pokój", cyberpunk2077.id())
                        .withSingleChoice("Aktor", actorInRoom.id())
                        .withSingleChoice("Dedykowany GM", dedicatedGameMaster.id())
                        .withApplicabilityConstraint(between("wiek", 18, 120))
                        .build();

        // 3. „Team building” = 2 pokoje (sekwencyjnie) + catering + dedykowany GM
        // Relacje: zgodność osób i progresja trudności (łatwy -> ekstremalny)

        ProductIdentifier[] roomIds = {
            labSzalonegoNaukowca.id(),
            wiezienieAlcatraz.id(),
            egipskiGrobowiec.id(),
            cyberpunk2077.id()
        };

        // Definiujemy zestawy kompatybilnych pokoi jako drugi wybór dla każdego pierwszego wyboru
        // Egypt (Easy, 2-4) -> Lab (Medium, 2-5) [Kompatybilne osoby]
        // Lab (Medium, 2-5) -> Alcatraz (Hard, 3-6) [Kompatybilne osoby]
        // Alcatraz (Hard, 3-6) -> Cyberpunk (Extreme, 4-6) [Kompatybilne osoby]

        ProductSet afterEgypt = ProductSet.of("Po Egipskim grobowcu", labSzalonegoNaukowca.id());
        ProductSet afterLab = ProductSet.of("Po Laboratorium", wiezienieAlcatraz.id());
        ProductSet afterAlcatraz = ProductSet.of("Po Alcatraz", cyberpunk2077.id());

        teamBuildingPackage =
                Product.builder(
                                UuidProductIdentifier.random(),
                                ProductName.of("Team building"),
                                ProductDescription.of("2 pokoje (sekwencyjnie) + catering + dedykowany GM"))
                        .asPackageType()
                        .withTrackingStrategy(INDIVIDUALLY_TRACKED)
                        .withSingleChoice("Pierwszy pokój", roomIds)
                        .withSingleChoice("Drugi pokój", roomIds)
                        .withSingleChoice("Catering", catering.id())
                        .withSingleChoice("Dedykowany GM", dedicatedGameMaster.id())
                        // Reguły relacji (progresja trudności i zgodność osób)
                        .withRule(
                                SelectionRule.and(
                                        SelectionRule.ifThen(
                                                SelectionRule.single(
                                                        ProductSet.of(
                                                                "Egipt", egipskiGrobowiec.id())),
                                                SelectionRule.single(afterEgypt)),
                                        SelectionRule.ifThen(
                                                SelectionRule.single(
                                                        ProductSet.of(
                                                                "Lab", labSzalonegoNaukowca.id())),
                                                SelectionRule.single(afterLab)),
                                        SelectionRule.ifThen(
                                                SelectionRule.single(
                                                        ProductSet.of(
                                                                "Alcatraz", wiezienieAlcatraz.id())),
                                                SelectionRule.single(afterAlcatraz))))
                        .build();
    }
}
