package com.softwarearchetypes.party;

import com.softwarearchetypes.common.Result;
import com.softwarearchetypes.party.commands.AddCapabilityCommand;
import com.softwarearchetypes.party.commands.RemoveCapabilityCommand;

/** Facade for managing party capabilities. */
public class CapabilitiesFacade {

    private final CapabilitiesRepository repository;
    private final PartiesQueries partiesQueries;

    CapabilitiesFacade(CapabilitiesRepository repository, PartiesQueries partiesQueries) {
        this.repository = repository;
        this.partiesQueries = partiesQueries;
    }

    public Result<String, CapabilityView> handle(AddCapabilityCommand command) {
        // Verify party exists
        if (partiesQueries.findBy(command.partyId()).isEmpty()) {
            return Result.failure("PARTY_NOT_FOUND");
        }

        Capability.Builder builder =
                Capability.forParty(command.partyId())
                        .type(command.capabilityType())
                        .validity(command.validity());

        for (OperatingScope scope : command.scopes()) {
            builder.withScope(scope);
        }

        Capability capability = builder.build();
        repository.save(capability);

        return Result.success(CapabilityView.from(capability));
    }

    public Result<String, CapabilityId> handle(RemoveCapabilityCommand command) {
        if (repository.findById(command.capabilityId()).isEmpty()) {
            return Result.failure("CAPABILITY_NOT_FOUND");
        }

        repository.remove(command.capabilityId());
        return Result.success(command.capabilityId());
    }
}
