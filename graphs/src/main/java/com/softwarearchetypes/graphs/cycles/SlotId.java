package com.softwarearchetypes.graphs.cycles;

record SlotId(String value) {

    static SlotId of(String value) {
        return new SlotId(value);
    }
}
