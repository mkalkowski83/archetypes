package com.softwarearchetypes.inventory.reservation;

import com.softwarearchetypes.inventory.InventoryConfiguration;
import com.softwarearchetypes.inventory.InventoryFacade;
import com.softwarearchetypes.inventory.availability.AvailabilityConfiguration;
import com.softwarearchetypes.inventory.availability.AvailabilityFacade;
import java.time.Clock;

public class ReservationConfiguration {

    private final Clock clock;
    private final InventoryFacade inventoryFacade;
    private final AvailabilityFacade availabilityFacade;
    private final ReservationRepository reservationRepository;
    private final ReservationFacade facade;

    ReservationConfiguration(
            Clock clock,
            InventoryFacade inventoryFacade,
            AvailabilityFacade availabilityFacade,
            ReservationRepository reservationRepository,
            ReservationFacade facade) {
        this.clock = clock;
        this.inventoryFacade = inventoryFacade;
        this.availabilityFacade = availabilityFacade;
        this.reservationRepository = reservationRepository;
        this.facade = facade;
    }

    public static ReservationConfiguration inMemory() {
        return inMemory(Clock.systemUTC());
    }

    public static ReservationConfiguration inMemory(Clock clock) {
        AvailabilityConfiguration availabilityConfig = AvailabilityConfiguration.inMemory(clock);
        InventoryConfiguration inventoryConfig =
                InventoryConfiguration.inMemory(availabilityConfig);
        return inMemory(inventoryConfig, availabilityConfig, clock);
    }

    public static ReservationConfiguration inMemory(
            InventoryConfiguration inventoryConfig,
            AvailabilityConfiguration availabilityConfig,
            Clock clock) {
        ReservationRepository reservationRepository = new InMemoryReservationRepository();
        ReservationFacade facade =
                new ReservationFacade(
                        inventoryConfig.facade(),
                        availabilityConfig.facade(),
                        reservationRepository,
                        clock);
        return new ReservationConfiguration(
                clock,
                inventoryConfig.facade(),
                availabilityConfig.facade(),
                reservationRepository,
                facade);
    }

    public ReservationFacade facade() {
        return facade;
    }

    public InventoryFacade inventoryFacade() {
        return inventoryFacade;
    }

    public AvailabilityFacade availabilityFacade() {
        return availabilityFacade;
    }

    public Clock clock() {
        return clock;
    }
}
