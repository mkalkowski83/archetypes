package com.softwarearchetypes.rules.discounting.client.rules;

import com.softwarearchetypes.rules.discounting.client.ClientContext;
import com.softwarearchetypes.rules.predicates.RichLogicalPredicate;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;

public class TimeBeingCustomer implements RichLogicalPredicate<ClientContext> {

    private final int threshold;
    private final ChronoUnit unit;

    public TimeBeingCustomer(int threshold, ChronoUnit unit) {
        this.threshold = threshold;
        this.unit = unit;
    }

    public static TimeBeingCustomer ofDays(int threshold) {
        return new TimeBeingCustomer(threshold, ChronoUnit.DAYS);
    }

    public static TimeBeingCustomer ofWeeks(int threshold) {
        return new TimeBeingCustomer(threshold, ChronoUnit.WEEKS);
    }

    public static TimeBeingCustomer ofMonths(int threshold) {
        return new TimeBeingCustomer(threshold, ChronoUnit.MONTHS);
    }

    public static TimeBeingCustomer ofYears(int threshold) {
        return new TimeBeingCustomer(threshold, ChronoUnit.YEARS);
    }

    @Override
    public boolean test(ClientContext clientContext) {
        Period period = Period.between(clientContext.firstOrder(), LocalDate.now());
        switch (unit) {
            case DAYS:
                {
                    return period.getDays() >= threshold;
                }
            case WEEKS:
                {
                    return period.getDays() % 7 >= threshold;
                }
            case MONTHS:
                {
                    return period.getMonths() >= threshold;
                }
            case YEARS:
                {
                    return period.getYears() >= threshold;
                }
            default:
                return false;
        }
    }

    public int getThreshold() {
        return threshold;
    }

    public ChronoUnit getUnit() {
        return unit;
    }
}
