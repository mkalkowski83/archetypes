package com.softwarearchetypes.party;

import static com.softwarearchetypes.party.RelationshipNameFixture.someRelationshipName;
import static com.softwarearchetypes.party.RoleFixture.someRole;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.softwarearchetypes.common.Result;
import com.softwarearchetypes.party.PartyRelationshipFixture.FixablePartyRelationshipIdSupplier;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PartyRelationshipFactoryTest {

    private final FixablePartyRelationshipIdSupplier partyRelationshipIdSupplier =
            new FixablePartyRelationshipIdSupplier();

    @Test
    void shouldCreatePartyRelationshipWhenAcceptAllPolicyIsApplied() {
        // given
        PartyRelationshipFactory factory =
                new PartyRelationshipFactory(partyRelationshipIdSupplier);

        // and
        PartyRole fromPartyRole = PartyRole.of(PartyId.random(), someRole());
        PartyRole toPartyRole = PartyRole.of(PartyId.random(), someRole());
        RelationshipName relationshipName = someRelationshipName();

        // and
        PartyRelationshipId relationshipId = PartyRelationshipId.random();
        partyRelationshipIdSupplier.fixPartyRelationshipIdTo(relationshipId);

        // and
        PartyRelationship expectedPartyRelationship =
                PartyRelationship.from(
                        relationshipId, fromPartyRole, toPartyRole, relationshipName);

        // when
        Result<String, PartyRelationship> result =
                factory.defineFor(fromPartyRole, toPartyRole, relationshipName);

        // then
        assertEquals(expectedPartyRelationship, result.getSuccess());

        // cleanup
        partyRelationshipIdSupplier.clear();
    }

    @Test
    void
            shouldFailToCreatePartyRelationshipWhenPolicyAcceptingOnlyPredefinedRelationshipNameIsApplied() {
        // given
        RelationshipName acceptedRelationName = someRelationshipName();
        PartyRelationshipDefiningPolicy acceptOnlyPredefinedRelationship =
                (from, to, name) ->
                        Optional.ofNullable(name).filter(acceptedRelationName::equals).isPresent();
        PartyRelationshipFactory factory =
                new PartyRelationshipFactory(
                        acceptOnlyPredefinedRelationship, PartyRelationshipId::random);

        // and
        PartyRole fromPartyRole = PartyRole.of(PartyId.random(), someRole());
        PartyRole toPartyRole = PartyRole.of(PartyId.random(), someRole());
        RelationshipName relationshipName = someRelationshipName();

        // when
        Result<String, PartyRelationship> result =
                factory.defineFor(fromPartyRole, toPartyRole, relationshipName);

        // then
        assertEquals("Policies for defining party relationship not met", result.getFailure());
    }
}
