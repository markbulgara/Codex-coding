package com.example.hytale.party;

import com.example.hytale.party.commands.PartyCommand;
import com.example.hytale.party.indicators.PartyIndicatorRenderer;
import com.example.hytale.party.indicators.PartyMemberTracker;
import com.example.hytale.party.markers.MarkerSyncService;

import java.util.Objects;

/**
 * Entry point for the party map marker sync mod.
 *
 * This is intentionally lightweight and focuses on organizing the data flows
 * that a Hytale modding API would connect to via event hooks.
 */
public final class PartyMod {
    private final PartyManager partyManager;
    private final MarkerSyncService markerSyncService;
    private final PartyMemberTracker partyMemberTracker;
    private final PartyCommand partyCommand;

    public PartyMod(PlayerDirectory playerDirectory, PartyIndicatorRenderer indicatorRenderer) {
        this.partyManager = new PartyManager();
        this.markerSyncService = new MarkerSyncService(partyManager, playerDirectory);
        this.partyMemberTracker = new PartyMemberTracker(partyManager, playerDirectory, indicatorRenderer);
        this.partyCommand = new PartyCommand(partyManager, markerSyncService, playerDirectory);
    }

    public PartyManager getPartyManager() {
        return partyManager;
    }

    public MarkerSyncService getMarkerSyncService() {
        return markerSyncService;
    }

    public PartyMemberTracker getPartyMemberTracker() {
        return partyMemberTracker;
    }

    public PartyCommand getPartyCommand() {
        return partyCommand;
    }

    public void onPlayerDisconnected(Player player) {
        Objects.requireNonNull(player, "player");
        partyManager.removePlayerFromParty(player);
        partyManager.clearInvitesFor(player.getId());
    }

    public void onMarkerPlaced(Player player, MapMarker marker) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(marker, "marker");
        markerSyncService.handleMarkerPlaced(player, marker);
    }

    public void onMarkerUpdated(Player player, MapMarker marker) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(marker, "marker");
        markerSyncService.handleMarkerUpdated(player, marker);
    }

    public void onMarkerRemoved(Player player, MapMarker marker) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(marker, "marker");
        markerSyncService.handleMarkerRemoved(player, marker);
    }

    public void onPlayerMoved(Player player, PartyMemberLocation location) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(location, "location");
        partyMemberTracker.handlePlayerMoved(player, location);
    }
}
