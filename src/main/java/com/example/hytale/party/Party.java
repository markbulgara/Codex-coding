package com.example.hytale.party;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class Party {
    private final UUID id;
    private final Set<UUID> members;
    private UUID leaderId;

    public Party(UUID leaderId) {
        this.id = UUID.randomUUID();
        this.leaderId = leaderId;
        this.members = new HashSet<>();
    }

    public UUID getId() {
        return id;
    }

    public UUID getLeaderId() {
        return leaderId;
    }

    public void setLeaderId(UUID leaderId) {
        this.leaderId = leaderId;
    }

    public Set<UUID> getMembers() {
        return Collections.unmodifiableSet(members);
    }

    public void addMember(UUID playerId) {
        members.add(playerId);
    }

    public void removeMember(UUID playerId) {
        members.remove(playerId);
    }
}
