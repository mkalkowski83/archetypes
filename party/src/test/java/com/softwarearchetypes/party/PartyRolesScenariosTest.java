package com.softwarearchetypes.party;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softwarearchetypes.common.Result;
import com.softwarearchetypes.party.commands.AddRoleCommand;
import com.softwarearchetypes.party.commands.RegisterCompanyCommand;
import com.softwarearchetypes.party.commands.RegisterOrganizationUnitCommand;
import com.softwarearchetypes.party.commands.RegisterPersonCommand;
import com.softwarearchetypes.party.commands.RemoveRoleCommand;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Scenarios for Party Roles - dynamic classification of parties. Tests role assignment, multiple
 * roles, and role management.
 */
@DisplayName("Party Roles Scenarios")
class PartyRolesScenariosTest {

    private final PartyConfiguration configuration = PartyConfiguration.inMemory();
    private final PartiesFacade partiesFacade = configuration.partiesFacade();
    private final PartiesQueries partiesQueries = configuration.partiesQueries();

    // ===== Basic role scenarios =====

    @Nested
    @DisplayName("Basic role assignment")
    class BasicRoleScenarios {

        @Test
        @DisplayName("Person can have Customer role")
        void personCanHaveCustomerRole() {
            PartyId personId =
                    partiesFacade
                            .handle(
                                    new RegisterPersonCommand(
                                            "Ewa", "Dąbrowska", Set.of(), Set.of()))
                            .getSuccess()
                            .partyId();

            Result<String, PartyId> result =
                    partiesFacade.handle(new AddRoleCommand(personId, "Customer"));

            assertTrue(result.success());
            PartyView party = partiesQueries.findBy(personId).orElseThrow();
            assertTrue(party.roles().contains("Customer"));
        }

        @Test
        @DisplayName("Company can have Supplier role")
        void companyCanHaveSupplierRole() {
            PartyId companyId =
                    partiesFacade
                            .handle(
                                    new RegisterCompanyCommand(
                                            "Industrial Parts Ltd", Set.of(), Set.of()))
                            .getSuccess()
                            .partyId();

            partiesFacade.handle(new AddRoleCommand(companyId, "Supplier"));

            PartyView company = partiesQueries.findBy(companyId).orElseThrow();
            assertTrue(company.roles().contains("Supplier"));
        }

        @Test
        @DisplayName("OrganizationUnit can have Department role")
        void organizationUnitCanHaveDepartmentRole() {
            PartyId unitId =
                    partiesFacade
                            .handle(
                                    new RegisterOrganizationUnitCommand(
                                            "IT Department", Set.of(), Set.of()))
                            .getSuccess()
                            .partyId();

            partiesFacade.handle(new AddRoleCommand(unitId, "Cost Center"));

            PartyView unit = partiesQueries.findBy(unitId).orElseThrow();
            assertTrue(unit.roles().contains("Cost Center"));
        }
    }

    // ===== Multiple roles =====

    @Nested
    @DisplayName("Multiple roles per party")
    class MultipleRolesScenarios {

        @Test
        @DisplayName("Company can have multiple roles: Supplier and Partner")
        void companyCanHaveMultipleRoles() {
            PartyId companyId =
                    partiesFacade
                            .handle(
                                    new RegisterCompanyCommand(
                                            "Tech Partners Ltd", Set.of(), Set.of()))
                            .getSuccess()
                            .partyId();

            partiesFacade.handle(new AddRoleCommand(companyId, "Supplier"));
            partiesFacade.handle(new AddRoleCommand(companyId, "Partner"));
            partiesFacade.handle(new AddRoleCommand(companyId, "Contractor"));

            PartyView company = partiesQueries.findBy(companyId).orElseThrow();
            assertTrue(company.roles().containsAll(Set.of("Supplier", "Partner", "Contractor")));
        }

        @Test
        @DisplayName("Person can be both Employee and Customer")
        void personCanBeBothEmployeeAndCustomer() {
            // Common scenario: company employee who also buys products
            PartyId personId =
                    partiesFacade
                            .handle(
                                    new RegisterPersonCommand(
                                            "Tomasz", "Lewandowski", Set.of(), Set.of()))
                            .getSuccess()
                            .partyId();

            partiesFacade.handle(new AddRoleCommand(personId, "Employee"));
            partiesFacade.handle(new AddRoleCommand(personId, "Customer"));

            PartyView party = partiesQueries.findBy(personId).orElseThrow();
            assertTrue(party.roles().containsAll(Set.of("Employee", "Customer")));
        }

        @Test
        @DisplayName("Roles can be assigned during party creation")
        void rolesCanBeAssignedDuringCreation() {
            Result<String, PartyView> result =
                    partiesFacade.handle(
                            new RegisterPersonCommand(
                                    "Katarzyna",
                                    "Wójcik",
                                    Set.of("Customer", "Newsletter Subscriber", "Beta Tester"),
                                    Set.of()));

            assertTrue(result.success());
            assertTrue(
                    result.getSuccess()
                            .roles()
                            .containsAll(
                                    Set.of("Customer", "Newsletter Subscriber", "Beta Tester")));
        }
    }

    // ===== Business role scenarios =====

    @Nested
    @DisplayName("Business role scenarios")
    class BusinessRoleScenarios {

        @Test
        @DisplayName("Banking: Person as Account Holder and Loan Applicant")
        void bankingPersonRoles() {
            PartyId personId =
                    partiesFacade
                            .handle(
                                    new RegisterPersonCommand(
                                            "Michał", "Bankowy", Set.of(), Set.of()))
                            .getSuccess()
                            .partyId();

            partiesFacade.handle(new AddRoleCommand(personId, "Account Holder"));
            partiesFacade.handle(new AddRoleCommand(personId, "Loan Applicant"));
            partiesFacade.handle(new AddRoleCommand(personId, "Credit Card Holder"));

            PartyView party = partiesQueries.findBy(personId).orElseThrow();
            assertEquals(3, party.roles().size());
        }

        @Test
        @DisplayName("Banking: Company as Corporate Client and Investment Client")
        void bankingCompanyRoles() {
            PartyId companyId =
                    partiesFacade
                            .handle(new RegisterCompanyCommand("BigCorp S.A.", Set.of(), Set.of()))
                            .getSuccess()
                            .partyId();

            partiesFacade.handle(new AddRoleCommand(companyId, "Corporate Client"));
            partiesFacade.handle(new AddRoleCommand(companyId, "Investment Client"));
            partiesFacade.handle(new AddRoleCommand(companyId, "Trade Finance Client"));

            PartyView company = partiesQueries.findBy(companyId).orElseThrow();
            assertTrue(
                    company.roles()
                            .containsAll(
                                    Set.of(
                                            "Corporate Client",
                                            "Investment Client",
                                            "Trade Finance Client")));
        }

        @Test
        @DisplayName("Telecom: Person as Subscriber and Prepaid User")
        void telecomPersonRoles() {
            PartyId personId =
                    partiesFacade
                            .handle(
                                    new RegisterPersonCommand(
                                            "Anna", "Mobilna", Set.of(), Set.of()))
                            .getSuccess()
                            .partyId();

            partiesFacade.handle(new AddRoleCommand(personId, "Subscriber"));
            partiesFacade.handle(new AddRoleCommand(personId, "Prepaid User"));
            partiesFacade.handle(new AddRoleCommand(personId, "Roaming Enabled"));

            PartyView party = partiesQueries.findBy(personId).orElseThrow();
            assertEquals(3, party.roles().size());
        }

        @Test
        @DisplayName("E-commerce: Person as Buyer and Seller")
        void ecommercePersonRoles() {
            // Marketplace scenario: person can both buy and sell
            PartyId personId =
                    partiesFacade
                            .handle(
                                    new RegisterPersonCommand(
                                            "Jan", "Handlowy", Set.of(), Set.of()))
                            .getSuccess()
                            .partyId();

            partiesFacade.handle(new AddRoleCommand(personId, "Buyer"));
            partiesFacade.handle(new AddRoleCommand(personId, "Seller"));
            partiesFacade.handle(new AddRoleCommand(personId, "Premium Seller"));

            PartyView party = partiesQueries.findBy(personId).orElseThrow();
            assertTrue(party.roles().containsAll(Set.of("Buyer", "Seller", "Premium Seller")));
        }

        @Test
        @DisplayName("Logistics: Company as Carrier and Warehouse Operator")
        void logisticsCompanyRoles() {
            PartyId companyId =
                    partiesFacade
                            .handle(
                                    new RegisterCompanyCommand(
                                            "FastLogistics Sp. z o.o.", Set.of(), Set.of()))
                            .getSuccess()
                            .partyId();

            partiesFacade.handle(new AddRoleCommand(companyId, "Carrier"));
            partiesFacade.handle(new AddRoleCommand(companyId, "Warehouse Operator"));
            partiesFacade.handle(new AddRoleCommand(companyId, "Customs Agent"));

            PartyView company = partiesQueries.findBy(companyId).orElseThrow();
            assertEquals(3, company.roles().size());
        }
    }

    // ===== Role management =====

    @Nested
    @DisplayName("Role management")
    class RoleManagementScenarios {

        @Test
        @DisplayName("Role can be removed from party")
        void roleCanBeRemovedFromParty() {
            PartyId personId =
                    partiesFacade
                            .handle(
                                    new RegisterPersonCommand(
                                            "Piotr",
                                            "Zmienny",
                                            Set.of("Customer", "VIP"),
                                            Set.of()))
                            .getSuccess()
                            .partyId();

            partiesFacade.handle(new RemoveRoleCommand(personId, "VIP"));

            PartyView party = partiesQueries.findBy(personId).orElseThrow();
            assertTrue(party.roles().contains("Customer"));
            assertFalse(party.roles().contains("VIP"));
        }

        @Test
        @DisplayName("Adding same role twice is idempotent")
        void addingSameRoleTwiceIsIdempotent() {
            PartyId personId =
                    partiesFacade
                            .handle(
                                    new RegisterPersonCommand(
                                            "Marek", "Podwójny", Set.of(), Set.of()))
                            .getSuccess()
                            .partyId();

            partiesFacade.handle(new AddRoleCommand(personId, "Customer"));
            partiesFacade.handle(new AddRoleCommand(personId, "Customer"));

            PartyView party = partiesQueries.findBy(personId).orElseThrow();
            long customerCount =
                    party.roles().stream().filter(role -> role.equals("Customer")).count();
            assertEquals(1, customerCount);
        }

        @Test
        @DisplayName("Removing non-existing role is idempotent")
        void removingNonExistingRoleIsIdempotent() {
            PartyId personId =
                    partiesFacade
                            .handle(
                                    new RegisterPersonCommand(
                                            "Zofia", "Spokojna", Set.of("Customer"), Set.of()))
                            .getSuccess()
                            .partyId();

            Result<String, PartyId> result =
                    partiesFacade.handle(new RemoveRoleCommand(personId, "NonExistingRole"));

            assertTrue(result.success()); // idempotent - no error
        }

        @Test
        @DisplayName("Adding role to non-existing party fails")
        void addingRoleToNonExistingPartyFails() {
            PartyId nonExisting = PartyId.random();

            Result<String, PartyId> result =
                    partiesFacade.handle(new AddRoleCommand(nonExisting, "Customer"));

            assertTrue(result.failure());
        }
    }

    // ===== Role evolution scenarios =====

    @Nested
    @DisplayName("Role evolution over time")
    class RoleEvolutionScenarios {

        @Test
        @DisplayName("Customer can become VIP and then Premium VIP")
        void customerRoleEvolution() {
            PartyId personId =
                    partiesFacade
                            .handle(
                                    new RegisterPersonCommand(
                                            "Klient", "Lojalny", Set.of("Customer"), Set.of()))
                            .getSuccess()
                            .partyId();

            // After some purchases, becomes VIP
            partiesFacade.handle(new AddRoleCommand(personId, "VIP"));

            // After more purchases, becomes Premium VIP
            partiesFacade.handle(new AddRoleCommand(personId, "Premium VIP"));

            PartyView party = partiesQueries.findBy(personId).orElseThrow();
            assertTrue(party.roles().containsAll(Set.of("Customer", "VIP", "Premium VIP")));
        }

        @Test
        @DisplayName("Employee becomes Manager and then Director")
        void employeePromotion() {
            PartyId personId =
                    partiesFacade
                            .handle(
                                    new RegisterPersonCommand(
                                            "Ambitny", "Pracownik", Set.of("Employee"), Set.of()))
                            .getSuccess()
                            .partyId();

            // Promotion to Manager
            partiesFacade.handle(new AddRoleCommand(personId, "Manager"));

            // Promotion to Director
            partiesFacade.handle(new AddRoleCommand(personId, "Director"));

            // Remove Manager role after becoming Director
            partiesFacade.handle(new RemoveRoleCommand(personId, "Manager"));

            PartyView party = partiesQueries.findBy(personId).orElseThrow();
            assertTrue(party.roles().contains("Employee"));
            assertTrue(party.roles().contains("Director"));
            assertFalse(party.roles().contains("Manager"));
        }
    }
}
