package com.example.hytale.party.indicators;

import com.example.hytale.party.Party;
import com.example.hytale.party.PartyManager;
import com.example.hytale.party.PartyMemberLocation;
import com.example.hytale.party.Player;
import com.example.hytale.party.PlayerDirectory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class PartyMemberTracker {
    private final PartyManager partyManager;
    private final PlayerDirectory playerDirectory;
    private final PartyIndicatorRenderer indicatorRenderer;
    private final Map<UUID, PartyMemberLocation> lastKnownLocations;

    public PartyMemberTracker(
            PartyManager partyManager,
            PlayerDirectory playerDirectory,
            PartyIndicatorRenderer indicatorRenderer
    ) {
        this.partyManager = partyManager;
        this.playerDirectory = playerDirectory;
        this.indicatorRenderer = indicatorRenderer;
        this.lastKnownLocations = new HashMap<>();
    }

    public void handlePlayerMoved(Player player, PartyMemberLocation location) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(location, "location");

        lastKnownLocations.put(player.getId(), location);
        Optional<Party> party = partyManager.getPartyFor(player);
        if (party.isEmpty()) {
            return;
        }

        for (UUID memberId : party.get().getMembers()) {
            if (memberId.equals(player.getId())) {
                continue;
            }
            playerDirectory.findById(memberId)
                    .ifPresent(viewer -> indicatorRenderer.showPartyMemberIndicator(viewer, player, location));
        }
    }

    public Optional<PartyMemberLocation> getLastKnownLocation(UUID memberId) {
        return Optional.ofNullable(lastKnownLocations.get(memberId));
    }
}
