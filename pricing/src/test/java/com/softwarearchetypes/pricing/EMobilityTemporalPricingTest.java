package com.softwarearchetypes.pricing;

import static org.assertj.core.api.Assertions.assertThat;

import com.softwarearchetypes.quantity.money.Money;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * eMobility Temporal Pricing - demonstracja wersjonowania cennika przez fasadę.
 *
 * <p>KLUCZOWE: Test operuje wyłącznie przez PricingFacade - Kalkulatory tworzymy przez
 * facade.addCalculator() - Komponenty tworzymy przez
 * facade.createSimpleComponent/createCompositeComponent() - Kolejne wywołania create*() na tej
 * samej nazwie DODAJĄ nową wersję - Obliczenia przez facade.calculateComponent*()
 */
class EMobilityTemporalPricingTest {

    private Clock fixedClock;
    private PricingFacade facade;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneId.systemDefault());

        facade = PricingConfiguration.inMemory(fixedClock).pricingFacade();

        // Rejestrujemy WSZYSTKIE kalkulatory z góry
        registerCalculators();

        // Tworzymy komponenty przez fasadę
        createInitialComponents();
    }

    private void registerCalculators() {
        // Energy calculators - StepFunction: basePrice + (quantity/stepSize) * stepIncrement
        facade.addCalculator(
                "energy-2.50",
                CalculatorType.SIMPLE_FIXED,
                Parameters.of("amount", Money.pln(2.50), "interpretation", Interpretation.UNIT));

        facade.addCalculator(
                "energy-2.00",
                CalculatorType.SIMPLE_FIXED,
                Parameters.of("amount", Money.pln(2.00), "interpretation", Interpretation.UNIT));

        facade.addCalculator(
                "energy-2.80",
                CalculatorType.SIMPLE_FIXED,
                Parameters.of("amount", Money.pln(2.80), "interpretation", Interpretation.UNIT));

        // VAT calculator - Percentage
        facade.addCalculator(
                "vat-23",
                CalculatorType.PERCENTAGE,
                Parameters.of("percentageRate", BigDecimal.valueOf(23)));

        // Parking calculators - SimpleFixed
        facade.addCalculator(
                "parking-5", CalculatorType.SIMPLE_FIXED, Parameters.of("amount", Money.pln(5)));

        facade.addCalculator(
                "parking-8", CalculatorType.SIMPLE_FIXED, Parameters.of("amount", Money.pln(8)));
    }

    private void createInitialComponents() {
        // 1. EnergyCharge - bazowa 2.50 PLN/kWh
        facade.createSimpleComponent(
                "EnergyCharge",
                "energy-2.50",
                Map.of("kwh", "quantity"),
                Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0)));

        // 2. VAT - 23%
        facade.createSimpleComponent(
                "VAT",
                "vat-23",
                Map.of("baseAmount", "baseAmount"),
                Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0)));

        // 3. ParkingFee - 5 PLN (od maja)
        facade.createSimpleComponent(
                "ParkingFee",
                "parking-5",
                Map.of(),
                Validity.from(LocalDateTime.of(2024, 5, 1, 0, 0)));

        // 4. TotalPrice - kompozycja [EnergyCharge, VAT]
        facade.createCompositeComponent(
                "TotalPrice",
                Map.of("VAT", Map.of("baseAmount", new ValueOf("EnergyCharge"))),
                Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0)),
                "EnergyCharge",
                "VAT");
    }

    @Test
    void shouldCalculatePriceInJanuary_BasePrice() {
        // given: Sesja ładowania w styczniu, 20 kWh
        Parameters jan15 =
                Parameters.of(
                        "timestamp", LocalDateTime.of(2024, 1, 15, 10, 30),
                        "kwh", BigDecimal.valueOf(20));

        // when: Obliczamy przez fasadę
        ComponentBreakdown breakdown = facade.calculateComponentBreakdown("TotalPrice", jan15);

        // then: 20 kWh × 2.50 PLN = 50.00 PLN + VAT 23% = 61.50 PLN
        assertThat(breakdown.total()).isEqualTo(Money.pln(61.50));
        assertThat(breakdown.children()).hasSize(2);
        assertThat(breakdown.children().get(0).name()).isEqualTo("EnergyCharge");
        assertThat(breakdown.children().get(0).total()).isEqualTo(Money.pln(50.00));

        System.out.println("=== STYCZEŃ 2024 - Cennik bazowy ===");
        System.out.println("Energia: 20 kWh × 2.50 PLN = " + breakdown.children().get(0).total());
        System.out.println("VAT 23%: " + breakdown.children().get(1).total());
        System.out.println("RAZEM: " + breakdown.total());
        System.out.println();
    }

    @Test
    void shouldApplyValentinePromotion_February() {
        // given: Walentynkowa promocja - dodajemy nową wersję przez create
        facade.createSimpleComponent(
                "EnergyCharge",
                "energy-2.00",
                Map.of("kwh", "quantity"),
                Validity.between(
                        LocalDateTime.of(2024, 2, 1, 0, 0), LocalDateTime.of(2024, 3, 1, 0, 0)));

        // when: Sesja 14 lutego
        Parameters feb14 =
                Parameters.of(
                        "timestamp", LocalDateTime.of(2024, 2, 14, 14, 0),
                        "kwh", BigDecimal.valueOf(20));

        ComponentBreakdown breakdown = facade.calculateComponentBreakdown("TotalPrice", feb14);

        // then: 20 kWh × 2.00 PLN = 40.00 PLN + VAT 23% = 49.20 PLN
        assertThat(breakdown.total()).isEqualTo(Money.pln(49.20));
        assertThat(breakdown.children().get(0).total()).isEqualTo(Money.pln(40.00));

        System.out.println("=== LUTY 2024 - Walentynkowa promocja ===");
        System.out.println(
                "Energia: 20 kWh × 2.00 PLN = "
                        + breakdown.children().get(0).total()
                        + " ← PROMOCJA!");
        System.out.println("VAT 23%: " + breakdown.children().get(1).total());
        System.out.println("RAZEM: " + breakdown.total() + " (-12.30 PLN taniej!)");
        System.out.println();
    }

    @Test
    void shouldRevertToBasePriceAfterPromotion_March() {
        // given: Dodajemy promocję lutową
        facade.createSimpleComponent(
                "EnergyCharge",
                "energy-2.00",
                Map.of("kwh", "quantity"),
                Validity.between(
                        LocalDateTime.of(2024, 2, 1, 0, 0), LocalDateTime.of(2024, 3, 1, 0, 0)));

        // when: Sesja 10 marca (PO promocji)
        Parameters mar10 =
                Parameters.of(
                        "timestamp", LocalDateTime.of(2024, 3, 10, 16, 45),
                        "kwh", BigDecimal.valueOf(20));

        ComponentBreakdown breakdown = facade.calculateComponentBreakdown("TotalPrice", mar10);

        // then: Automatyczny powrót do 2.50 PLN/kWh
        assertThat(breakdown.total()).isEqualTo(Money.pln(61.50));
        assertThat(breakdown.children().get(0).total()).isEqualTo(Money.pln(50.00));

        System.out.println("=== MARZEC 2024 - Powrót do cennika bazowego ===");
        System.out.println(
                "Energia: 20 kWh × 2.50 PLN = "
                        + breakdown.children().get(0).total()
                        + " ← automatyczny powrót");
        System.out.println("VAT 23%: " + breakdown.children().get(1).total());
        System.out.println("RAZEM: " + breakdown.total());
        System.out.println();
    }

    @Test
    void shouldAddParkingFeeInMay_CompositeVersionUpdate() {
        // given: Od maja ZMIENIAMY SKŁAD - dodajemy parking przez create
        facade.createCompositeComponent(
                "TotalPrice",
                Map.of("VAT", Map.of("baseAmount", new SumOf("EnergyCharge", "ParkingFee"))),
                Validity.from(LocalDateTime.of(2024, 5, 1, 0, 0)),
                "EnergyCharge",
                "ParkingFee",
                "VAT");

        // when: Sesja 20 maja
        Parameters may20 =
                Parameters.of(
                        "timestamp", LocalDateTime.of(2024, 5, 20, 12, 0),
                        "kwh", BigDecimal.valueOf(20));

        ComponentBreakdown breakdown = facade.calculateComponentBreakdown("TotalPrice", may20);

        // then: (50.00 + 5.00) * 1,23 = 67.65 PLN
        assertThat(breakdown.total()).isEqualTo(Money.pln(67.65));
        assertThat(breakdown.children()).hasSize(3);
        assertThat(breakdown.children().get(0).name()).isEqualTo("EnergyCharge");
        assertThat(breakdown.children().get(1).name()).isEqualTo("ParkingFee");
        assertThat(breakdown.children().get(2).name()).isEqualTo("VAT");

        System.out.println("=== MAJ 2024 - Dodanie parkingu ===");
        System.out.println("Energia: " + breakdown.children().get(0).total());
        System.out.println("Parking: " + breakdown.children().get(1).total() + " ← NOWY SKŁADNIK");
        System.out.println("VAT: " + breakdown.children().get(2).total());
        System.out.println("RAZEM: " + breakdown.total());
        System.out.println();
    }

    @Test
    void shouldIncreasePriceInSummer_July() {
        // given: Dodajemy parking (maj)
        facade.createCompositeComponent(
                "TotalPrice",
                Map.of("VAT", Map.of("baseAmount", new SumOf("EnergyCharge", "ParkingFee"))),
                Validity.from(LocalDateTime.of(2024, 5, 1, 0, 0)),
                "EnergyCharge",
                "ParkingFee",
                "VAT");

        // Letnia podwyżka energii
        facade.createSimpleComponent(
                "EnergyCharge",
                "energy-2.80",
                Map.of("kwh", "quantity"),
                Validity.between(
                        LocalDateTime.of(2024, 7, 1, 0, 0), LocalDateTime.of(2024, 9, 1, 0, 0)));

        // when: Sesja 15 lipca
        Parameters jul15 =
                Parameters.of(
                        "timestamp", LocalDateTime.of(2024, 7, 15, 18, 20),
                        "kwh", BigDecimal.valueOf(20));

        ComponentBreakdown breakdown = facade.calculateComponentBreakdown("TotalPrice", jul15);

        // then: (20 * 2.80 + 5 ) * 1,23 = 75,03 PLN
        assertThat(breakdown.total()).isEqualTo(Money.pln(75.03));
        assertThat(breakdown.children().get(0).total()).isEqualTo(Money.pln(56.00));

        System.out.println("=== LIPIEC 2024 - Letnia podwyżka ===");
        System.out.println("Energia: " + breakdown.children().get(0).total() + " ← podwyżka");
        System.out.println("Parking: " + breakdown.children().get(1).total());
        System.out.println("VAT: " + breakdown.children().get(2).total());
        System.out.println("RAZEM: " + breakdown.total() + " ← NAJDROŻEJ!");
        System.out.println();
    }

    @Test
    void shouldRevertToBasePriceAfterSummer_September() {
        // given: Dodajemy parking (maj)
        facade.createCompositeComponent(
                "TotalPrice",
                Map.of("VAT", Map.of("baseAmount", new SumOf("EnergyCharge", "ParkingFee"))),
                Validity.from(LocalDateTime.of(2024, 5, 1, 0, 0)),
                "EnergyCharge",
                "ParkingFee",
                "VAT");

        // Letnia podwyżka energii (lipiec-sierpień)
        facade.createSimpleComponent(
                "EnergyCharge",
                "energy-2.80",
                Map.of("kwh", "quantity"),
                Validity.between(
                        LocalDateTime.of(2024, 7, 1, 0, 0),
                        LocalDateTime.of(2024, 9, 1, 0, 0) // Do końca sierpnia
                        ));

        // when: Sesja 15 września - NIC NIE ROBIMY, automatyczny powrót!
        Parameters sep15 =
                Parameters.of(
                        "timestamp", LocalDateTime.of(2024, 9, 15, 14, 0),
                        "kwh", BigDecimal.valueOf(20));

        ComponentBreakdown breakdown = facade.calculateComponentBreakdown("TotalPrice", sep15);

        // then: (50.00 + 5.00) * 1,23 = 67.65 PLN - automatyczny powrót do 2.50 PLN/kWh
        assertThat(breakdown.total()).isEqualTo(Money.pln(67.65));
        assertThat(breakdown.children().get(0).total())
                .isEqualTo(Money.pln(50.00)); // Energia z powrotem 2.50
        assertThat(breakdown.children().get(1).total())
                .isEqualTo(Money.pln(5.00)); // Parking nadal 5.00

        System.out.println("=== WRZESIEŃ 2024 - Automatyczny powrót po lecie ===");
        System.out.println(
                "Energia: "
                        + breakdown.children().get(0).total()
                        + " ← automatyczny powrót do 2.50 PLN/kWh");
        System.out.println("Parking: " + breakdown.children().get(1).total());
        System.out.println("VAT: " + breakdown.children().get(2).total());
        System.out.println("RAZEM: " + breakdown.total());
        System.out.println("⚡ NIC NIE MUSIELIŚMY ROBIĆ - system sam wrócił do ceny bazowej!");
        System.out.println();
    }

    @Test
    void shouldIncreaseWinterParkingFee_November() {
        facade.createCompositeComponent(
                "TotalPrice",
                Map.of("VAT", Map.of("baseAmount", new SumOf("EnergyCharge", "ParkingFee"))),
                Validity.from(LocalDateTime.of(2024, 5, 1, 0, 0)),
                "EnergyCharge",
                "ParkingFee",
                "VAT");

        // Letnia podwyżka energii
        facade.createSimpleComponent(
                "EnergyCharge",
                "energy-2.80",
                Map.of("kwh", "quantity"),
                Validity.between(
                        LocalDateTime.of(2024, 7, 1, 0, 0), LocalDateTime.of(2024, 9, 1, 0, 0)));
        // Zimowa podwyżka parkingu
        facade.createSimpleComponent(
                "ParkingFee",
                "parking-8",
                Map.of(),
                Validity.from(LocalDateTime.of(2024, 11, 1, 0, 0)));

        // when: Sesja 5 grudnia
        Parameters dec05 =
                Parameters.of(
                        "timestamp", LocalDateTime.of(2024, 12, 5, 8, 0),
                        "kwh", BigDecimal.valueOf(20));

        ComponentBreakdown breakdown = facade.calculateComponentBreakdown("TotalPrice", dec05);

        // then: (50.00 + 8.00) * 1,23 = 71.34 PLN
        assertThat(breakdown.total()).isEqualTo(Money.pln(71.34));
        assertThat(breakdown.children().get(1).total()).isEqualTo(Money.pln(8.00));

        System.out.println("=== GRUDZIEŃ 2024 - Zimowy parking ===");
        System.out.println("Energia: " + breakdown.children().get(0).total());
        System.out.println(
                "Parking: " + breakdown.children().get(1).total() + " ← zimowa podwyżka");
        System.out.println("VAT: " + breakdown.children().get(2).total());
        System.out.println("RAZEM: " + breakdown.total());
        System.out.println();
    }

    @Test
    void shouldVisualizeFullYearTimeline() {
        // given: Pełna konfiguracja
        setupFullYearPricing();

        System.out.println("╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║    EMOBILITY - TIMELINE CENNIKA 2024 (20 kWh)                 ║");
        System.out.println("╠════════════════════════════════════════════════════════════════╣");

        LocalDateTime[] dates = {
            LocalDateTime.of(2024, 1, 15, 12, 0),
            LocalDateTime.of(2024, 2, 14, 12, 0),
            LocalDateTime.of(2024, 3, 15, 12, 0),
            LocalDateTime.of(2024, 4, 15, 12, 0),
            LocalDateTime.of(2024, 5, 15, 12, 0),
            LocalDateTime.of(2024, 6, 15, 12, 0),
            LocalDateTime.of(2024, 7, 15, 12, 0),
            LocalDateTime.of(2024, 8, 15, 12, 0),
            LocalDateTime.of(2024, 9, 15, 12, 0),
            LocalDateTime.of(2024, 10, 15, 12, 0),
            LocalDateTime.of(2024, 11, 15, 12, 0),
            LocalDateTime.of(2024, 12, 15, 12, 0)
        };

        String[] months = {
            "STY", "LUT", "MAR", "KWI", "MAJ", "CZE", "LIP", "SIE", "WRZ", "PAŹ", "LIS", "GRU"
        };

        for (int i = 0; i < dates.length; i++) {
            Parameters params = Parameters.of("timestamp", dates[i], "kwh", BigDecimal.valueOf(20));
            Money price = facade.calculateComponent("TotalPrice", params);

            System.out.printf(
                    "║ %s  %s  %7s  %s%n",
                    months[i], getIndicator(price), price, getComment(dates[i].getMonthValue()));
        }

        System.out.println("╚════════════════════════════════════════════════════════════════╝");
    }

    private void setupFullYearPricing() {
        // Energy: promocja lutowa
        facade.createSimpleComponent(
                "EnergyCharge",
                "energy-2.00",
                Map.of("kwh", "quantity"),
                Validity.between(
                        LocalDateTime.of(2024, 2, 1, 0, 0), LocalDateTime.of(2024, 3, 1, 0, 0)));

        // Energy: letnia podwyżka
        facade.createSimpleComponent(
                "EnergyCharge",
                "energy-2.80",
                Map.of("kwh", "quantity"),
                Validity.between(
                        LocalDateTime.of(2024, 7, 1, 0, 0), LocalDateTime.of(2024, 9, 1, 0, 0)));

        // Parking: zimowa podwyżka
        facade.createSimpleComponent(
                "ParkingFee",
                "parking-8",
                Map.of(),
                Validity.from(LocalDateTime.of(2024, 11, 1, 0, 0)));

        // Composite: dodanie parkingu od maja
        facade.createCompositeComponent(
                "TotalPrice",
                Map.of("VAT", Map.of("baseAmount", new SumOf("EnergyCharge", "ParkingFee"))),
                Validity.from(LocalDateTime.of(2024, 5, 1, 0, 0)),
                "EnergyCharge",
                "ParkingFee",
                "VAT");
    }

    private String getIndicator(Money price) {
        double amount = price.value().doubleValue();
        if (amount < 50) {
            return "▁▁▁";
        }
        if (amount < 55) {
            return "▂▂▂";
        }
        if (amount < 60) {
            return "▃▃▃";
        }
        if (amount < 65) {
            return "▄▄▄";
        }
        if (amount < 70) {
            return "▅▅▅";
        }
        if (amount < 75) {
            return "▆▆▆";
        }
        return "▇▇▇";
    }

    private String getComment(int month) {
        return switch (month) {
            case 1 -> "║ ← Cennik bazowy";
            case 2 -> "║ ← PROMOCJA -20%!";
            case 3 -> "║ ← Powrót do bazowej";
            case 5 -> "║ ← +Parking 5 PLN";
            case 7 -> "║ ← Letnia podwyżka";
            case 9 -> "║ ← Powrót ceny";
            case 11 -> "║ ← Zimowy parking 8 PLN";
            default -> "║";
        };
    }
}
