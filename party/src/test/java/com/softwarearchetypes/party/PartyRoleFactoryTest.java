package com.softwarearchetypes.party;

import static com.softwarearchetypes.party.PartyFixture.somePerson;
import static com.softwarearchetypes.party.RoleFixture.someRole;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.softwarearchetypes.common.Result;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PartyRoleFactoryTest {

    @Test
    void shouldCreatePartyRoleWhenAcceptAllPolicyIsApplied() {
        // given
        PartyRoleFactory factory = new PartyRoleFactory();

        // and
        Party party = somePerson().withRandomPartyId().build();
        Role role = someRole();

        // and
        PartyRole expectedPartyRole = PartyRole.of(party.id(), role);

        // when
        Result<String, PartyRole> result = factory.defineFor(party, role);

        // then
        assertEquals(expectedPartyRole, result.getSuccess());
    }

    @Test
    void shouldFailToCreatePartyRoleWhenPolicyAcceptingOnlyCompaniesIsApplied() {
        // given
        PartyRoleDefiningPolicy acceptOnlyCompaniesPolicy =
                (party, role) ->
                        Optional.ofNullable(party).filter(Company.class::isInstance).isPresent();
        PartyRoleFactory factory = new PartyRoleFactory(acceptOnlyCompaniesPolicy);

        // and
        Party party = somePerson().withRandomPartyId().build();
        Role role = someRole();

        // when
        Result<String, PartyRole> result = factory.defineFor(party, role);

        // then
        assertEquals("Policies for assigning party role not met", result.getFailure());
    }
}
