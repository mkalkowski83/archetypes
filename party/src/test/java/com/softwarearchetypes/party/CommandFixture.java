package com.softwarearchetypes.party;

import static com.softwarearchetypes.party.OrganizationNameFixture.someOrganizationName;
import static com.softwarearchetypes.party.PersonalDataFixture.someFirstName;
import static com.softwarearchetypes.party.PersonalDataFixture.someLastName;

import com.softwarearchetypes.party.commands.RegisterCompanyCommand;
import com.softwarearchetypes.party.commands.RegisterPersonCommand;
import java.util.Set;

class CommandFixture {

    static RegisterPersonCommand someRegisterPersonCommand() {
        return new RegisterPersonCommand(someFirstName(), someLastName(), Set.of(), Set.of());
    }

    static RegisterPersonCommand registerPersonCommand(String firstName, String lastName) {
        return new RegisterPersonCommand(firstName, lastName, Set.of(), Set.of());
    }

    static RegisterCompanyCommand someRegisterCompanyCommand() {
        return new RegisterCompanyCommand(someOrganizationName().asString(), Set.of(), Set.of());
    }

    static RegisterCompanyCommand registerCompanyCommand(String name) {
        return new RegisterCompanyCommand(name, Set.of(), Set.of());
    }
}
