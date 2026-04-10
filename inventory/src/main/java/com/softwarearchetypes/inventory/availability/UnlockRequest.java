package com.softwarearchetypes.inventory.availability;

import java.util.Objects;

public record UnlockRequest(OwnerId requester, BlockadeId blockadeId) {

    public UnlockRequest {
        Objects.requireNonNull(requester, "Requester cannot be null");
        Objects.requireNonNull(blockadeId, "BlockadeId cannot be null");
    }

    public static UnlockRequest of(OwnerId requester, BlockadeId blockadeId) {
        return new UnlockRequest(requester, blockadeId);
    }
}
