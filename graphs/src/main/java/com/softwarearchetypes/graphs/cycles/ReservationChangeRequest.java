package com.softwarearchetypes.graphs.cycles;

record ReservationChangeRequest(SlotId fromSlot, SlotId toSlot, OwnerId userId) {}
