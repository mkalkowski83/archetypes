package com.softwarearchetypes.party;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softwarearchetypes.common.Result;
import com.softwarearchetypes.party.commands.AssignPartyRelationshipCommand;
import com.softwarearchetypes.party.commands.RegisterCompanyCommand;
import com.softwarearchetypes.party.commands.RegisterOrganizationUnitCommand;
import com.softwarearchetypes.party.commands.RegisterPersonCommand;
import com.softwarearchetypes.party.commands.RemovePartyRelationshipCommand;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Scenarios for PartyRelationships - connections between parties. Tests employment, account
 * holding, subscriptions, partnerships, and organization structure.
 */
@DisplayName("Party Relationships Scenarios")
class PartyRelationshipsScenariosTest {

    private final PartyConfiguration configuration = PartyConfiguration.inMemory();
    private final PartiesFacade partiesFacade = configuration.partiesFacade();
    private final PartyRelationshipsFacade relationshipsFacade =
            configuration.partyRelationshipsFacade();
    private final PartyRelationshipsQueries relationshipsQueries =
            configuration.partyRelationshipsQueries();

    // ===== Employment relationships =====

    @Nested
    @DisplayName("Employment relationships")
    class EmploymentScenarios {

        @Test
        @DisplayName("Person can be employed by Company")
        void personCanBeEmployedByCompany() {
            PartyId employee = registerPerson("Jan", "Pracownik");
            PartyId employer = registerCompany("BigCorp S.A.");

            Result<String, PartyRelationshipView> result =
                    relationshipsFacade.handle(
                            new AssignPartyRelationshipCommand(
                                    employee, "Employee", employer, "Employer", "Employment"));

            assertTrue(result.success());
            assertEquals("Employment", result.getSuccess().relationshipName());
            assertEquals(employee, result.getSuccess().fromPartyId());
            assertEquals(employer, result.getSuccess().toPartyId());
        }

        @Test
        @DisplayName("Multiple employees can work for same company")
        void multipleEmployeesCanWorkForSameCompany() {
            PartyId employee1 = registerPerson("Anna", "Pierwsza");
            PartyId employee2 = registerPerson("Marek", "Drugi");
            PartyId employee3 = registerPerson("Ewa", "Trzecia");
            PartyId employer = registerCompany("MegaCorp Sp. z o.o.");

            PartyRelationshipView rel1 = createEmployment(employee1, employer);
            PartyRelationshipView rel2 = createEmployment(employee2, employer);
            PartyRelationshipView rel3 = createEmployment(employee3, employer);

            assertNotNull(rel1.id());
            assertNotNull(rel2.id());
            assertNotNull(rel3.id());
        }

        @Test
        @DisplayName("Person can have employment relationships with multiple companies")
        void personCanHaveMultipleEmployments() {
            // Scenario: consultant working for multiple clients
            PartyId consultant = registerPerson("Paweł", "Konsultant");
            PartyId client1 = registerCompany("Client A S.A.");
            PartyId client2 = registerCompany("Client B Sp. z o.o.");

            PartyRelationshipView rel1 =
                    relationshipsFacade
                            .handle(
                                    new AssignPartyRelationshipCommand(
                                            consultant,
                                            "Consultant",
                                            client1,
                                            "Client",
                                            "Consulting Agreement"))
                            .getSuccess();

            PartyRelationshipView rel2 =
                    relationshipsFacade
                            .handle(
                                    new AssignPartyRelationshipCommand(
                                            consultant,
                                            "Consultant",
                                            client2,
                                            "Client",
                                            "Consulting Agreement"))
                            .getSuccess();

            assertNotNull(rel1.id());
            assertNotNull(rel2.id());
        }
    }

    // ===== Banking relationships =====

    @Nested
    @DisplayName("Banking relationships")
    class BankingScenarios {

        @Test
        @DisplayName("Person holds account at Bank")
        void personHoldsAccountAtBank() {
            PartyId customer = registerPerson("Klient", "Bankowy");
            PartyId bank = registerCompany("MójBank S.A.");

            Result<String, PartyRelationshipView> result =
                    relationshipsFacade.handle(
                            new AssignPartyRelationshipCommand(
                                    customer,
                                    "Account Holder",
                                    bank,
                                    "Account Provider",
                                    "Account Holding"));

            assertTrue(result.success());
            assertEquals("Account Holding", result.getSuccess().relationshipName());
        }

        @Test
        @DisplayName("Company holds corporate account at Bank")
        void companyHoldsCorporateAccount() {
            PartyId company = registerCompany("TechStartup Sp. z o.o.");
            PartyId bank = registerCompany("CorporateBank S.A.");

            Result<String, PartyRelationshipView> result =
                    relationshipsFacade.handle(
                            new AssignPartyRelationshipCommand(
                                    company,
                                    "Corporate Account Holder",
                                    bank,
                                    "Corporate Banking Provider",
                                    "Corporate Account Agreement"));

            assertTrue(result.success());
        }

        @Test
        @DisplayName("Person has loan relationship with Bank")
        void personHasLoanWithBank() {
            PartyId borrower = registerPerson("Kredytobiorca", "Nowak");
            PartyId lender = registerCompany("LoanBank S.A.");

            Result<String, PartyRelationshipView> result =
                    relationshipsFacade.handle(
                            new AssignPartyRelationshipCommand(
                                    borrower, "Borrower", lender, "Lender", "Mortgage Loan"));

            assertTrue(result.success());
            assertEquals("Mortgage Loan", result.getSuccess().relationshipName());
        }

        @Test
        @DisplayName("Customer can have multiple products with same bank")
        void customerCanHaveMultipleProductsWithSameBank() {
            PartyId customer = registerPerson("Wieloproduktowy", "Klient");
            PartyId bank = registerCompany("UniversalBank S.A.");

            PartyRelationshipView account =
                    relationshipsFacade
                            .handle(
                                    new AssignPartyRelationshipCommand(
                                            customer,
                                            "Account Holder",
                                            bank,
                                            "Account Provider",
                                            "Savings Account"))
                            .getSuccess();

            PartyRelationshipView card =
                    relationshipsFacade
                            .handle(
                                    new AssignPartyRelationshipCommand(
                                            customer,
                                            "Card Holder",
                                            bank,
                                            "Card Issuer",
                                            "Credit Card"))
                            .getSuccess();

            PartyRelationshipView loan =
                    relationshipsFacade
                            .handle(
                                    new AssignPartyRelationshipCommand(
                                            customer, "Borrower", bank, "Lender", "Personal Loan"))
                            .getSuccess();

            assertNotNull(account.id());
            assertNotNull(card.id());
            assertNotNull(loan.id());
        }
    }

    // ===== Telecom relationships =====

    @Nested
    @DisplayName("Telecom relationships")
    class TelecomScenarios {

        @Test
        @DisplayName("Person has subscription with Telco")
        void personHasSubscriptionWithTelco() {
            PartyId subscriber = registerPerson("Abonent", "Mobilny");
            PartyId telco = registerCompany("MobileTel S.A.");

            Result<String, PartyRelationshipView> result =
                    relationshipsFacade.handle(
                            new AssignPartyRelationshipCommand(
                                    subscriber,
                                    "Subscriber",
                                    telco,
                                    "Service Provider",
                                    "Mobile Subscription"));

            assertTrue(result.success());
        }

        @Test
        @DisplayName("Company has corporate subscription with Telco")
        void companyHasCorporateSubscription() {
            PartyId company = registerCompany("BigEnterprise S.A.");
            PartyId telco = registerCompany("BusinessTel Sp. z o.o.");

            Result<String, PartyRelationshipView> result =
                    relationshipsFacade.handle(
                            new AssignPartyRelationshipCommand(
                                    company,
                                    "Corporate Subscriber",
                                    telco,
                                    "Corporate Service Provider",
                                    "Corporate Subscription"));

            assertTrue(result.success());
        }

        @Test
        @DisplayName("Person can have multiple subscriptions")
        void personCanHaveMultipleSubscriptions() {
            PartyId person = registerPerson("Multi", "Abonent");
            PartyId telco = registerCompany("AllServicesTel S.A.");

            PartyRelationshipView mobile =
                    relationshipsFacade
                            .handle(
                                    new AssignPartyRelationshipCommand(
                                            person,
                                            "Mobile Subscriber",
                                            telco,
                                            "Mobile Provider",
                                            "Mobile Voice"))
                            .getSuccess();

            PartyRelationshipView internet =
                    relationshipsFacade
                            .handle(
                                    new AssignPartyRelationshipCommand(
                                            person,
                                            "Internet Subscriber",
                                            telco,
                                            "Internet Provider",
                                            "Home Internet"))
                            .getSuccess();

            PartyRelationshipView tv =
                    relationshipsFacade
                            .handle(
                                    new AssignPartyRelationshipCommand(
                                            person,
                                            "TV Subscriber",
                                            telco,
                                            "TV Provider",
                                            "Cable TV"))
                            .getSuccess();

            assertNotNull(mobile.id());
            assertNotNull(internet.id());
            assertNotNull(tv.id());
        }
    }

    // ===== Organization structure =====

    @Nested
    @DisplayName("Organization structure")
    class OrganizationStructureScenarios {

        @Test
        @DisplayName("Department belongs to Company")
        void departmentBelongsToCompany() {
            PartyId hrDepartment = registerOrganizationUnit("HR Department");
            PartyId company = registerCompany("CorporateHQ S.A.");

            Result<String, PartyRelationshipView> result =
                    relationshipsFacade.handle(
                            new AssignPartyRelationshipCommand(
                                    hrDepartment,
                                    "Department",
                                    company,
                                    "Parent Organization",
                                    "Organizational Membership"));

            assertTrue(result.success());
        }

        @Test
        @DisplayName("Multiple departments belong to same Company")
        void multipleDepartmentsBelongToCompany() {
            PartyId company = registerCompany("BigOrg S.A.");
            PartyId hr = registerOrganizationUnit("HR");
            PartyId it = registerOrganizationUnit("IT");
            PartyId sales = registerOrganizationUnit("Sales");
            PartyId marketing = registerOrganizationUnit("Marketing");

            createOrganizationalMembership(hr, company);
            createOrganizationalMembership(it, company);
            createOrganizationalMembership(sales, company);
            createOrganizationalMembership(marketing, company);

            // All relationships created successfully - check from each department
            assertTrue(relationshipsQueries.findAllRelationsFrom(hr).size() >= 1);
            assertTrue(relationshipsQueries.findAllRelationsFrom(it).size() >= 1);
            assertTrue(relationshipsQueries.findAllRelationsFrom(sales).size() >= 1);
            assertTrue(relationshipsQueries.findAllRelationsFrom(marketing).size() >= 1);
        }

        @Test
        @DisplayName("Team belongs to Department")
        void teamBelongsToDepartment() {
            PartyId team = registerOrganizationUnit("Backend Team");
            PartyId department = registerOrganizationUnit("Engineering Department");

            Result<String, PartyRelationshipView> result =
                    relationshipsFacade.handle(
                            new AssignPartyRelationshipCommand(
                                    team, "Team", department, "Department", "Team Membership"));

            assertTrue(result.success());
        }

        @Test
        @DisplayName("Hierarchical structure: Team -> Department -> Company")
        void hierarchicalOrganizationStructure() {
            PartyId company = registerCompany("TechCorp S.A.");
            PartyId engineeringDept = registerOrganizationUnit("Engineering");
            PartyId backendTeam = registerOrganizationUnit("Backend Team");

            PartyRelationshipView deptToCompany =
                    relationshipsFacade
                            .handle(
                                    new AssignPartyRelationshipCommand(
                                            engineeringDept,
                                            "Department",
                                            company,
                                            "Company",
                                            "Organizational Membership"))
                            .getSuccess();

            PartyRelationshipView teamToDept =
                    relationshipsFacade
                            .handle(
                                    new AssignPartyRelationshipCommand(
                                            backendTeam,
                                            "Team",
                                            engineeringDept,
                                            "Department",
                                            "Team Membership"))
                            .getSuccess();

            assertNotNull(deptToCompany.id());
            assertNotNull(teamToDept.id());
        }
    }

    // ===== Business partnerships =====

    @Nested
    @DisplayName("Business partnerships")
    class PartnershipScenarios {

        @Test
        @DisplayName("Strategic partnership between two companies")
        void strategicPartnership() {
            PartyId company1 = registerCompany("Tech Innovations Ltd");
            PartyId company2 = registerCompany("Marketing Solutions S.A.");

            Result<String, PartyRelationshipView> result =
                    relationshipsFacade.handle(
                            new AssignPartyRelationshipCommand(
                                    company1,
                                    "Partner",
                                    company2,
                                    "Partner",
                                    "Strategic Partnership"));

            assertTrue(result.success());
        }

        @Test
        @DisplayName("Supplier-Customer relationship")
        void supplierCustomerRelationship() {
            PartyId supplier = registerCompany("Parts Manufacturer Ltd");
            PartyId customer = registerCompany("Product Assembly S.A.");

            Result<String, PartyRelationshipView> result =
                    relationshipsFacade.handle(
                            new AssignPartyRelationshipCommand(
                                    supplier,
                                    "Supplier",
                                    customer,
                                    "Customer",
                                    "Supply Agreement"));

            assertTrue(result.success());
        }

        @Test
        @DisplayName("Franchise relationship")
        void franchiseRelationship() {
            PartyId franchisor = registerCompany("FastFood International");
            PartyId franchisee = registerCompany("Local FastFood Sp. z o.o.");

            Result<String, PartyRelationshipView> result =
                    relationshipsFacade.handle(
                            new AssignPartyRelationshipCommand(
                                    franchisee,
                                    "Franchisee",
                                    franchisor,
                                    "Franchisor",
                                    "Franchise Agreement"));

            assertTrue(result.success());
        }

        @Test
        @DisplayName("Distribution agreement")
        void distributionAgreement() {
            PartyId manufacturer = registerCompany("Electronics Producer Ltd");
            PartyId distributor = registerCompany("Regional Distributor S.A.");

            Result<String, PartyRelationshipView> result =
                    relationshipsFacade.handle(
                            new AssignPartyRelationshipCommand(
                                    distributor,
                                    "Distributor",
                                    manufacturer,
                                    "Manufacturer",
                                    "Distribution Agreement"));

            assertTrue(result.success());
        }
    }

    // ===== Relationship management =====

    @Nested
    @DisplayName("Relationship management")
    class RelationshipManagementScenarios {

        @Test
        @DisplayName("Relationship can be terminated")
        void relationshipCanBeTerminated() {
            PartyId employee = registerPerson("Były", "Pracownik");
            PartyId employer = registerCompany("Former Employer S.A.");

            PartyRelationshipView employment = createEmployment(employee, employer);

            Result<String, PartyRelationshipId> result =
                    relationshipsFacade.handle(new RemovePartyRelationshipCommand(employment.id()));

            assertTrue(result.success());
            assertTrue(relationshipsQueries.findBy(employment.id()).isEmpty());
        }

        @Test
        @DisplayName("Relationship can be queried by id")
        void relationshipCanBeQueriedById() {
            PartyId person = registerPerson("Query", "Test");
            PartyId company = registerCompany("Query Test S.A.");

            PartyRelationshipView created = createEmployment(person, company);

            var found = relationshipsQueries.findBy(created.id());

            assertTrue(found.isPresent());
            assertEquals(created.id(), found.get().id());
        }

        @Test
        @DisplayName("All relationships for party can be queried")
        void allRelationshipsForPartyCanBeQueried() {
            PartyId person = registerPerson("Multi", "Related");
            PartyId company1 = registerCompany("Company One");
            PartyId company2 = registerCompany("Company Two");

            createEmployment(person, company1);
            relationshipsFacade
                    .handle(
                            new AssignPartyRelationshipCommand(
                                    person, "Consultant", company2, "Client", "Consulting"))
                    .getSuccess();

            var relationships = relationshipsQueries.findAllRelationsFrom(person);

            assertTrue(relationships.size() >= 2);
        }
    }

    // ===== Helper methods =====

    private PartyId registerPerson(String firstName, String lastName) {
        return partiesFacade
                .handle(new RegisterPersonCommand(firstName, lastName, Set.of(), Set.of()))
                .getSuccess()
                .partyId();
    }

    private PartyId registerCompany(String name) {
        return partiesFacade
                .handle(new RegisterCompanyCommand(name, Set.of(), Set.of()))
                .getSuccess()
                .partyId();
    }

    private PartyId registerOrganizationUnit(String name) {
        return partiesFacade
                .handle(new RegisterOrganizationUnitCommand(name, Set.of(), Set.of()))
                .getSuccess()
                .partyId();
    }

    private PartyRelationshipView createEmployment(PartyId employee, PartyId employer) {
        return relationshipsFacade
                .handle(
                        new AssignPartyRelationshipCommand(
                                employee, "Employee", employer, "Employer", "Employment"))
                .getSuccess();
    }

    private void createOrganizationalMembership(PartyId unit, PartyId parent) {
        relationshipsFacade.handle(
                new AssignPartyRelationshipCommand(
                        unit, "Unit", parent, "Parent", "Organizational Membership"));
    }
}
