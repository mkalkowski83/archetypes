package com.softwarearchetypes.planvsexecution.badimplementation.deliveryscheduling;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * Working calendar with holidays and non-working days. Part of the problem: delivery plan is
 * reconstructed from multiple entities like this one.
 */
public class WorkingCalendar {
    private Set<LocalDate> holidays;
    private Set<Integer> nonWorkingDaysOfWeek; // 6=Saturday, 7=Sunday

    public WorkingCalendar() {
        this.holidays = new HashSet<>();
        this.nonWorkingDaysOfWeek = new HashSet<>();
        this.nonWorkingDaysOfWeek.add(6); // Saturday
        this.nonWorkingDaysOfWeek.add(7); // Sunday
    }

    public void addHoliday(LocalDate date) {
        this.holidays.add(date);
    }

    public boolean isWorkingDay(LocalDate date) {
        return !holidays.contains(date)
                && !nonWorkingDaysOfWeek.contains(date.getDayOfWeek().getValue());
    }

    public LocalDate addWorkingDays(LocalDate start, int workingDays) {
        LocalDate result = start;
        int added = 0;
        while (added < workingDays) {
            result = result.plusDays(1);
            if (isWorkingDay(result)) {
                added++;
            }
        }
        return result;
    }
}
