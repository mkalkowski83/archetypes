package com.softwarearchetypes.planvsexecution.badimplementation.deliveryscheduling;

/**
 * Warehouse entity with capacity information. Part of the problem: delivery plan is reconstructed
 * from multiple entities like this one.
 */
public class Warehouse {
    private Long id;
    private String location;
    private int dailyCapacity; // Maximum deliveries per day
    private int currentLoad; // Current number of scheduled deliveries

    public Warehouse(Long id, String location, int dailyCapacity, int currentLoad) {
        this.id = id;
        this.location = location;
        this.dailyCapacity = dailyCapacity;
        this.currentLoad = currentLoad;
    }

    public Long getId() {
        return id;
    }

    public String getLocation() {
        return location;
    }

    public int getDailyCapacity() {
        return dailyCapacity;
    }

    public void setDailyCapacity(int dailyCapacity) {
        this.dailyCapacity = dailyCapacity;
    }

    public int getCurrentLoad() {
        return currentLoad;
    }

    public void setCurrentLoad(int currentLoad) {
        this.currentLoad = currentLoad;
    }

    public boolean hasCapacityFor(int deliveries) {
        return currentLoad + deliveries <= dailyCapacity;
    }
}
