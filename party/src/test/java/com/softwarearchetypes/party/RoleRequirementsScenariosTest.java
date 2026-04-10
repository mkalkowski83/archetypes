package com.softwarearchetypes.party;

import static com.softwarearchetypes.party.OperatingScope.ProtocolScope;
import static com.softwarearchetypes.party.OperatingScope.SkillLevelScope;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softwarearchetypes.party.commands.AddCapabilityCommand;
import com.softwarearchetypes.party.commands.RegisterCompanyCommand;
import com.softwarearchetypes.party.commands.RegisterPersonCommand;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Scenarios demonstrating RoleRequirements verification against party capabilities. Shows how to
 * define role requirements and check if parties meet them.
 */
@DisplayName("Role Requirements Scenarios")
class RoleRequirementsScenariosTest {

    private final PartyConfiguration configuration = PartyConfiguration.inMemory();
    private final PartiesFacade partiesFacade = configuration.partiesFacade();
    private final CapabilitiesFacade capabilitiesFacade = configuration.capabilitiesFacade();
    private final CapabilitiesQueries capabilitiesQueries = configuration.capabilitiesQueries();

    @Nested
    @DisplayName("Medical: Senior Radiologist role")
    class SeniorRadiologistScenarios {

        // Role definition: Senior Radiologist requires MedicalImaging capability at Senior level
        final RoleRequirements seniorRadiologist =
                RoleRequirements.forRole("Senior Radiologist")
                        .requireCapability(
                                CapabilityRequirement.requiring("MedicalImaging")
                                        .withScope(SkillLevelScope.SENIOR)
                                        .build())
                        .build();

        @Test
        @DisplayName(
                "Doctor with Senior MedicalImaging capability CAN assume Senior Radiologist role")
        void doctorWithSeniorLevelCanAssumeRole() {
            // Given: Dr. Smith with Senior level MedicalImaging capability
            PartyId drSmith = registerPerson("John", "Smith");
            capabilitiesFacade.handle(
                    new AddCapabilityCommand(
                            drSmith, "MedicalImaging", List.of(SkillLevelScope.SENIOR)));

            // When: Check if can assume Senior Radiologist role
            List<Capability> capabilities = capabilitiesQueries.findByPartyId(drSmith);
            boolean canAssume = seniorRadiologist.isSatisfiedBy(capabilities);

            // Then: Yes, requirements are satisfied
            assertTrue(canAssume);
            assertTrue(seniorRadiologist.findMissing(capabilities).isEmpty());
        }

        @Test
        @DisplayName(
                "Doctor with Expert level CAN assume Senior Radiologist role (Expert > Senior)")
        void doctorWithExpertLevelCanAssumeRole() {
            // Given: Dr. Expert with Expert level (higher than Senior)
            PartyId drExpert = registerPerson("Expert", "Doctor");
            capabilitiesFacade.handle(
                    new AddCapabilityCommand(
                            drExpert, "MedicalImaging", List.of(SkillLevelScope.EXPERT)));

            // When/Then: Expert level satisfies Senior requirement
            List<Capability> capabilities = capabilitiesQueries.findByPartyId(drExpert);
            assertTrue(seniorRadiologist.isSatisfiedBy(capabilities));
        }

        @Test
        @DisplayName(
                "Doctor with Junior MedicalImaging capability CANNOT assume Senior Radiologist role")
        void doctorWithJuniorLevelCannotAssumeRole() {
            // Given: Dr. Junior with only Junior level
            PartyId drJunior = registerPerson("Junior", "Doctor");
            capabilitiesFacade.handle(
                    new AddCapabilityCommand(
                            drJunior, "MedicalImaging", List.of(SkillLevelScope.JUNIOR)));

            // When: Check if can assume Senior Radiologist role
            List<Capability> capabilities = capabilitiesQueries.findByPartyId(drJunior);
            boolean canAssume = seniorRadiologist.isSatisfiedBy(capabilities);

            // Then: No, Junior doesn't meet Senior requirement
            assertFalse(canAssume);
            assertEquals(1, seniorRadiologist.findMissing(capabilities).size());
        }

        @Test
        @DisplayName(
                "Doctor without MedicalImaging capability CANNOT assume Senior Radiologist role")
        void doctorWithoutCapabilityCannotAssumeRole() {
            // Given: Doctor with different capability (not MedicalImaging)
            PartyId drOther = registerPerson("Other", "Doctor");
            capabilitiesFacade.handle(
                    new AddCapabilityCommand(
                            drOther,
                            "Surgery", // Different capability type
                            List.of(SkillLevelScope.EXPERT)));

            // When/Then: Missing MedicalImaging capability entirely
            List<Capability> capabilities = capabilitiesQueries.findByPartyId(drOther);
            assertFalse(seniorRadiologist.isSatisfiedBy(capabilities));
        }
    }

    @Nested
    @DisplayName("Logistics: Hazmat Driver role")
    class HazmatDriverScenarios {

        // Role definition: Hazmat Driver requires GoodsDelivery capability with ADR certification
        final RoleRequirements hazmatDriver =
                RoleRequirements.forRole("Hazmat Driver")
                        .requireCapability(
                                CapabilityRequirement.requiring("GoodsDelivery")
                                        .withScope(ProtocolScope.of("ADR"))
                                        .build())
                        .build();

        @Test
        @DisplayName("Driver with ADR certification CAN assume Hazmat Driver role")
        void driverWithAdrCanAssumeRole() {
            // Given: Driver with ADR certification
            PartyId certifiedDriver = registerPerson("Certified", "Driver");
            capabilitiesFacade.handle(
                    new AddCapabilityCommand(
                            certifiedDriver,
                            "GoodsDelivery",
                            List.of(ProtocolScope.of("ADR", "ISO9001"))));

            // When/Then: Can assume Hazmat Driver role
            List<Capability> capabilities = capabilitiesQueries.findByPartyId(certifiedDriver);
            assertTrue(hazmatDriver.isSatisfiedBy(capabilities));
        }

        @Test
        @DisplayName("Driver without ADR certification CANNOT assume Hazmat Driver role")
        void driverWithoutAdrCannotAssumeRole() {
            // Given: Driver with other certifications but not ADR
            PartyId regularDriver = registerPerson("Regular", "Driver");
            capabilitiesFacade.handle(
                    new AddCapabilityCommand(
                            regularDriver,
                            "GoodsDelivery",
                            List.of(ProtocolScope.of("ISO9001", "HACCP")))); // No ADR!

            // When/Then: Cannot assume Hazmat Driver role
            List<Capability> capabilities = capabilitiesQueries.findByPartyId(regularDriver);
            assertFalse(hazmatDriver.isSatisfiedBy(capabilities));
        }
    }

    @Nested
    @DisplayName("IT: Tech Lead role with multiple requirements")
    class TechLeadScenarios {

        // Role definition: Tech Lead requires both development AND leadership capabilities
        final RoleRequirements techLead =
                RoleRequirements.forRole("Tech Lead")
                        .requireCapability(
                                CapabilityRequirement.requiring("SoftwareDevelopment")
                                        .withScope(SkillLevelScope.SENIOR)
                                        .build())
                        .requireCapability(
                                CapabilityRequirement.requiring("TeamLeadership").build())
                        .build();

        @Test
        @DisplayName("Developer with both capabilities CAN assume Tech Lead role")
        void developerWithBothCapabilitiesCanAssumeRole() {
            // Given: Developer with Senior development AND leadership capability
            PartyId seniorDev = registerPerson("Senior", "Developer");
            capabilitiesFacade.handle(
                    new AddCapabilityCommand(
                            seniorDev, "SoftwareDevelopment", List.of(SkillLevelScope.SENIOR)));
            capabilitiesFacade.handle(
                    new AddCapabilityCommand(seniorDev, "TeamLeadership", List.of()));

            // When/Then: Both requirements satisfied
            List<Capability> capabilities = capabilitiesQueries.findByPartyId(seniorDev);
            assertTrue(techLead.isSatisfiedBy(capabilities));
            assertTrue(techLead.findMissing(capabilities).isEmpty());
        }

        @Test
        @DisplayName("Developer with only development capability CANNOT assume Tech Lead role")
        void developerWithOnlyDevelopmentCannotAssumeRole() {
            // Given: Great developer but no leadership capability
            PartyId justDeveloper = registerPerson("Just", "Developer");
            capabilitiesFacade.handle(
                    new AddCapabilityCommand(
                            justDeveloper, "SoftwareDevelopment", List.of(SkillLevelScope.EXPERT)));
            // Note: No TeamLeadership capability!

            // When: Check requirements
            List<Capability> capabilities = capabilitiesQueries.findByPartyId(justDeveloper);

            // Then: Missing TeamLeadership capability
            assertFalse(techLead.isSatisfiedBy(capabilities));
            List<CapabilityRequirement> missing = techLead.findMissing(capabilities);
            assertEquals(1, missing.size());
            assertEquals(CapabilityType.of("TeamLeadership"), missing.get(0).requiredType());
        }

        @Test
        @DisplayName("Manager with only leadership capability CANNOT assume Tech Lead role")
        void managerWithOnlyLeadershipCannotAssumeRole() {
            // Given: Good manager but not technical enough
            PartyId justManager = registerPerson("Just", "Manager");
            capabilitiesFacade.handle(
                    new AddCapabilityCommand(
                            justManager, "TeamLeadership", List.of(SkillLevelScope.EXPERT)));
            // Note: No SoftwareDevelopment capability!

            // When/Then: Missing development capability
            List<Capability> capabilities = capabilitiesQueries.findByPartyId(justManager);
            assertFalse(techLead.isSatisfiedBy(capabilities));
            List<CapabilityRequirement> missing = techLead.findMissing(capabilities);
            assertEquals(1, missing.size());
            assertEquals(CapabilityType.of("SoftwareDevelopment"), missing.get(0).requiredType());
        }
    }

    @Nested
    @DisplayName("Capability validity affects role eligibility")
    class CapabilityValidityScenarios {

        final RoleRequirements certifiedRole =
                RoleRequirements.forRole("Certified Professional")
                        .requireCapability(
                                CapabilityRequirement.requiring("ProfessionalCertification")
                                        .build())
                        .build();

        @Test
        @DisplayName("Party with expired capability CANNOT assume role")
        void partyWithExpiredCapabilityCannotAssumeRole() {
            // Given: Professional with expired certification
            PartyId professional = registerPerson("Expired", "Professional");
            capabilitiesFacade.handle(
                    new AddCapabilityCommand(
                            professional,
                            "ProfessionalCertification",
                            List.of(),
                            Instant.now().minus(1, ChronoUnit.DAYS))); // Expired yesterday!

            // When/Then: Expired capability doesn't count
            List<Capability> capabilities = capabilitiesQueries.findByPartyId(professional);
            assertFalse(certifiedRole.isSatisfiedBy(capabilities));
        }

        @Test
        @DisplayName("Party with valid capability CAN assume role")
        void partyWithValidCapabilityCanAssumeRole() {
            // Given: Professional with valid certification
            PartyId professional = registerPerson("Valid", "Professional");
            capabilitiesFacade.handle(
                    new AddCapabilityCommand(
                            professional,
                            "ProfessionalCertification",
                            List.of(),
                            Validity.until(
                                    Instant.now().plus(365, ChronoUnit.DAYS)))); // Valid for a year

            // When/Then: Valid capability counts
            List<Capability> capabilities = capabilitiesQueries.findByPartyId(professional);
            assertTrue(certifiedRole.isSatisfiedBy(capabilities));
        }
    }

    @Nested
    @DisplayName("PartyRoleType with requirements")
    class PartyRoleTypeScenarios {

        @Test
        @DisplayName("PartyRoleType can check if party can assume the role")
        void partyRoleTypeChecksRequirements() {
            // Given: Define PartyRoleType with requirements
            PartyRoleType seniorRadiologistType =
                    PartyRoleType.create(
                            "Senior Radiologist",
                            "A radiologist with senior-level imaging expertise",
                            RoleRequirements.forRole("Senior Radiologist")
                                    .requireCapability(
                                            CapabilityRequirement.requiring("MedicalImaging")
                                                    .withScope(SkillLevelScope.SENIOR)
                                                    .build())
                                    .build());

            // And: Two doctors with different capabilities
            PartyId seniorDoctor = registerPerson("Senior", "Doctor");
            capabilitiesFacade.handle(
                    new AddCapabilityCommand(
                            seniorDoctor, "MedicalImaging", List.of(SkillLevelScope.SENIOR)));

            PartyId juniorDoctor = registerPerson("Junior", "Doctor");
            capabilitiesFacade.handle(
                    new AddCapabilityCommand(
                            juniorDoctor, "MedicalImaging", List.of(SkillLevelScope.JUNIOR)));

            // When/Then: PartyRoleType validates correctly
            assertTrue(
                    seniorRadiologistType.canBeAssumedWith(
                            capabilitiesQueries.findByPartyId(seniorDoctor)));
            assertFalse(
                    seniorRadiologistType.canBeAssumedWith(
                            capabilitiesQueries.findByPartyId(juniorDoctor)));
        }

        @Test
        @DisplayName("PartyRoleType without requirements allows anyone")
        void partyRoleTypeWithoutRequirementsAllowsAnyone() {
            // Given: Simple role without requirements
            PartyRoleType customerType = PartyRoleType.create("Customer");

            // And: Party with no capabilities
            PartyId customer = registerPerson("Regular", "Customer");

            // When/Then: No requirements means anyone can assume
            assertFalse(customerType.hasRequirements());
            assertTrue(customerType.canBeAssumedWith(capabilitiesQueries.findByPartyId(customer)));
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
}
