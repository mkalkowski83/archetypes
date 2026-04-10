package com.softwarearchetypes.party;

import com.softwarearchetypes.common.Result;
import com.softwarearchetypes.common.Version;
import com.softwarearchetypes.party.commands.*;
import com.softwarearchetypes.party.events.EventPublisher;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * The class serves as a facade for the Party Management module, providing a unified interface for
 * managing parties (e.g., individuals, organizations) within the system. It simplifies interaction
 * with the underlying logic by offering methods for registering, updating, and managing parties,
 * roles, and identifiers. This class is designed to support diverse use cases and enforce business
 * policies.
 *
 * <p>### Features: - Registration of new parties with varying levels of complexity. - Updating data
 * for existing parties, including attributes, roles, and identifiers. - Managing unique identifiers
 * and enforcing policy compliance.
 *
 * <p>### Methods Overview:
 *
 * <p>#### Registration Methods: These methods handle the registration process for parties:
 *
 * <p>1. **Complex Registration**: - May require verification steps, such as validating a phone
 * number or email address. - Could involve sending notifications, like an activation code, to the
 * user for confirmation.
 *
 * <p>2. **Simple Registration**: - For specific cases, such as when a company is imported from a
 * CRM system, verification steps might not be necessary. Minimal validation is performed.
 *
 * <p>3. **Unique Identifier Verification**: - Based on organizational policies, these methods can
 * enforce the uniqueness of a "registered identifier" (e.g., email or username) to ensure it is not
 * already in use.
 *
 * <p>4. **Mandatory Address Requirement**: - Enforces requirements for providing specific types of
 * contact information during registration. - For instance, a confirmed phone number might be
 * mandatory for a party, but not necessarily for an organization. - The system can support the
 * creation of multiple aggregates to accommodate diverse data models.
 *
 * <p>#### Update Methods: These methods allow for modifying existing parties' data, roles, and
 * identifiers:
 *
 * <p>1. **Updating Party Data**: - Enables modification of basic attributes, such as name, or
 * metadata. - May trigger validations or workflows, depending on the type of data being updated.
 *
 * <p>2. **Role Management**: - Supports adding, removing, or updating roles assigned to a party. -
 * Roles can define the behavior or permissions of the party within the system.
 *
 * <p>3. **Identifier Management**: - Manages unique identifiers associated with a party, such as
 * emails, phone numbers, or external system IDs. - Allows for adding new identifiers, modifying
 * existing ones, or marking identifiers as deprecated or invalid. - Ensures compliance with
 * policies requiring identifier uniqueness or specific verification processes. *
 */
// tx required
public class PartiesFacade {

    private final PartyRepository partyRepository;
    private final EventPublisher eventPublisher;
    private final Supplier<PartyId> newPartyIdSupplier;

    PartiesFacade(
            PartyRepository partyRepository,
            EventPublisher eventPublisher,
            Supplier<PartyId> newPartyIdSupplier) {
        this.partyRepository = partyRepository;
        this.eventPublisher = eventPublisher;
        this.newPartyIdSupplier = newPartyIdSupplier != null ? newPartyIdSupplier : PartyId::random;
    }

    public Result<String, PartyView> handle(RegisterPersonCommand command) {
        PersonalData personalData = PersonalData.from(command.firstName(), command.lastName());
        Set<Role> roles = command.roles().stream().map(Role::of).collect(Collectors.toSet());
        return registerPartyAccordingTo(
                        () ->
                                new Person(
                                        newPartyIdSupplier.get(),
                                        personalData,
                                        roles,
                                        command.registeredIdentifiers(),
                                        Version.initial()))
                .map(PartyViewMapper::toView);
    }

    public Result<String, PartyView> handle(RegisterCompanyCommand command) {
        OrganizationName organizationName = OrganizationName.of(command.organizationName());
        Set<Role> roles = command.roles().stream().map(Role::of).collect(Collectors.toSet());
        return registerPartyAccordingTo(
                        () ->
                                new Company(
                                        newPartyIdSupplier.get(),
                                        organizationName,
                                        roles,
                                        command.registeredIdentifiers(),
                                        Version.initial()))
                .map(PartyViewMapper::toView);
    }

    public Result<String, PartyView> handle(RegisterOrganizationUnitCommand command) {
        OrganizationName organizationName = OrganizationName.of(command.organizationName());
        Set<Role> roles = command.roles().stream().map(Role::of).collect(Collectors.toSet());
        return registerPartyAccordingTo(
                        () ->
                                new OrganizationUnit(
                                        newPartyIdSupplier.get(),
                                        organizationName,
                                        roles,
                                        command.registeredIdentifiers(),
                                        Version.initial()))
                .map(PartyViewMapper::toView);
    }

    public Result<String, PartyId> handle(AddRoleCommand command) {
        Role role = Role.of(command.role());
        return partyRepository
                .findBy(command.partyId())
                .map(party -> party.add(role))
                .orElse(Result.failure("Party not found: " + command.partyId().asString()))
                .peekSuccess(partyRepository::save)
                .peekSuccess(party -> eventPublisher.publish(party.publishedEvents()))
                .map(Party::id);
    }

    public Result<String, PartyId> handle(RemoveRoleCommand command) {
        Role role = Role.of(command.role());
        return partyRepository
                .findBy(command.partyId())
                .map(party -> party.remove(role))
                .orElse(Result.failure("Party not found: " + command.partyId().asString()))
                .peekSuccess(partyRepository::save)
                .peekSuccess(party -> eventPublisher.publish(party.publishedEvents()))
                .map(Party::id);
    }

    public Result<String, PartyId> handle(AddRegisteredIdentifierCommand command) {
        return partyRepository
                .findBy(command.partyId())
                .map(party -> party.add(command.registeredIdentifier()))
                .orElse(Result.failure("Party not found: " + command.partyId().asString()))
                .peekSuccess(partyRepository::save)
                .peekSuccess(party -> eventPublisher.publish(party.publishedEvents()))
                .map(Party::id);
    }

    public Result<String, PartyId> handle(RemoveRegisteredIdentifierCommand command) {
        return partyRepository
                .findBy(command.partyId())
                .map(party -> party.remove(command.registeredIdentifier()))
                .orElse(Result.failure("Party not found: " + command.partyId().asString()))
                .peekSuccess(partyRepository::save)
                .peekSuccess(party -> eventPublisher.publish(party.publishedEvents()))
                .map(Party::id);
    }

    public Result<String, PartyView> handle(UpdatePersonalDataCommand command) {
        PersonalData personalData = PersonalData.from(command.firstName(), command.lastName());
        return partyRepository
                .findBy(command.partyId(), Person.class)
                .map(Person.class::cast)
                .map(party -> party.update(personalData))
                .orElse(
                        Result.failure(
                                "Incorrect party type for: "
                                        + command.partyId().asString()
                                        + ", expected: Person"))
                .peekSuccess(partyRepository::save)
                .peekSuccess(party -> eventPublisher.publish(party.publishedEvents()))
                .map(PartyViewMapper::toView);
    }

    public Result<String, PartyView> handle(UpdateOrganizationNameCommand command) {
        OrganizationName organizationName = OrganizationName.of(command.organizationName());
        return partyRepository
                .findBy(command.partyId(), Organization.class)
                .map(Organization.class::cast)
                .map(party -> party.update(organizationName))
                .orElse(
                        Result.failure(
                                "Incorrect party type for: "
                                        + command.partyId().asString()
                                        + ", expected: Organization"))
                .peekSuccess(partyRepository::save)
                .peekSuccess(party -> eventPublisher.publish(party.publishedEvents()))
                .map(PartyViewMapper::toView);
    }

    private Result<String, Party> registerPartyAccordingTo(Supplier<Party> partySupplier) {
        try {
            Party party = partySupplier.get();
            partyRepository.save(party);
            eventPublisher.publish(party.toPartyRegisteredEvent());
            return Result.success(party);
        } catch (Exception ex) {
            return Result.failure("Party registration failed: " + ex.getMessage());
        }
    }
}
