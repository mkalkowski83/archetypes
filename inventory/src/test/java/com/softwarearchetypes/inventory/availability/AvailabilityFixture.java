package com.softwarearchetypes.inventory.availability;

import java.time.Clock;

/**
 * Test fixture for creating availability resources. Provides access to package-private classes for
 * tests in other packages.
 */
public class AvailabilityFixture {

    private final AvailabilityFacade facade;
    private final Clock clock;

    public AvailabilityFixture(AvailabilityFacade facade, Clock clock) {
        this.facade = facade;
        this.clock = clock;
    }

    public ResourceAvailabilityId registerTemporalSlot(ResourceId resourceId, TimeSlot slot) {
        TemporalResourceAvailability availability =
                TemporalResourceAvailability.create(resourceId, slot, clock);
        return facade.register(availability).getSuccess();
    }

    public ResourceAvailabilityId registerIndividual(ResourceId resourceId) {
        IndividualResourceAvailability availability =
                IndividualResourceAvailability.create(resourceId, clock);
        return facade.register(availability).getSuccess();
    }

    public ResourceAvailabilityId registerPool(
            ResourceId resourceId, com.softwarearchetypes.quantity.Quantity capacity) {
        PoolResourceAvailability availability =
                PoolResourceAvailability.create(resourceId, capacity, clock);
        return facade.register(availability).getSuccess();
    }
}
