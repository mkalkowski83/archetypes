package com.softwarearchetypes.graphs.userjourney;

record UserJourneyId(String value) {

    static UserJourneyId of(String value) {
        return new UserJourneyId(value);
    }
}
