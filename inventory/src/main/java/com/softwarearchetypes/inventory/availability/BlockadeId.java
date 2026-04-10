package com.softwarearchetypes.inventory.availability;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record BlockadeId(UUID id) {

    public BlockadeId {
        Objects.requireNonNull(id, "BlockadeId cannot be null");
    }

    public static BlockadeId random() {
        return new BlockadeId(UUID.randomUUID());
    }

    public static BlockadeId of(UUID id) {
        return new BlockadeId(id);
    }

    public static BlockadeId of(String id) {
        return new BlockadeId(UUID.fromString(id));
    }

    public static BlockadeId composite(List<BlockadeId> blockadeIds) {
        // Create a deterministic composite ID from multiple blockade IDs
        int hash = Objects.hash(blockadeIds.toArray());
        return new BlockadeId(new UUID(hash, blockadeIds.size()));
    }
}
