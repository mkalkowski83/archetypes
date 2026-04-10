package com.softwarearchetypes.party;

import static com.softwarearchetypes.party.OrganizationNameFixture.someOrganizationName;
import static com.softwarearchetypes.party.PersonalDataFixture.somePersonalData;
import static com.softwarearchetypes.party.RegisteredIdentifierFixture.someRegisteredIdentifier;
import static com.softwarearchetypes.party.RoleFixture.someRole;
import static com.softwarearchetypes.party.RoleFixture.someRoleSetOfSize;
import static com.softwarearchetypes.party.RoleFixture.stringSetFrom;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softwarearchetypes.common.Result;
import com.softwarearchetypes.party.commands.AddRegisteredIdentifierCommand;
import com.softwarearchetypes.party.commands.AddRoleCommand;
import com.softwarearchetypes.party.commands.RegisterCompanyCommand;
import com.softwarearchetypes.party.commands.RegisterOrganizationUnitCommand;
import com.softwarearchetypes.party.commands.RegisterPersonCommand;
import com.softwarearchetypes.party.commands.RemoveRegisteredIdentifierCommand;
import com.softwarearchetypes.party.commands.RemoveRoleCommand;
import com.softwarearchetypes.party.commands.UpdateOrganizationNameCommand;
import com.softwarearchetypes.party.commands.UpdatePersonalDataCommand;
import com.softwarearchetypes.party.events.CompanyRegistered;
import com.softwarearchetypes.party.events.OrganizationUnitRegistered;
import com.softwarearchetypes.party.events.PersonRegistered;
import com.softwarearchetypes.party.events.PersonalDataUpdated;
import com.softwarearchetypes.party.events.RegisteredIdentifierAdded;
import com.softwarearchetypes.party.events.RegisteredIdentifierRemoved;
import com.softwarearchetypes.party.events.RoleAdded;
import com.softwarearchetypes.party.events.RoleRemoved;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PartiesFacadeTest {

    private final PartyConfiguration configuration = PartyConfiguration.inMemory();
    private final PartiesFacade partiesFacade = configuration.partiesFacade();
    private final PartiesQueries partiesQueries = configuration.partiesQueries();
    private final PartiesTestEventListener testEventListener =
            new PartiesTestEventListener(configuration.eventPublisher());

    @Test
    void canRegisterPerson() {
        // given
        PersonalData personalData = somePersonalData();
        Set<Role> roles = someRoleSetOfSize(3);
        RegisteredIdentifier identifier = someRegisteredIdentifier();

        // when
        Result<String, PartyView> result =
                partiesFacade.handle(
                        new RegisterPersonCommand(
                                personalData.firstName(),
                                personalData.lastName(),
                                stringSetFrom(roles),
                                Set.of(identifier)));

        // then
        assertTrue(result.success());
        assertEquals("PERSON", result.getSuccess().partyType());
    }

    @Test
    void personRegisteredEventIsEmittedWhenOperationSucceeds() {
        // given
        PersonalData personalData = somePersonalData();
        Set<Role> roles = someRoleSetOfSize(3);
        RegisteredIdentifier identifier = someRegisteredIdentifier();

        // when
        Result<String, PartyView> result =
                partiesFacade.handle(
                        new RegisterPersonCommand(
                                personalData.firstName(),
                                personalData.lastName(),
                                stringSetFrom(roles),
                                Set.of(identifier)));

        // then
        String partyId = result.getSuccess().partyId().asString();
        PersonRegistered expectedEvent =
                new PersonRegistered(
                        partyId,
                        personalData.firstName(),
                        personalData.lastName(),
                        Set.of(identifier.asString()),
                        stringSetFrom(roles));
        assertTrue(testEventListener.thereIsAnEventEqualTo(expectedEvent));
    }

    @Test
    void canRegisterCompany() {
        // given
        OrganizationName organizationName = someOrganizationName();
        Set<Role> roles = someRoleSetOfSize(3);
        RegisteredIdentifier identifier = someRegisteredIdentifier();

        // when
        Result<String, PartyView> result =
                partiesFacade.handle(
                        new RegisterCompanyCommand(
                                organizationName.value(),
                                stringSetFrom(roles),
                                Set.of(identifier)));

        // then
        assertTrue(result.success());
        assertEquals("COMPANY", result.getSuccess().partyType());
    }

    @Test
    void companyRegisteredEventIsEmittedWhenOperationSucceeds() {
        // given
        OrganizationName organizationName = someOrganizationName();
        Set<Role> roles = someRoleSetOfSize(3);
        RegisteredIdentifier identifier = someRegisteredIdentifier();

        // when
        Result<String, PartyView> result =
                partiesFacade.handle(
                        new RegisterCompanyCommand(
                                organizationName.value(),
                                stringSetFrom(roles),
                                Set.of(identifier)));

        // then
        String partyId = result.getSuccess().partyId().asString();
        CompanyRegistered expectedEvent =
                new CompanyRegistered(
                        partyId,
                        organizationName.value(),
                        Set.of(identifier.asString()),
                        stringSetFrom(roles));
        assertTrue(testEventListener.thereIsAnEventEqualTo(expectedEvent));
    }

    @Test
    void canRegisterOrganizationUnit() {
        // given
        OrganizationName organizationName = someOrganizationName();
        Set<Role> roles = someRoleSetOfSize(3);
        RegisteredIdentifier identifier = someRegisteredIdentifier();

        // when
        Result<String, PartyView> result =
                partiesFacade.handle(
                        new RegisterOrganizationUnitCommand(
                                organizationName.value(),
                                stringSetFrom(roles),
                                Set.of(identifier)));

        // then
        assertTrue(result.success());
        assertEquals("ORGANIZATION_UNIT", result.getSuccess().partyType());
    }

    @Test
    void organizationUnitRegisteredEventIsEmittedWhenOperationSucceeds() {
        // given
        OrganizationName organizationName = someOrganizationName();
        Set<Role> roles = someRoleSetOfSize(3);
        RegisteredIdentifier identifier = someRegisteredIdentifier();

        // when
        Result<String, PartyView> result =
                partiesFacade.handle(
                        new RegisterOrganizationUnitCommand(
                                organizationName.value(),
                                stringSetFrom(roles),
                                Set.of(identifier)));

        // then
        String partyId = result.getSuccess().partyId().asString();
        OrganizationUnitRegistered expectedEvent =
                new OrganizationUnitRegistered(
                        partyId,
                        organizationName.value(),
                        Set.of(identifier.asString()),
                        stringSetFrom(roles));
        assertTrue(testEventListener.thereIsAnEventEqualTo(expectedEvent));
    }

    @Test
    void canAddRoleToParty() {
        // given
        PartyId partyId = registerSomePerson();
        Role newRole = someRole();

        // when
        Result<String, PartyId> result =
                partiesFacade.handle(new AddRoleCommand(partyId, newRole.name()));

        // then
        assertTrue(result.success());
        PartyView updatedParty = partiesQueries.findBy(partyId).orElseThrow();
        assertTrue(updatedParty.roles().contains(newRole.name()));
    }

    @Test
    void roleAddedEventIsEmittedWhenOperationSucceeds() {
        // given
        PartyId partyId = registerSomePerson();
        Role newRole = someRole();

        // when
        partiesFacade.handle(new AddRoleCommand(partyId, newRole.name()));

        // then
        RoleAdded expectedEvent = new RoleAdded(partyId.asString(), newRole.asString());
        assertTrue(testEventListener.thereIsAnEventEqualTo(expectedEvent));
    }

    @Test
    void canRemoveRoleFromParty() {
        // given
        Role role = someRole();
        PartyId partyId = registerSomePerson();
        partiesFacade.handle(new AddRoleCommand(partyId, role.name()));

        // when
        Result<String, PartyId> result =
                partiesFacade.handle(new RemoveRoleCommand(partyId, role.name()));

        // then
        assertTrue(result.success());
    }

    @Test
    void roleRemovedEventIsEmittedWhenOperationSucceeds() {
        // given
        Role role = someRole();
        PartyId partyId = registerSomePerson();
        partiesFacade.handle(new AddRoleCommand(partyId, role.name()));

        // when
        partiesFacade.handle(new RemoveRoleCommand(partyId, role.name()));

        // then
        RoleRemoved expectedEvent = new RoleRemoved(partyId.asString(), role.asString());
        assertTrue(testEventListener.thereIsAnEventEqualTo(expectedEvent));
    }

    @Test
    void canAddRegisteredIdentifierToParty() {
        // given
        PartyId partyId = registerSomePerson();
        RegisteredIdentifier newIdentifier = someRegisteredIdentifier();

        // when
        Result<String, PartyId> result =
                partiesFacade.handle(new AddRegisteredIdentifierCommand(partyId, newIdentifier));

        // then
        assertTrue(result.success());
        PartyView updatedParty = partiesQueries.findBy(partyId).orElseThrow();
        assertTrue(updatedParty.registeredIdentifiers().contains(newIdentifier));
    }

    @Test
    void registeredIdentifierAddedEventIsEmittedWhenOperationSucceeds() {
        // given
        PartyId partyId = registerSomePerson();
        RegisteredIdentifier newIdentifier = someRegisteredIdentifier();

        // when
        partiesFacade.handle(new AddRegisteredIdentifierCommand(partyId, newIdentifier));

        // then
        RegisteredIdentifierAdded expectedEvent =
                new RegisteredIdentifierAdded(
                        partyId.asString(), newIdentifier.type(), newIdentifier.asString());
        assertTrue(testEventListener.thereIsAnEventEqualTo(expectedEvent));
    }

    @Test
    void canRemoveRegisteredIdentifierFromParty() {
        // given
        RegisteredIdentifier identifier = someRegisteredIdentifier();
        PartyId partyId = registerSomePerson();
        partiesFacade.handle(new AddRegisteredIdentifierCommand(partyId, identifier));

        // when
        Result<String, PartyId> result =
                partiesFacade.handle(new RemoveRegisteredIdentifierCommand(partyId, identifier));

        // then
        assertTrue(result.success());
    }

    @Test
    void registeredIdentifierRemovedEventIsEmittedWhenOperationSucceeds() {
        // given
        RegisteredIdentifier identifier = someRegisteredIdentifier();
        PartyId partyId = registerSomePerson();
        partiesFacade.handle(new AddRegisteredIdentifierCommand(partyId, identifier));

        // when
        partiesFacade.handle(new RemoveRegisteredIdentifierCommand(partyId, identifier));

        // then
        RegisteredIdentifierRemoved expectedEvent =
                new RegisteredIdentifierRemoved(
                        partyId.asString(), identifier.type(), identifier.asString());
        assertTrue(testEventListener.thereIsAnEventEqualTo(expectedEvent));
    }

    @Test
    void canUpdatePersonalDataOfExistingPerson() {
        // given
        PartyId partyId = registerSomePerson();
        PersonalData newPersonalData = somePersonalData();

        // when
        Result<String, PartyView> result =
                partiesFacade.handle(
                        new UpdatePersonalDataCommand(
                                partyId, newPersonalData.firstName(), newPersonalData.lastName()));

        // then
        assertTrue(result.success());
    }

    @Test
    void personalDataUpdatedEventIsEmittedWhenOperationSucceeds() {
        // given
        PartyId partyId = registerSomePerson();
        PersonalData newPersonalData = somePersonalData();

        // when
        partiesFacade.handle(
                new UpdatePersonalDataCommand(
                        partyId, newPersonalData.firstName(), newPersonalData.lastName()));

        // then
        PersonalDataUpdated expectedEvent =
                new PersonalDataUpdated(
                        partyId.asString(),
                        newPersonalData.firstName(),
                        newPersonalData.lastName());
        assertTrue(testEventListener.thereIsAnEventEqualTo(expectedEvent));
    }

    @Test
    void cannotUpdatePersonalDataOfOrganization() {
        // given
        PartyId partyId = registerSomeCompany();
        PersonalData newPersonalData = somePersonalData();

        // when
        Result<String, PartyView> result =
                partiesFacade.handle(
                        new UpdatePersonalDataCommand(
                                partyId, newPersonalData.firstName(), newPersonalData.lastName()));

        // then
        assertTrue(result.failure());
    }

    @Test
    void cannotUpdateOrganizationNameOfPerson() {
        // given
        PartyId partyId = registerSomePerson();
        OrganizationName newName = someOrganizationName();

        // when
        Result<String, PartyView> result =
                partiesFacade.handle(new UpdateOrganizationNameCommand(partyId, newName.value()));

        // then
        assertTrue(result.failure());
    }

    @Test
    void addRoleShouldFailWhenPartyDoesNotExist() {
        // given
        PartyId nonExistingPartyId = PartyId.random();
        Role role = someRole();

        // when
        Result<String, PartyId> result =
                partiesFacade.handle(new AddRoleCommand(nonExistingPartyId, role.name()));

        // then
        assertTrue(result.failure());
    }

    private PartyId registerSomePerson() {
        PersonalData personalData = somePersonalData();
        Result<String, PartyView> result =
                partiesFacade.handle(
                        new RegisterPersonCommand(
                                personalData.firstName(),
                                personalData.lastName(),
                                Set.of(),
                                Set.of()));
        return result.getSuccess().partyId();
    }

    private PartyId registerSomeCompany() {
        OrganizationName name = someOrganizationName();
        Result<String, PartyView> result =
                partiesFacade.handle(new RegisterCompanyCommand(name.value(), Set.of(), Set.of()));
        return result.getSuccess().partyId();
    }
}
