package com.softwarearchetypes.inventory;

import com.softwarearchetypes.inventory.availability.AvailabilityConfiguration;
import com.softwarearchetypes.inventory.availability.AvailabilityFacade;

public class InventoryConfiguration {

    private final InventoryEntryRepository entryRepository;
    private final InstanceRepository instanceRepository;
    private final ProductDefinitionValidator productValidator;
    private final AvailabilityFacade availabilityFacade;
    private final InventoryFacade facade;

    InventoryConfiguration(
            InventoryEntryRepository entryRepository,
            InstanceRepository instanceRepository,
            ProductDefinitionValidator productValidator,
            AvailabilityFacade availabilityFacade,
            InventoryFacade facade) {
        this.entryRepository = entryRepository;
        this.instanceRepository = instanceRepository;
        this.productValidator = productValidator;
        this.availabilityFacade = availabilityFacade;
        this.facade = facade;
    }

    public static InventoryConfiguration inMemory() {
        AvailabilityConfiguration availabilityConfig = AvailabilityConfiguration.inMemory();
        return inMemory(availabilityConfig);
    }

    public static InventoryConfiguration inMemory(AvailabilityConfiguration availabilityConfig) {
        return inMemory(availabilityConfig, ProductDefinitionValidator.alwaysValid());
    }

    public static InventoryConfiguration inMemory(
            AvailabilityConfiguration availabilityConfig,
            ProductDefinitionValidator productValidator) {
        AvailabilityFacade availabilityFacade = availabilityConfig.facade();
        InventoryEntryRepository entryRepository = new InMemoryInventoryEntryRepository();
        InstanceRepository instanceRepository = new InMemoryInstanceRepository();
        InventoryFacade facade =
                new InventoryFacade(
                        entryRepository, instanceRepository, productValidator, availabilityFacade);
        return new InventoryConfiguration(
                entryRepository, instanceRepository, productValidator, availabilityFacade, facade);
    }

    public InventoryFacade facade() {
        return facade;
    }

    public AvailabilityFacade availabilityFacade() {
        return availabilityFacade;
    }
}
