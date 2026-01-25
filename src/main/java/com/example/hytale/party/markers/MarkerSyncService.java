package com.example.hytale.party.markers;

import com.example.hytale.party.MapMarker;
import com.example.hytale.party.Party;
import com.example.hytale.party.PartyManager;
import com.example.hytale.party.Player;
import com.example.hytale.party.PlayerDirectory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class MarkerSyncService {
    private final PartyManager partyManager;
    private final PlayerDirectory playerDirectory;
    private final Map<UUID, Map<UUID, MapMarker>> partyMarkers;

    public MarkerSyncService(PartyManager partyManager, PlayerDirectory playerDirectory) {
        this.partyManager = partyManager;
        this.playerDirectory = playerDirectory;
        this.partyMarkers = new HashMap<>();
    }

    public void handleMarkerPlaced(Player player, MapMarker marker) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(marker, "marker");
        Optional<Party> party = partyManager.getPartyFor(player);
        party.ifPresent(foundParty -> {
            partyMarkers.computeIfAbsent(foundParty.getId(), id -> new HashMap<>())
                    .put(marker.getId(), marker);
            broadcastMarker(foundParty, player, marker, "placed");
        });
    }

    public void handleMarkerUpdated(Player player, MapMarker marker) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(marker, "marker");
        Optional<Party> party = partyManager.getPartyFor(player);
        party.ifPresent(foundParty -> {
            partyMarkers.computeIfAbsent(foundParty.getId(), id -> new HashMap<>())
                    .put(marker.getId(), marker);
            broadcastMarker(foundParty, player, marker, "updated");
        });
    }

    public void handleMarkerRemoved(Player player, MapMarker marker) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(marker, "marker");
        Optional<Party> party = partyManager.getPartyFor(player);
        party.ifPresent(foundParty -> {
            Map<UUID, MapMarker> markers = partyMarkers.get(foundParty.getId());
            if (markers != null) {
                markers.remove(marker.getId());
            }
            broadcastMarker(foundParty, player, marker, "removed");
        });
    }

    public Map<UUID, MapMarker> getMarkersForParty(Party party) {
        return partyMarkers.getOrDefault(party.getId(), Map.of());
    }

    private void broadcastMarker(Party party, Player source, MapMarker marker, String action) {
        for (UUID memberId : party.getMembers()) {
            if (memberId.equals(source.getId())) {
                continue;
            }
            playerDirectory.findById(memberId)
                    .ifPresent(member -> member.sendMessage("[Party] Marker " + action + ": " + marker.getName()));
        }
    }
}
