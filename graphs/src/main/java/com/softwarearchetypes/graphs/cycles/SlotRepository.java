package com.softwarearchetypes.graphs.cycles;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

interface SlotRepository {
    Optional<Slot> findById(SlotId slotId);

    void save(Slot slot);

    void saveAll(Collection<Slot> values);

    Map<SlotId, Slot> findAll(Set<SlotId> allSlotIds);
}

class InMemorySlotRepository implements SlotRepository {

    private final Map<SlotId, Slot> slots = new ConcurrentHashMap<>();

    @Override
    public Optional<Slot> findById(SlotId slotId) {
        return Optional.ofNullable(slots.get(slotId));
    }

    @Override
    public void save(Slot slot) {
        slots.put(slot.id(), slot);
    }

    public Map<SlotId, Slot> findAll(Set<SlotId> allSlotIds) {
        return allSlotIds.stream()
                .collect(
                        Collectors.toMap(
                                slotId -> slotId,
                                slotId -> {
                                    Slot original = findById(slotId).orElse(null);
                                    // Zwracaj kopię bo to repo w pamieci
                                    return original != null
                                            ? Slot.create(original.id(), original.getOwner())
                                            : null;
                                }));
    }

    public void saveAll(Collection<Slot> slots) {
        slots.forEach(this::save);
    }
}
