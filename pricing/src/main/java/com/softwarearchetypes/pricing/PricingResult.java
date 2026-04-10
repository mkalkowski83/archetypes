package com.softwarearchetypes.pricing;

import com.softwarearchetypes.quantity.Unit;
import com.softwarearchetypes.quantity.money.Money;

/**
 * Sealed interface representing different interpretations of pricing calculations.
 *
 * <p>A pricing result carries both the monetary value AND its semantic meaning: - TotalPrice: whole
 * cost for entire quantity/period - UnitPrice: average price per single unit - MarginalPrice: price
 * of the n-th specific unit
 *
 * <p>This design ensures type safety and makes the meaning of calculated prices explicit.
 */
public sealed interface PricingResult permits TotalPrice, UnitPrice, MarginalPrice {

    /** The monetary value of this pricing result. */
    Money money();

    /** Human-readable description of what this price represents. */
    String describe();
}

/**
 * Represents the total price for entire quantity or period.
 *
 * <p>Example: "For 15 items, you pay PLN 150"
 *
 * <p>This is the most common pricing result - used for invoices, shopping carts, and any situation
 * where you need to know the complete cost.
 */
record TotalPrice(Money amount) implements PricingResult {

    @Override
    public Money money() {
        return amount;
    }

    @Override
    public String describe() {
        return "Total: " + amount;
    }
}

/**
 * Represents the average price per single unit.
 *
 * <p>Example: "At quantity 15, average price is PLN 10/kg"
 *
 * <p>Used for comparing prices, displaying unit prices in stores (PLN/kg), and analyzing unit
 * costs. This is the AVERAGE price, not the price of each individual unit.
 */
record UnitPrice(Money amountPerUnit, Unit unit) implements PricingResult {

    @Override
    public Money money() {
        return amountPerUnit;
    }

    @Override
    public String describe() {
        return "Unit price: " + amountPerUnit + "/" + unit;
    }
}

/**
 * Represents the price of the n-th specific unit.
 *
 * <p>Example: "The 15th kilogram costs PLN 8"
 *
 * <p>Used for marginal analysis - "does it pay off to buy one more unit?" Helps optimize orders and
 * analyze marginal costs.
 */
record MarginalPrice(Money amount, int unitIndex, Unit unit) implements PricingResult {

    @Override
    public Money money() {
        return amount;
    }

    @Override
    public String describe() {
        return unitIndex + "-th " + unit.name() + ": " + amount;
    }
}
