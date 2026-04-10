package com.softwarearchetypes.planvsexecution.repaymentanalysis.tolerance;

import com.softwarearchetypes.quantity.money.Money;
import com.softwarearchetypes.quantity.money.Percentage;
import java.math.BigDecimal;

public class ToleranceBuilder {

    private ToleranceStrategy moneyStrategy;
    private ToleranceStrategy dateStrategy;

    private ToleranceBuilder() {}

    public static ToleranceBuilder tolerance() {
        return new ToleranceBuilder();
    }

    public static ToleranceStrategy exact() {
        return new ExactMatch();
    }

    public ToleranceBuilder money(Money tolerance) {
        this.moneyStrategy = moneyTolerance(tolerance);
        return this;
    }

    public ToleranceBuilder percentage(Percentage tolerance) {
        this.moneyStrategy = new PercentageTolerance(tolerance);
        return this;
    }

    public ToleranceBuilder days(long toleranceDays) {
        this.dateStrategy = new DateTolerance(toleranceDays);
        return this;
    }

    public ToleranceStrategy build() {
        if (moneyStrategy == null && dateStrategy == null) {
            return exact();
        }

        if (moneyStrategy != null && dateStrategy != null) {
            return new CombinedTolerance(moneyStrategy, dateStrategy);
        }

        if (moneyStrategy != null) {
            return moneyStrategy;
        }

        return dateStrategy;
    }

    public static ToleranceStrategy fiveGroszy() {
        return moneyTolerance(Money.pln(0.05));
    }

    public static ToleranceStrategy moneyTolerance(Money pln) {
        return new MoneyTolerance(pln);
    }

    public static ToleranceStrategy halfPercent() {
        return new PercentageTolerance(Percentage.of(BigDecimal.valueOf(0.5)));
    }

    public static ToleranceStrategy threeDays() {
        return new DateTolerance(3);
    }

    public static ToleranceStrategy lenient() {
        return tolerance().money(Money.pln(0.05)).days(3).build();
    }

    public static ToleranceStrategy partialPayments(Money tolerance, java.time.Instant deadline) {
        return new PartialPaymentsStrategy(tolerance, deadline);
    }
}
