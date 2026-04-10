package com.softwarearchetypes.pricing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.softwarearchetypes.quantity.money.Money;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Use case tests demonstrating real-world pricing scenarios using CompositeFunctionCalculator with
 * different range types (numeric, time, date).
 */
class CompositeFunctionCalculatorScenariosTest {

    private CalculatorRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryCalculatorsRepository();
    }

    @Test
    void use_case_parking_pricing_by_time_of_day() {
        // given - parking with different rates for day and night
        SimpleFixedCalculator dayRate =
                new SimpleFixedCalculator(
                        "parking-day-rate", Money.pln(5.00) // 5 PLN/hour during day
                        );
        SimpleFixedCalculator nightRate =
                new SimpleFixedCalculator(
                        "parking-night-rate", Money.pln(2.00) // 2 PLN/hour during night
                        );

        repository.save(dayRate);
        repository.save(nightRate);

        Ranges timeRanges =
                new Ranges(
                        "parkingTime",
                        List.of(
                                CalculatorRange.time(
                                        LocalTime.of(6, 0),
                                        LocalTime.of(22, 0),
                                        dayRate.id()), // 6-22: day
                                CalculatorRange.time(
                                        LocalTime.of(22, 0),
                                        LocalTime.of(6, 0),
                                        nightRate.id()) // 22-6: night
                                ));

        CompositeFunctionCalculator parkingPricing =
                new CompositeFunctionCalculator("parking-hourly-rate", timeRanges, repository);

        // when - parking at 3 PM (day rate)
        Parameters dayParams = new Parameters(Map.of("parkingTime", LocalTime.of(15, 0)));
        Money dayPrice = parkingPricing.calculate(dayParams);

        // then
        assertEquals(0, new BigDecimal("5.00").compareTo(dayPrice.value()));

        // when - parking at 11 PM (night rate)
        Parameters nightParams = new Parameters(Map.of("parkingTime", LocalTime.of(23, 0)));
        Money nightPrice = parkingPricing.calculate(nightParams);

        // then
        assertEquals(0, new BigDecimal("2.00").compareTo(nightPrice.value()));
    }

    @Test
    void use_case_volume_discount_by_quantity() {
        // given - volume-based pricing for products
        SimpleFixedCalculator smallOrder =
                new SimpleFixedCalculator(
                        "price-small", Money.pln(10.00) // 10 PLN per unit for 1-10 items
                        );
        SimpleFixedCalculator mediumOrder =
                new SimpleFixedCalculator(
                        "price-medium", Money.pln(8.00) // 8 PLN per unit for 10-50 items
                        );
        SimpleFixedCalculator largeOrder =
                new SimpleFixedCalculator(
                        "price-large", Money.pln(6.00) // 6 PLN per unit for 50+ items
                        );

        repository.save(smallOrder);
        repository.save(mediumOrder);
        repository.save(largeOrder);

        Ranges quantityRanges =
                new Ranges(
                        "quantity",
                        List.of(
                                CalculatorRange.numeric(
                                        new BigDecimal("1"), new BigDecimal("10"), smallOrder.id()),
                                CalculatorRange.numeric(
                                        new BigDecimal("10"),
                                        new BigDecimal("50"),
                                        mediumOrder.id()),
                                CalculatorRange.numeric(
                                        new BigDecimal("50"),
                                        new BigDecimal("1000"),
                                        largeOrder.id())));

        CompositeFunctionCalculator volumePricing =
                new CompositeFunctionCalculator("volume-discount", quantityRanges, repository);

        // when - ordering 5 items (small order)
        Parameters smallParams = new Parameters(Map.of("quantity", new BigDecimal("5")));
        Money smallPrice = volumePricing.calculate(smallParams);

        // then
        assertEquals(0, new BigDecimal("10.00").compareTo(smallPrice.value()));

        // when - ordering 25 items (medium order)
        Parameters mediumParams = new Parameters(Map.of("quantity", new BigDecimal("25")));
        Money mediumPrice = volumePricing.calculate(mediumParams);

        // then
        assertEquals(0, new BigDecimal("8.00").compareTo(mediumPrice.value()));

        // when - ordering 100 items (large order)
        Parameters largeParams = new Parameters(Map.of("quantity", new BigDecimal("100")));
        Money largePrice = volumePricing.calculate(largeParams);

        // then
        assertEquals(0, new BigDecimal("6.00").compareTo(largePrice.value()));
    }

    @Test
    void use_case_shipping_cost_by_weight() {
        // given - shipping costs based on package weight
        SimpleFixedCalculator tinyPackage =
                new SimpleFixedCalculator(
                        "shipping-tiny", Money.pln(12.00) // 0-1 kg
                        );
        SimpleFixedCalculator smallPackage =
                new SimpleFixedCalculator(
                        "shipping-small", Money.pln(18.00) // 1-5 kg
                        );
        SimpleFixedCalculator mediumPackage =
                new SimpleFixedCalculator(
                        "shipping-medium", Money.pln(28.00) // 5-10 kg
                        );
        SimpleFixedCalculator largePackage =
                new SimpleFixedCalculator(
                        "shipping-large", Money.pln(45.00) // 10-20 kg
                        );

        repository.save(tinyPackage);
        repository.save(smallPackage);
        repository.save(mediumPackage);
        repository.save(largePackage);

        Ranges weightRanges =
                new Ranges(
                        "weight",
                        List.of(
                                CalculatorRange.numeric(
                                        new BigDecimal("0"), new BigDecimal("1"), tinyPackage.id()),
                                CalculatorRange.numeric(
                                        new BigDecimal("1"),
                                        new BigDecimal("5"),
                                        smallPackage.id()),
                                CalculatorRange.numeric(
                                        new BigDecimal("5"),
                                        new BigDecimal("10"),
                                        mediumPackage.id()),
                                CalculatorRange.numeric(
                                        new BigDecimal("10"),
                                        new BigDecimal("20"),
                                        largePackage.id())));

        CompositeFunctionCalculator shippingPricing =
                new CompositeFunctionCalculator("shipping-by-weight", weightRanges, repository);

        // when - shipping 0.5 kg package
        Parameters tinyParams = new Parameters(Map.of("weight", new BigDecimal("0.5")));
        Money tinyPrice = shippingPricing.calculate(tinyParams);

        // then
        assertEquals(0, new BigDecimal("12.00").compareTo(tinyPrice.value()));

        // when - shipping 3.5 kg package
        Parameters smallParams = new Parameters(Map.of("weight", new BigDecimal("3.5")));
        Money smallPrice = shippingPricing.calculate(smallParams);

        // then
        assertEquals(0, new BigDecimal("18.00").compareTo(smallPrice.value()));

        // when - shipping 15 kg package
        Parameters largeParams = new Parameters(Map.of("weight", new BigDecimal("15")));
        Money largePrice = shippingPricing.calculate(largeParams);

        // then
        assertEquals(0, new BigDecimal("45.00").compareTo(largePrice.value()));
    }

    @Test
    void use_case_happy_hour_bar_pricing() {
        // given - bar with happy hour pricing
        SimpleFixedCalculator regularPrice =
                new SimpleFixedCalculator(
                        "drink-regular", Money.pln(25.00) // regular price
                        );
        SimpleFixedCalculator happyHourPrice =
                new SimpleFixedCalculator(
                        "drink-happy-hour", Money.pln(15.00) // happy hour discount
                        );

        repository.save(regularPrice);
        repository.save(happyHourPrice);

        Ranges happyHourRanges =
                new Ranges(
                        "orderTime",
                        List.of(
                                CalculatorRange.time(
                                        LocalTime.of(0, 0),
                                        LocalTime.of(17, 0),
                                        regularPrice.id()), // 0-17: regular
                                CalculatorRange.time(
                                        LocalTime.of(17, 0),
                                        LocalTime.of(19, 0),
                                        happyHourPrice.id()), // 17-19: happy hour
                                CalculatorRange.time(
                                        LocalTime.of(19, 0),
                                        LocalTime.of(23, 59),
                                        regularPrice.id()) // 19-24: regular
                                ));

        CompositeFunctionCalculator barPricing =
                new CompositeFunctionCalculator("bar-pricing", happyHourRanges, repository);

        // when - ordering at 2 PM (regular)
        Parameters afternoonParams = new Parameters(Map.of("orderTime", LocalTime.of(14, 0)));
        Money afternoonPrice = barPricing.calculate(afternoonParams);

        // then
        assertEquals(0, new BigDecimal("25.00").compareTo(afternoonPrice.value()));

        // when - ordering at 6 PM (happy hour)
        Parameters happyParams = new Parameters(Map.of("orderTime", LocalTime.of(18, 0)));
        Money happyPrice = barPricing.calculate(happyParams);

        // then
        assertEquals(0, new BigDecimal("15.00").compareTo(happyPrice.value()));

        // when - ordering at 9 PM (regular)
        Parameters eveningParams = new Parameters(Map.of("orderTime", LocalTime.of(21, 0)));
        Money eveningPrice = barPricing.calculate(eveningParams);

        // then
        assertEquals(0, new BigDecimal("25.00").compareTo(eveningPrice.value()));
    }

    @Test
    void use_case_transfer_fee_by_amount() {
        // given - bank transfer fees based on amount
        SimpleFixedCalculator freeTransfer =
                new SimpleFixedCalculator(
                        "transfer-free", Money.zeroPln() // 0-100: free
                        );
        SimpleFixedCalculator smallFee =
                new SimpleFixedCalculator(
                        "transfer-small-fee", Money.pln(2.00) // 100-1000: 2 PLN
                        );
        SimpleFixedCalculator mediumFee =
                new SimpleFixedCalculator(
                        "transfer-medium-fee", Money.pln(5.00) // 1000-10000: 5 PLN
                        );

        repository.save(freeTransfer);
        repository.save(smallFee);
        repository.save(mediumFee);

        Ranges amountRanges =
                new Ranges(
                        "amount",
                        List.of(
                                CalculatorRange.numeric(
                                        new BigDecimal("0"),
                                        new BigDecimal("100"),
                                        freeTransfer.id()),
                                CalculatorRange.numeric(
                                        new BigDecimal("100"),
                                        new BigDecimal("1000"),
                                        smallFee.id()),
                                CalculatorRange.numeric(
                                        new BigDecimal("1000"),
                                        new BigDecimal("10000"),
                                        mediumFee.id())));

        CompositeFunctionCalculator transferFees =
                new CompositeFunctionCalculator("transfer-fees", amountRanges, repository);

        // when - transferring 50 PLN (free)
        Parameters smallTransfer = new Parameters(Map.of("amount", new BigDecimal("50")));
        Money smallFeeAmount = transferFees.calculate(smallTransfer);

        // then
        assertEquals(0, BigDecimal.ZERO.compareTo(smallFeeAmount.value()));

        // when - transferring 500 PLN (2 PLN fee)
        Parameters mediumTransfer = new Parameters(Map.of("amount", new BigDecimal("500")));
        Money mediumFeeAmount = transferFees.calculate(mediumTransfer);

        // then
        assertEquals(0, new BigDecimal("2.00").compareTo(mediumFeeAmount.value()));

        // when - transferring 5000 PLN (5 PLN fee)
        Parameters largeTransfer = new Parameters(Map.of("amount", new BigDecimal("5000")));
        Money largeFeeAmount = transferFees.calculate(largeTransfer);

        // then
        assertEquals(0, new BigDecimal("5.00").compareTo(largeFeeAmount.value()));
    }
}
