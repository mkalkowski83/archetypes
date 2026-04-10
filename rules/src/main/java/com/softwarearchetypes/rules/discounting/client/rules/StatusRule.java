package com.softwarearchetypes.rules.discounting.client.rules;

import com.softwarearchetypes.rules.discounting.client.ClientContext;
import com.softwarearchetypes.rules.discounting.client.ClientStatus;
import com.softwarearchetypes.rules.predicates.RichLogicalPredicate;

public class StatusRule implements RichLogicalPredicate<ClientContext> {
    private final ClientStatus status;

    public StatusRule(ClientStatus status) {
        this.status = status;
    }

    public static StatusRule of(ClientStatus status) {
        return new StatusRule(status);
    }

    @Override
    public boolean test(ClientContext clientContext) {
        return clientContext.status().equals(status);
    }

    public ClientStatus getStatus() {
        return status;
    }
}
