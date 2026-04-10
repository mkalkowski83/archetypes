package com.softwarearchetypes.party;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softwarearchetypes.common.Result;
import com.softwarearchetypes.party.commands.RegisterCompanyCommand;
import com.softwarearchetypes.party.commands.RegisterOrganizationUnitCommand;
import com.softwarearchetypes.party.commands.RegisterPersonCommand;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Scenarios for Party registration covering Person, Company, and OrganizationUnit. Tests the
 * unified Party abstraction and PartyId assignment.
 */
@DisplayName("Party Registration Scenarios")
class PartyRegistrationScenariosTest {

    private final PartyConfiguration configuration = PartyConfiguration.inMemory();
    private final PartiesFacade partiesFacade = configuration.partiesFacade();
    private final PartiesQueries partiesQueries = configuration.partiesQueries();

    // ===== Person Registration =====

    @Test
    @DisplayName("Person can be registered with first and last name")
    void personCanBeRegisteredWithPersonalData() {
        Result<String, PartyView> result =
                partiesFacade.handle(
                        new RegisterPersonCommand("Jan", "Kowalski", Set.of(), Set.of()));

        assertTrue(result.success());
        assertEquals("PERSON", result.getSuccess().partyType());
        PersonView person = (PersonView) result.getSuccess();
        assertEquals("Jan", person.firstName());
        assertEquals("Kowalski", person.lastName());
    }

    @Test
    @DisplayName("Person registration assigns unique PartyId")
    void personRegistrationAssignsUniquePartyId() {
        PartyView person1 =
                partiesFacade
                        .handle(new RegisterPersonCommand("Anna", "Nowak", Set.of(), Set.of()))
                        .getSuccess();
        PartyView person2 =
                partiesFacade
                        .handle(new RegisterPersonCommand("Anna", "Nowak", Set.of(), Set.of()))
                        .getSuccess();

        assertNotNull(person1.partyId());
        assertNotNull(person2.partyId());
        assertFalse(
                person1.partyId().equals(person2.partyId()),
                "Two persons with same name should get different PartyIds");
    }

    // ===== Company Registration =====

    @Test
    @DisplayName("Company can be registered with organization name")
    void companyCanBeRegisteredWithOrganizationName() {
        Result<String, PartyView> result =
                partiesFacade.handle(
                        new RegisterCompanyCommand("ABC Sp. z o.o.", Set.of(), Set.of()));

        assertTrue(result.success());
        assertEquals("COMPANY", result.getSuccess().partyType());
        CompanyView company = (CompanyView) result.getSuccess();
        assertEquals("ABC Sp. z o.o.", company.organizationName());
    }

    @Test
    @DisplayName("Company gets unique PartyId different from Person")
    void companyGetsUniquePartyIdDifferentFromPerson() {
        PartyView person =
                partiesFacade
                        .handle(new RegisterPersonCommand("Jan", "Kowalski", Set.of(), Set.of()))
                        .getSuccess();
        PartyView company =
                partiesFacade
                        .handle(
                                new RegisterCompanyCommand(
                                        "Kowalski Sp. z o.o.", Set.of(), Set.of()))
                        .getSuccess();

        assertFalse(
                person.partyId().equals(company.partyId()),
                "Person and Company should have different PartyIds");
    }

    // ===== OrganizationUnit Registration =====

    @Test
    @DisplayName("Organization unit can be registered")
    void organizationUnitCanBeRegistered() {
        Result<String, PartyView> result =
                partiesFacade.handle(
                        new RegisterOrganizationUnitCommand(
                                "Marketing Department", Set.of(), Set.of()));

        assertTrue(result.success());
        assertEquals("ORGANIZATION_UNIT", result.getSuccess().partyType());
    }

    @Test
    @DisplayName("Multiple departments can be registered independently")
    void multipleDepartmentsCanBeRegistered() {
        PartyView hr =
                partiesFacade
                        .handle(
                                new RegisterOrganizationUnitCommand(
                                        "HR Department", Set.of(), Set.of()))
                        .getSuccess();
        PartyView it =
                partiesFacade
                        .handle(
                                new RegisterOrganizationUnitCommand(
                                        "IT Department", Set.of(), Set.of()))
                        .getSuccess();
        PartyView sales =
                partiesFacade
                        .handle(
                                new RegisterOrganizationUnitCommand(
                                        "Sales Department", Set.of(), Set.of()))
                        .getSuccess();

        assertFalse(hr.partyId().equals(it.partyId()));
        assertFalse(it.partyId().equals(sales.partyId()));
    }

    // ===== PartyId and Queries =====

    @Test
    @DisplayName("Party can be found by PartyId")
    void partyCanBeFoundByPartyId() {
        PartyView created =
                partiesFacade
                        .handle(
                                new RegisterPersonCommand(
                                        "Piotr", "Wiśniewski", Set.of(), Set.of()))
                        .getSuccess();

        var found = partiesQueries.findBy(created.partyId());

        assertTrue(found.isPresent());
        assertEquals(created.partyId(), found.get().partyId());
    }

    @Test
    @DisplayName("Non-existing PartyId returns empty result")
    void nonExistingPartyIdReturnsEmpty() {
        PartyId nonExisting = PartyId.random();

        var found = partiesQueries.findBy(nonExisting);

        assertTrue(found.isEmpty());
    }

    // ===== Registration with initial data =====

    @Test
    @DisplayName("Person can be registered with roles and identifiers")
    void personCanBeRegisteredWithRolesAndIdentifiers() {
        PersonalIdentificationNumber pesel = PersonalIdentificationNumber.of("44051401458");

        PartyView person =
                partiesFacade
                        .handle(
                                new RegisterPersonCommand(
                                        "Krzysztof",
                                        "Mazur",
                                        Set.of("Customer", "Premium Member"),
                                        Set.of(pesel)))
                        .getSuccess();

        assertTrue(person.roles().containsAll(Set.of("Customer", "Premium Member")));
        assertTrue(person.registeredIdentifiers().contains(pesel));
    }

    @Test
    @DisplayName("Company can be registered with roles and NIP")
    void companyCanBeRegisteredWithRolesAndNip() {
        TaxNumber nip = TaxNumber.of("1234563218");

        PartyView company =
                partiesFacade
                        .handle(
                                new RegisterCompanyCommand(
                                        "Software House Sp. z o.o.",
                                        Set.of("Supplier", "Contractor"),
                                        Set.of(nip)))
                        .getSuccess();

        assertTrue(company.roles().containsAll(Set.of("Supplier", "Contractor")));
        assertTrue(company.registeredIdentifiers().contains(nip));
    }
}
