package com.softwarearchetypes.party;

import static com.softwarearchetypes.party.PersonalDataFixture.somePersonalData;
import static com.softwarearchetypes.party.RelationshipNameFixture.someRelationshipName;
import static com.softwarearchetypes.party.RoleFixture.someRole;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softwarearchetypes.common.Result;
import com.softwarearchetypes.party.commands.AssignPartyRelationshipCommand;
import com.softwarearchetypes.party.commands.RegisterPersonCommand;
import com.softwarearchetypes.party.commands.RemovePartyRelationshipCommand;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PartyRelationshipsFacadeTest {

    private final PartyConfiguration configuration = PartyConfiguration.inMemory();
    private final PartiesFacade partiesFacade = configuration.partiesFacade();
    private final PartyRelationshipsFacade facade = configuration.partyRelationshipsFacade();
    private final PartyRelationshipsQueries partyRelationshipsQueries =
            configuration.partyRelationshipsQueries();

    @Test
    void shouldFailToAddRelationshipWhenFromPartyDoesNotExist() {
        // given
        PartyId nonExistingFromPartyId = PartyId.random();
        Role nonExistingFromPartyRole = someRole();

        // and
        PartyId toPartyId = registerSomePerson();
        Role toPartyRole = someRole();

        // and
        RelationshipName relationshipName = someRelationshipName();

        // when
        Result<String, PartyRelationshipView> result =
                facade.handle(
                        new AssignPartyRelationshipCommand(
                                nonExistingFromPartyId,
                                nonExistingFromPartyRole.name(),
                                toPartyId,
                                toPartyRole.name(),
                                relationshipName.value()));

        // then
        assertTrue(result.failure());
        assertEquals("PARTY_NOT_FOUND", result.getFailure());
    }

    @Test
    void shouldFailToAddRelationshipWhenToPartyDoesNotExist() {
        // given
        PartyId fromPartyId = registerSomePerson();
        Role fromPartyRole = someRole();

        // and
        PartyId nonExistingToPartyId = PartyId.random();
        Role nonExistingToPartyRole = someRole();

        // and
        RelationshipName relationshipName = someRelationshipName();

        // when
        Result<String, PartyRelationshipView> result =
                facade.handle(
                        new AssignPartyRelationshipCommand(
                                fromPartyId,
                                fromPartyRole.name(),
                                nonExistingToPartyId,
                                nonExistingToPartyRole.name(),
                                relationshipName.value()));

        // then
        assertTrue(result.failure());
        assertEquals("PARTY_NOT_FOUND", result.getFailure());
    }

    @Test
    void shouldAddRelationshipBetweenParties() {
        // given
        PartyId fromPartyId = registerSomePerson();
        Role fromPartyRole = someRole();

        // and
        PartyId toPartyId = registerSomePerson();
        Role toPartyRole = someRole();

        // and
        RelationshipName relationshipName = someRelationshipName();

        // when
        Result<String, PartyRelationshipView> result =
                facade.handle(
                        new AssignPartyRelationshipCommand(
                                fromPartyId,
                                fromPartyRole.name(),
                                toPartyId,
                                toPartyRole.name(),
                                relationshipName.value()));

        // then
        assertTrue(result.success());
        PartyRelationshipView view = result.getSuccess();
        assertEquals(fromPartyId, view.fromPartyId());
        assertEquals(toPartyId, view.toPartyId());
        assertEquals(relationshipName.value(), view.relationshipName());
    }

    @Test
    void shouldRemoveRelationshipBetweenParties() {
        // given
        PartyId fromPartyId = registerSomePerson();
        Role fromPartyRole = someRole();

        // and
        PartyId toPartyId = registerSomePerson();
        Role toPartyRole = someRole();

        // and
        RelationshipName relationshipName = someRelationshipName();

        // and
        PartyRelationshipView existing =
                facade.handle(
                                new AssignPartyRelationshipCommand(
                                        fromPartyId,
                                        fromPartyRole.name(),
                                        toPartyId,
                                        toPartyRole.name(),
                                        relationshipName.value()))
                        .getSuccess();

        // when
        PartyRelationshipId relationshipId = existing.id();
        Result<String, PartyRelationshipId> result =
                facade.handle(new RemovePartyRelationshipCommand(relationshipId));

        // then
        assertTrue(result.success());
        assertTrue(partyRelationshipsQueries.findBy(relationshipId).isEmpty());
    }

    private PartyId registerSomePerson() {
        PersonalData personalData = somePersonalData();
        return partiesFacade
                .handle(
                        new RegisterPersonCommand(
                                personalData.firstName(),
                                personalData.lastName(),
                                Set.of(),
                                Set.of()))
                .getSuccess()
                .partyId();
    }
}
