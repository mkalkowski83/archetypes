package com.softwarearchetypes.party;

record OrganizationName(String value) {

    static OrganizationName of(String value) {
        return new OrganizationName(value);
    }

    public String asString() {
        return value;
    }
}
