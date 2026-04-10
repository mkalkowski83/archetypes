package com.softwarearchetypes.planvsexecution.badimplementation.deliveryscheduling;

/**
 * Customer entity with SLA information. Part of the problem: delivery plan is reconstructed from
 * multiple entities like this one.
 */
public class Customer {
    private Long id;
    private String name;
    private int slaDeliveryDays; // How many days for delivery as per SLA
    private String region;

    public Customer(Long id, String name, int slaDeliveryDays, String region) {
        this.id = id;
        this.name = name;
        this.slaDeliveryDays = slaDeliveryDays;
        this.region = region;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getSlaDeliveryDays() {
        return slaDeliveryDays;
    }

    public void setSlaDeliveryDays(int slaDeliveryDays) {
        this.slaDeliveryDays = slaDeliveryDays;
    }

    public String getRegion() {
        return region;
    }
}
