package com.example.hytale.party;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class PartyManager {
    private static final Duration INVITE_TTL = Duration.ofMinutes(3);

    private final Map<UUID, Party> partiesByPlayer = new HashMap<>();
    private final Map<UUID, List<PartyInvite>> invitesByRecipient = new HashMap<>();

    public Optional<Party> getPartyFor(Player player) {
        return Optional.ofNullable(partiesByPlayer.get(player.getId()));
    }

    public Party createParty(Player leader) {
        Party party = new Party(leader.getId());
        party.addMember(leader.getId());
        partiesByPlayer.put(leader.getId(), party);
        return party;
    }

    public void removePlayerFromParty(Player player) {
        Party party = partiesByPlayer.remove(player.getId());
        if (party == null) {
            return;
        }
        party.removeMember(player.getId());
        if (party.getMembers().isEmpty()) {
            return;
        }
        if (party.getLeaderId().equals(player.getId())) {
            UUID newLeader = party.getMembers().iterator().next();
            party.setLeaderId(newLeader);
        }
        for (UUID memberId : party.getMembers()) {
            partiesByPlayer.put(memberId, party);
        }
    }

    public void disbandParty(Party party) {
        for (UUID memberId : party.getMembers()) {
            partiesByPlayer.remove(memberId);
        }
    }

    public Collection<PartyInvite> getInvitesFor(UUID recipientId) {
        pruneExpiredInvites(recipientId);
        return Collections.unmodifiableList(invitesByRecipient.getOrDefault(recipientId, List.of()));
    }

    public void createInvite(Player sender, Player recipient) {
        Party party = partiesByPlayer.get(sender.getId());
        if (party == null) {
            party = createParty(sender);
        }
        PartyInvite invite = new PartyInvite(party.getId(), sender.getId(), recipient.getId(), Instant.now());
        invitesByRecipient.computeIfAbsent(recipient.getId(), id -> new ArrayList<>()).add(invite);
    }

    public Optional<PartyInvite> acceptInvite(UUID recipientId, UUID partyId) {
        pruneExpiredInvites(recipientId);
        List<PartyInvite> invites = invitesByRecipient.getOrDefault(recipientId, List.of());
        Optional<PartyInvite> match = invites.stream()
                .filter(invite -> invite.partyId().equals(partyId))
                .findFirst();
        match.ifPresent(invite -> {
            invites.remove(invite);
            Party party = partiesByPlayer.values().stream()
                    .filter(existing -> existing.getId().equals(partyId))
                    .findFirst()
                    .orElseThrow();
            party.addMember(recipientId);
            partiesByPlayer.put(recipientId, party);
        });
        return match;
    }

    public void declineInvite(UUID recipientId, UUID partyId) {
        pruneExpiredInvites(recipientId);
        List<PartyInvite> invites = invitesByRecipient.getOrDefault(recipientId, List.of());
        invites.removeIf(invite -> invite.partyId().equals(partyId));
    }

    public void clearInvitesFor(UUID recipientId) {
        invitesByRecipient.remove(recipientId);
    }

    public boolean isInviteExpired(PartyInvite invite) {
        return invite.sentAt().plus(INVITE_TTL).isBefore(Instant.now());
    }

    private void pruneExpiredInvites(UUID recipientId) {
        List<PartyInvite> invites = invitesByRecipient.get(recipientId);
        if (invites == null) {
            return;
        }
        invites.removeIf(this::isInviteExpired);
        if (invites.isEmpty()) {
            invitesByRecipient.remove(recipientId);
        }
    }

    public Set<Party> getAllParties() {
        return new HashSet<>(partiesByPlayer.values());
    }
}
