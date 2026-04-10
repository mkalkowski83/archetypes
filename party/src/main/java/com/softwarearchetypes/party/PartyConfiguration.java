package com.softwarearchetypes.party;

import com.softwarearchetypes.party.events.InMemoryEventsPublisher;

class PartyConfiguration {

    private final PartiesFacade partiesFacade;
    private final PartyRelationshipsFacade partyRelationshipsFacade;
    private final AddressesFacade addressesFacade;
    private final CapabilitiesFacade capabilitiesFacade;
    private final PartiesQueries partiesQueries;
    private final PartyRelationshipsQueries partyRelationshipsQueries;
    private final AddressesQueries addressesQueries;
    private final CapabilitiesQueries capabilitiesQueries;
    private final InMemoryEventsPublisher eventPublisher;

    PartyConfiguration(
            PartiesFacade partiesFacade,
            PartyRelationshipsFacade partyRelationshipsFacade,
            AddressesFacade addressesFacade,
            CapabilitiesFacade capabilitiesFacade,
            PartiesQueries partiesQueries,
            PartyRelationshipsQueries partyRelationshipsQueries,
            AddressesQueries addressesQueries,
            CapabilitiesQueries capabilitiesQueries,
            InMemoryEventsPublisher eventPublisher) {
        this.partiesFacade = partiesFacade;
        this.partyRelationshipsFacade = partyRelationshipsFacade;
        this.addressesFacade = addressesFacade;
        this.capabilitiesFacade = capabilitiesFacade;
        this.partiesQueries = partiesQueries;
        this.partyRelationshipsQueries = partyRelationshipsQueries;
        this.addressesQueries = addressesQueries;
        this.capabilitiesQueries = capabilitiesQueries;
        this.eventPublisher = eventPublisher;
    }

    public static PartyConfiguration inMemory() {
        InMemoryEventsPublisher eventPublisher = new InMemoryEventsPublisher();
        InMemoryPartyRepository partyRepository = new InMemoryPartyRepository();
        InMemoryPartyRelationshipRepository partyRelationshipRepository =
                new InMemoryPartyRelationshipRepository();
        InMemoryAddressesRepository addressesRepository = new InMemoryAddressesRepository();
        InMemoryCapabilitiesRepository capabilitiesRepository =
                new InMemoryCapabilitiesRepository();

        PartiesFacade partiesFacade =
                new PartiesFacade(partyRepository, eventPublisher, PartyId::random);

        PartyRoleFactory partyRoleFactory = new PartyRoleFactory();
        PartyRelationshipFactory partyRelationshipFactory =
                new PartyRelationshipFactory(PartyRelationshipId::random);
        PartyRelationshipsFacade partyRelationshipsFacade =
                new PartyRelationshipsFacade(
                        partyRoleFactory,
                        partyRelationshipFactory,
                        partyRelationshipRepository,
                        partyRepository,
                        eventPublisher);

        AddressesFacade addressesFacade = new AddressesFacade(addressesRepository, eventPublisher);

        PartiesQueries partiesQueries = new PartiesQueries(partyRepository);
        PartyRelationshipsQueries partyRelationshipsQueries =
                new PartyRelationshipsQueries(partyRelationshipRepository);
        AddressesQueries addressesQueries = new AddressesQueries(addressesRepository);
        CapabilitiesQueries capabilitiesQueries = new CapabilitiesQueries(capabilitiesRepository);

        CapabilitiesFacade capabilitiesFacade =
                new CapabilitiesFacade(capabilitiesRepository, partiesQueries);

        return new PartyConfiguration(
                partiesFacade,
                partyRelationshipsFacade,
                addressesFacade,
                capabilitiesFacade,
                partiesQueries,
                partyRelationshipsQueries,
                addressesQueries,
                capabilitiesQueries,
                eventPublisher);
    }

    public PartiesFacade partiesFacade() {
        return partiesFacade;
    }

    public PartyRelationshipsFacade partyRelationshipsFacade() {
        return partyRelationshipsFacade;
    }

    public AddressesFacade addressesFacade() {
        return addressesFacade;
    }

    public PartiesQueries partiesQueries() {
        return partiesQueries;
    }

    public PartyRelationshipsQueries partyRelationshipsQueries() {
        return partyRelationshipsQueries;
    }

    public AddressesQueries addressesQueries() {
        return addressesQueries;
    }

    public InMemoryEventsPublisher eventPublisher() {
        return eventPublisher;
    }

    public CapabilitiesFacade capabilitiesFacade() {
        return capabilitiesFacade;
    }

    public CapabilitiesQueries capabilitiesQueries() {
        return capabilitiesQueries;
    }
}
