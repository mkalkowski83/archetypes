package com.softwarearchetypes.scoring.context;

import com.softwarearchetypes.scoring.events.CustomerEvent;

public class EventWindowContext extends WindowContext {

    private final CustomerEvent currentEvent;

    public EventWindowContext(WindowContext base, CustomerEvent currentEvent) {
        super(
                base.getCustomerId(),
                base.getFrom(),
                base.getTo(),
                base.getEvents(),
                base.getMetrics());
        this.currentEvent = currentEvent;
    }

    public CustomerEvent getCurrentEvent() {
        return currentEvent;
    }
}
