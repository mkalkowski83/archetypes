package com.softwarearchetypes.graphs.cycles;

class Slot {
    private final SlotId slotId;
    private OwnerId owner;

    Slot(SlotId slotId, OwnerId owner) {
        this.slotId = slotId;
        this.owner = owner;
    }

    static Slot create(SlotId slotId, OwnerId owner) {
        return new Slot(slotId, owner);
    }

    void release() {
        this.owner = OwnerId.empty();
    }

    void assignTo(OwnerId newOwner) {
        this.owner = newOwner;
    }

    OwnerId getOwner() {
        return owner;
    }

    SlotId id() {
        return slotId;
    }
}
