package com.softwarearchetypes.party;

import static com.softwarearchetypes.party.PartyFixture.somePartyOfType;
import static com.softwarearchetypes.party.RoleFixture.someRole;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softwarearchetypes.common.Result;
import com.softwarearchetypes.party.events.RoleAdded;
import com.softwarearchetypes.party.events.RoleAdditionSkipped;
import com.softwarearchetypes.party.events.RoleRemovalSkipped;
import com.softwarearchetypes.party.events.RoleRemoved;
import org.junit.jupiter.api.Test;

abstract class PartyRolesTest<T extends Party> {

    Class<T> supportedClass;

    PartyRolesTest(Class<T> supportedClass) {
        this.supportedClass = supportedClass;
    }

    @Test
    void shouldAddRoleToTheParty() {
        // given
        T party = somePartyOfType(supportedClass).withRandomPartyId().build();

        // and
        Role role = someRole();

        // when
        Result<String, Party> result = party.add(role);

        // then
        assertTrue(result.success());
    }

    @Test
    void shouldReturnAddedRole() {
        // given
        T party = somePartyOfType(supportedClass).withRandomPartyId().build();

        // and
        Role role = someRole();

        // when
        party.add(role);

        // then
        assertTrue(party.roles().contains(role));
    }

    @Test
    void shouldGenerateRoleAddedEventWhenSuccessfullyAddingRole() {
        // given
        T party = somePartyOfType(supportedClass).withRandomPartyId().build();

        // and
        Role role = someRole();
        RoleAdded expectedEvent = new RoleAdded(party.id().asString(), role.name());

        // when
        party.add(role);

        // then
        assertTrue(party.events().contains(expectedEvent));
    }

    @Test
    void shouldGenerateRoleAdditionSkippedEventWhenAddingAlreadyExistingRole() {
        // given
        Role role = someRole();
        T party = somePartyOfType(supportedClass).withRandomPartyId().with(role).build();
        RoleAdditionSkipped expectedEvent =
                RoleAdditionSkipped.dueToDuplicationFor(party.id().asString(), role.name());

        // when
        party.add(role);

        // then
        assertTrue(party.events().contains(expectedEvent));
    }

    @Test
    void shouldRemoveRoleFromParty() {
        // given
        Role role = someRole();
        T party = somePartyOfType(supportedClass).withRandomPartyId().with(role).build();

        // when
        Result<String, Party> result = party.remove(role);

        // then
        assertTrue(result.success());
    }

    @Test
    void shouldNotReturnRemovedRole() {
        // given
        Role role = someRole();
        T party = somePartyOfType(supportedClass).withRandomPartyId().with(role).build();

        // when
        party.remove(role);

        // then
        assertFalse(party.roles().contains(role));
    }

    @Test
    void shouldGenerateRoleRemovedEventWhenSuccessfullyAddingRole() {
        // given
        Role role = someRole();
        T party = somePartyOfType(supportedClass).withRandomPartyId().with(role).build();
        RoleRemoved expectedEvent = new RoleRemoved(party.id().asString(), role.name());

        // when
        party.remove(role);

        // then
        assertTrue(party.events().contains(expectedEvent));
    }

    @Test
    void shouldGenerateRoleRemovalSkippedEventWhenRemovingNonExistingRole() {
        // given
        T party = somePartyOfType(supportedClass).withRandomPartyId().build();

        // and
        Role roleToBeDeleted = someRole();
        RoleRemovalSkipped expectedEvent =
                RoleRemovalSkipped.dueToMissingRoleFor(
                        party.id().asString(), roleToBeDeleted.name());

        // when
        party.remove(roleToBeDeleted);

        // then
        assertTrue(party.events().contains(expectedEvent));
    }

    /*
    public Result<String, Party> add(Role role) {
        return roles.add(role).map(event -> {
            events.add(event);
            return this;
        });
    }

    public Result<String, Party> remove(Role role) {
        return roles.remove(role).map(event -> {
            events.add(event);
            return this;
        });
    }

    public Result<String, Party> add(RegisteredIdentifier identifier) {
        return registeredIdentifiers.add(identifier).map(event -> {
            events.add(event);
            return this;
        });
    }

    public Result<String, Party> remove(RegisteredIdentifier identifier) {
        return registeredIdentifiers.remove(identifier).map(event -> {
            events.add(event);
            return this;
        });
    }

    public final PartyId id() {
        return partyId;
    }

    public final Roles roles() {
        return roles;
    }

    public final RegisteredIdentifiers registeredIdentifiers() {
        return registeredIdentifiers;
    }

    public List<PartyRelatedEvent> events() {
        return List.copyOf(events);
    }

    final void register(PartyRelatedEvent event) {
        events.add(event);
    }
     */

}
