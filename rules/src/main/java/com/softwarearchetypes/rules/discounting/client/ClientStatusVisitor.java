package com.softwarearchetypes.rules.discounting.client;

public interface ClientStatusVisitor<R> {
    R visitStandard();

    R visitVIP();

    R visitGold();
}
