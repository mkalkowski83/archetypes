package com.softwarearchetypes.party;

import static com.softwarearchetypes.party.OperatingScope.LocationScope;
import static com.softwarearchetypes.party.OperatingScope.ProductScope;
import static com.softwarearchetypes.party.OperatingScope.ProtocolScope;
import static com.softwarearchetypes.party.OperatingScope.ResourceScope;
import static com.softwarearchetypes.party.OperatingScope.SkillLevelScope;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softwarearchetypes.party.commands.AddCapabilityCommand;
import com.softwarearchetypes.party.commands.RegisterCompanyCommand;
import com.softwarearchetypes.party.commands.RegisterOrganizationUnitCommand;
import com.softwarearchetypes.party.commands.RegisterPersonCommand;
import com.softwarearchetypes.party.commands.RemoveCapabilityCommand;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * End-to-end scenarios for Capabilities. Tests realistic business scenarios with party registration
 * and capability queries.
 */
@DisplayName("Capabilities End-to-End Scenarios")
class CapabilitiesScenariosTest {

    private PartyConfiguration configuration;
    private PartiesFacade partiesFacade;
    private CapabilitiesFacade capabilitiesFacade;
    private CapabilitiesQueries capabilitiesQueries;

    @BeforeEach
    void setUp() {
        configuration = PartyConfiguration.inMemory();
        partiesFacade = configuration.partiesFacade();
        capabilitiesFacade = configuration.capabilitiesFacade();
        capabilitiesQueries = configuration.capabilitiesQueries();
    }

    // ===== Hospital scenario =====

    @Nested
    @DisplayName("Hospital: Finding doctors by capability")
    class HospitalScenarios {

        @Test
        @DisplayName("Find all radiologists at Hospital A with Senior level")
        void findRadiologistsAtHospitalAWithSeniorLevel() {
            // Register 3 doctors
            PartyId drSmith = registerPerson("John", "Smith");
            PartyId drJones = registerPerson("Anna", "Jones");
            PartyId drBrown = registerPerson("Mike", "Brown");

            // Dr. Smith - Senior at Hospital A and B
            capabilitiesFacade.handle(
                    new AddCapabilityCommand(
                            drSmith,
                            "MedicalImaging",
                            List.of(
                                    LocationScope.of("Hospital A", "Hospital B"),
                                    SkillLevelScope.SENIOR)));

            // Dr. Jones - Expert at Hospital A only
            capabilitiesFacade.handle(
                    new AddCapabilityCommand(
                            drJones,
                            "MedicalImaging",
                            List.of(LocationScope.of("Hospital A"), SkillLevelScope.EXPERT)));

            // Dr. Brown - Junior at Hospital A
            capabilitiesFacade.handle(
                    new AddCapabilityCommand(
                            drBrown,
                            "MedicalImaging",
                            List.of(LocationScope.of("Hospital A"), SkillLevelScope.JUNIOR)));

            // Find doctors at Hospital A with at least Senior level
            List<Capability> results =
                    capabilitiesQueries.findCapabilitiesAtLocationWithSkill(
                            CapabilityType.of("MedicalImaging"),
                            "Hospital A",
                            SkillLevelScope.SENIOR);

            assertEquals(2, results.size());
            assertTrue(results.stream().anyMatch(c -> c.partyId().equals(drSmith)));
            assertTrue(results.stream().anyMatch(c -> c.partyId().equals(drJones)));
            assertFalse(results.stream().anyMatch(c -> c.partyId().equals(drBrown)));
        }

        @Test
        @DisplayName("Find doctors at Hospital B")
        void findDoctorsAtHospitalB() {
            PartyId drSmith = registerPerson("John", "Smith");
            PartyId drJones = registerPerson("Anna", "Jones");

            // Dr. Smith works at Hospital A and B
            capabilitiesFacade.handle(
                    new AddCapabilityCommand(
                            drSmith,
                            "MedicalImaging",
                            List.of(LocationScope.of("Hospital A", "Hospital B"))));

            // Dr. Jones works only at Hospital A
            capabilitiesFacade.handle(
                    new AddCapabilityCommand(
                            drJones, "MedicalImaging", List.of(LocationScope.of("Hospital A"))));

            List<PartyId> atHospitalB =
                    capabilitiesQueries.findPartiesAtLocation(
                            CapabilityType.of("MedicalImaging"), "Hospital B");

            assertEquals(1, atHospitalB.size());
            assertTrue(atHospitalB.contains(drSmith));
        }

        @Test
        @DisplayName("Expired medical license is not returned in search")
        void expiredLicenseNotReturnedInSearch() {
            PartyId drSmith = registerPerson("John", "Smith");
            PartyId drJones = registerPerson("Anna", "Jones");

            // Dr. Smith - valid license
            capabilitiesFacade.handle(
                    new AddCapabilityCommand(
                            drSmith,
                            "MedicalImaging",
                            List.of(LocationScope.of("Hospital A")),
                            Validity.ALWAYS));

            // Dr. Jones - expired license
            capabilitiesFacade.handle(
                    new AddCapabilityCommand(
                            drJones,
                            "MedicalImaging",
                            List.of(LocationScope.of("Hospital A")),
                            Instant.now().minus(1, ChronoUnit.DAYS)));

            List<PartyId> validDoctors =
                    capabilitiesQueries.findPartiesAtLocation(
                            CapabilityType.of("MedicalImaging"), "Hospital A");

            assertEquals(1, validDoctors.size());
            assertTrue(validDoctors.contains(drSmith));
            assertFalse(validDoctors.contains(drJones));
        }
    }

    // ===== Logistics scenario =====

    @Nested
    @DisplayName("Logistics: Finding carriers by capability")
    class LogisticsScenarios {

        @Test
        @DisplayName("Find carriers with ADR certification for hazmat transport")
        void findCarriersWithAdrCertification() {
            PartyId fastLogistics = registerCompany("FastLogistics Sp. z o.o.");
            PartyId safeTransport = registerCompany("SafeTransport S.A.");
            PartyId localDelivery = registerCompany("LocalDelivery");

            // FastLogistics - has ADR
            capabilitiesFacade.handle(
                    new AddCapabilityCommand(
                            fastLogistics,
                            "GoodsDelivery",
                            List.of(
                                    LocationScope.of("Warsaw", "Cracow"),
                                    ProtocolScope.of("ADR", "ISO9001"))));

            // SafeTransport - has ADR
            capabilitiesFacade.handle(
                    new AddCapabilityCommand(
                            safeTransport,
                            "GoodsDelivery",
                            List.of(LocationScope.of("Warsaw"), ProtocolScope.of("ADR"))));

            // LocalDelivery - no ADR
            capabilitiesFacade.handle(
                    new AddCapabilityCommand(
                            localDelivery,
                            "GoodsDelivery",
                            List.of(LocationScope.of("Warsaw"), ProtocolScope.of("ISO9001"))));

            // Find carriers with ADR for hazmat
            CapabilityRequirement hazmatRequirement =
                    CapabilityRequirement.requiring("GoodsDelivery")
                            .withScope(ProtocolScope.of("ADR"))
                            .build();

            List<PartyId> hazmatCarriers =
                    capabilitiesQueries.findPartiesSatisfying(hazmatRequirement);

            assertEquals(2, hazmatCarriers.size());
            assertTrue(hazmatCarriers.contains(fastLogistics));
            assertTrue(hazmatCarriers.contains(safeTransport));
            assertFalse(hazmatCarriers.contains(localDelivery));
        }

        @Test
        @DisplayName("Find carriers covering specific region")
        void findCarriersCoveringRegion() {
            PartyId carrier1 = registerCompany("NationalCarrier");
            PartyId carrier2 = registerCompany("RegionalCarrier");

            // National - covers multiple cities
            capabilitiesFacade.handle(
                    new AddCapabilityCommand(
                            carrier1,
                            "GoodsDelivery",
                            List.of(LocationScope.of("Warsaw", "Cracow", "Gdańsk", "Wrocław"))));

            // Regional - covers only Warsaw
            capabilitiesFacade.handle(
                    new AddCapabilityCommand(
                            carrier2, "GoodsDelivery", List.of(LocationScope.of("Warsaw"))));

            // Find carriers that can deliver to Cracow
            List<PartyId> cracowCarriers =
                    capabilitiesQueries.findPartiesAtLocation(
                            CapabilityType.of("GoodsDelivery"), "Cracow");

            assertEquals(1, cracowCarriers.size());
            assertTrue(cracowCarriers.contains(carrier1));
        }
    }

    // ===== IT Teams scenario =====

    @Nested
    @DisplayName("IT: Finding teams by capability")
    class ITTeamsScenarios {

        @Test
        @DisplayName("Find teams that can work on specific product")
        void findTeamsForProduct() {
            PartyId backendTeam = registerOrganizationUnit("Backend Team");
            PartyId frontendTeam = registerOrganizationUnit("Frontend Team");
            PartyId devOpsTeam = registerOrganizationUnit("DevOps Team");

            // Backend Team - works on API Gateway and Auth Service
            capabilitiesFacade.handle(
                    new AddCapabilityCommand(
                            backendTeam,
                            "SoftwareDevelopment",
                            List.of(
                                    ProductScope.of("API Gateway", "Auth Service"),
                                    SkillLevelScope.SENIOR)));

            // Frontend Team - works on Web Portal
            capabilitiesFacade.handle(
                    new AddCapabilityCommand(
                            frontendTeam,
                            "SoftwareDevelopment",
                            List.of(
                                    ProductScope.of("Web Portal", "Mobile App"),
                                    SkillLevelScope.MID)));

            // DevOps Team - works on infrastructure
            capabilitiesFacade.handle(
                    new AddCapabilityCommand(
                            devOpsTeam,
                            "DevOps",
                            List.of(
                                    ProductScope.of("API Gateway", "Auth Service", "Web Portal"),
                                    ResourceScope.of("AWS", "Kubernetes"))));

            // Find teams that can develop API Gateway
            CapabilityRequirement apiGatewayDev =
                    CapabilityRequirement.requiring("SoftwareDevelopment")
                            .withScope(ProductScope.of("API Gateway"))
                            .build();

            List<PartyId> teamsForApiGateway =
                    capabilitiesQueries.findPartiesSatisfying(apiGatewayDev);

            assertEquals(1, teamsForApiGateway.size());
            assertTrue(teamsForApiGateway.contains(backendTeam));
        }

        @Test
        @DisplayName("Find Senior level development teams")
        void findSeniorDevelopmentTeams() {
            PartyId seniorTeam = registerOrganizationUnit("Senior Team");
            PartyId juniorTeam = registerOrganizationUnit("Junior Team");

            capabilitiesFacade.handle(
                    new AddCapabilityCommand(
                            seniorTeam, "SoftwareDevelopment", List.of(SkillLevelScope.SENIOR)));

            capabilitiesFacade.handle(
                    new AddCapabilityCommand(
                            juniorTeam, "SoftwareDevelopment", List.of(SkillLevelScope.JUNIOR)));

            List<PartyId> seniorTeams =
                    capabilitiesQueries.findPartiesWithSkillLevel(
                            CapabilityType.of("SoftwareDevelopment"), SkillLevelScope.SENIOR);

            assertEquals(1, seniorTeams.size());
            assertTrue(seniorTeams.contains(seniorTeam));
        }
    }

    // ===== Role Requirements scenario =====

    @Nested
    @DisplayName("Role Requirements verification")
    class RoleRequirementsScenarios {

        @Test
        @DisplayName("Verify if party can assume Senior Radiologist role")
        void verifyPartyCanAssumeSeniorRadiologistRole() {
            PartyId drSmith = registerPerson("John", "Smith");
            PartyId drJones = registerPerson("Anna", "Jones");

            // Dr. Smith - Senior level
            capabilitiesFacade.handle(
                    new AddCapabilityCommand(
                            drSmith, "MedicalImaging", List.of(SkillLevelScope.SENIOR)));

            // Dr. Jones - Junior level
            capabilitiesFacade.handle(
                    new AddCapabilityCommand(
                            drJones, "MedicalImaging", List.of(SkillLevelScope.JUNIOR)));

            // Define role requirements
            RoleRequirements seniorRadiologist =
                    RoleRequirements.forRole("Senior Radiologist")
                            .requireCapability(
                                    CapabilityRequirement.requiring("MedicalImaging")
                                            .withScope(SkillLevelScope.SENIOR)
                                            .build())
                            .build();

            // Check Dr. Smith
            List<Capability> smithCapabilities = capabilitiesQueries.findByPartyId(drSmith);
            assertTrue(seniorRadiologist.isSatisfiedBy(smithCapabilities));

            // Check Dr. Jones
            List<Capability> jonesCapabilities = capabilitiesQueries.findByPartyId(drJones);
            assertFalse(seniorRadiologist.isSatisfiedBy(jonesCapabilities));
        }

        @Test
        @DisplayName("Find missing capabilities for role")
        void findMissingCapabilitiesForRole() {
            PartyId candidate = registerPerson("New", "Doctor");

            // Candidate has only basic imaging capability
            capabilitiesFacade.handle(
                    new AddCapabilityCommand(
                            candidate, "MedicalImaging", List.of(SkillLevelScope.JUNIOR)));

            // Role requires multiple capabilities
            RoleRequirements headOfRadiology =
                    RoleRequirements.forRole("Head of Radiology")
                            .requireCapability(
                                    CapabilityRequirement.requiring("MedicalImaging")
                                            .withScope(SkillLevelScope.EXPERT)
                                            .build())
                            .requireCapability(
                                    CapabilityRequirement.requiring("TeamLeadership").build())
                            .build();

            List<Capability> candidateCapabilities = capabilitiesQueries.findByPartyId(candidate);
            List<CapabilityRequirement> missing =
                    headOfRadiology.findMissing(candidateCapabilities);

            assertEquals(2, missing.size()); // Missing Expert level AND TeamLeadership
        }
    }

    // ===== Capability lifecycle =====

    @Nested
    @DisplayName("Capability lifecycle")
    class CapabilityLifecycleScenarios {

        @Test
        @DisplayName("Add capability to party")
        void addCapabilityToParty() {
            PartyId doctor = registerPerson("Test", "Doctor");

            var result =
                    capabilitiesFacade.handle(
                            new AddCapabilityCommand(
                                    doctor,
                                    "MedicalImaging",
                                    List.of(LocationScope.of("Hospital A"))));

            assertTrue(result.success());
            assertEquals(doctor, result.getSuccess().partyId());
            assertEquals(CapabilityType.of("MedicalImaging"), result.getSuccess().type());
        }

        @Test
        @DisplayName("Cannot add capability to non-existing party")
        void cannotAddCapabilityToNonExistingParty() {
            PartyId nonExisting = PartyId.random();

            var result =
                    capabilitiesFacade.handle(
                            new AddCapabilityCommand(nonExisting, "SomeCapability", List.of()));

            assertTrue(result.failure());
            assertEquals("PARTY_NOT_FOUND", result.getFailure());
        }

        @Test
        @DisplayName("Remove capability from party")
        void removeCapabilityFromParty() {
            PartyId doctor = registerPerson("Test", "Doctor");

            var addResult =
                    capabilitiesFacade.handle(
                            new AddCapabilityCommand(doctor, "MedicalImaging", List.of()));

            CapabilityId capabilityId = addResult.getSuccess().id();

            var removeResult = capabilitiesFacade.handle(new RemoveCapabilityCommand(capabilityId));

            assertTrue(removeResult.success());
            assertTrue(capabilitiesQueries.findById(capabilityId).isEmpty());
        }

        @Test
        @DisplayName("Party can have multiple capabilities")
        void partyCanHaveMultipleCapabilities() {
            PartyId doctor = registerPerson("Multi", "Skilled");

            capabilitiesFacade.handle(
                    new AddCapabilityCommand(
                            doctor, "MedicalImaging", List.of(SkillLevelScope.SENIOR)));
            capabilitiesFacade.handle(
                    new AddCapabilityCommand(doctor, "Surgery", List.of(SkillLevelScope.MID)));
            capabilitiesFacade.handle(
                    new AddCapabilityCommand(
                            doctor, "EmergencyMedicine", List.of(SkillLevelScope.EXPERT)));

            List<Capability> capabilities = capabilitiesQueries.findByPartyId(doctor);

            assertEquals(3, capabilities.size());
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
}
