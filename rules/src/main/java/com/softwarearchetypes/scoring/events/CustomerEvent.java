package com.softwarearchetypes.scoring.events;

import java.time.Instant;

public record CustomerEvent(String type, Instant occurredAt, double amount) {}
