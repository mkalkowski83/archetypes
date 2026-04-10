package com.softwarearchetypes.planvsexecution.badimplementation.deliveryscheduling;

import java.time.LocalDate;

/**
 * Driver availability information. Part of the problem: delivery plan is reconstructed from
 * multiple entities like this one.
 */
public class DriverAvailability {
    private Long driverId;
    private String driverName;
    private LocalDate date;
    private boolean available;
    private int maxDeliveries; // Maximum deliveries this driver can handle per day

    public DriverAvailability(
            Long driverId,
            String driverName,
            LocalDate date,
            boolean available,
            int maxDeliveries) {
        this.driverId = driverId;
        this.driverName = driverName;
        this.date = date;
        this.available = available;
        this.maxDeliveries = maxDeliveries;
    }

    public Long getDriverId() {
        return driverId;
    }

    public String getDriverName() {
        return driverName;
    }

    public LocalDate getDate() {
        return date;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public int getMaxDeliveries() {
        return maxDeliveries;
    }
}
