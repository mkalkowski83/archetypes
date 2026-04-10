package com.softwarearchetypes.party;

import com.softwarearchetypes.common.Result;
import java.util.function.Supplier;

class PartyRelationshipFactory {

    private static final PartyRelationshipDefiningPolicy
            DEFAULT_PARTY_RELATIONSHIP_DEFINING_POLICY =
                    new AlwaysAllowPartyRelationshipDefiningPolicy();
    private final PartyRelationshipDefiningPolicy policy;
    private final Supplier<PartyRelationshipId> partyRelationshipIdSupplier;

    PartyRelationshipFactory(
            PartyRelationshipDefiningPolicy policy,
            Supplier<PartyRelationshipId> partyRelationshipIdSupplier) {
        this.policy = policy != null ? policy : DEFAULT_PARTY_RELATIONSHIP_DEFINING_POLICY;
        this.partyRelationshipIdSupplier =
                partyRelationshipIdSupplier != null
                        ? partyRelationshipIdSupplier
                        : PartyRelationshipId::random;
    }

    PartyRelationshipFactory(Supplier<PartyRelationshipId> partyRelationshipIdSupplier) {
        this(null, partyRelationshipIdSupplier);
    }

    Result<String, PartyRelationship> defineFor(
            PartyRole from, PartyRole to, RelationshipName name) {
        if (policy.canDefineFor(from, to, name)) {
            return Result.success(
                    PartyRelationship.from(partyRelationshipIdSupplier.get(), from, to, name));
        } else {
            return Result.failure("Policies for defining party relationship not met");
        }
    }
}
