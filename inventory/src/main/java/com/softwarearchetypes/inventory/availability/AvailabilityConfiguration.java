package com.softwarearchetypes.inventory.availability;

import java.time.Clock;

public class AvailabilityConfiguration {

    private final Clock clock;
    private final ResourceAvailabilityRepository repository;
    private final AvailabilityFacade facade;

    AvailabilityConfiguration(
            Clock clock, ResourceAvailabilityRepository repository, AvailabilityFacade facade) {
        this.clock = clock;
        this.repository = repository;
        this.facade = facade;
    }

    public static AvailabilityConfiguration inMemory() {
        return inMemory(Clock.systemUTC());
    }

    public static AvailabilityConfiguration inMemory(Clock clock) {
        ResourceAvailabilityRepository repository = new InMemoryResourceAvailabilityRepository();
        AvailabilityFacade facade = new AvailabilityFacade(repository, clock);
        return new AvailabilityConfiguration(clock, repository, facade);
    }

    public AvailabilityFacade facade() {
        return facade;
    }

    public Clock clock() {
        return clock;
    }
}
