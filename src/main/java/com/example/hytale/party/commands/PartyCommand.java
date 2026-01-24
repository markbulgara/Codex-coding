package com.example.hytale.party.commands;

import com.example.hytale.party.Party;
import com.example.hytale.party.PartyInvite;
import com.example.hytale.party.PartyManager;
import com.example.hytale.party.Player;
import com.example.hytale.party.PlayerDirectory;
import com.example.hytale.party.markers.MarkerSyncService;

import java.util.Collection;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public final class PartyCommand {
    private final PartyManager partyManager;
    private final MarkerSyncService markerSyncService;
    private final PlayerDirectory playerDirectory;

    public PartyCommand(PartyManager partyManager, MarkerSyncService markerSyncService, PlayerDirectory playerDirectory) {
        this.partyManager = partyManager;
        this.markerSyncService = markerSyncService;
        this.playerDirectory = playerDirectory;
    }

    public void handleCommand(Player sender, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return;
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);
        switch (subcommand) {
            case "invite" -> handleInvite(sender, args);
            case "accept" -> handleAccept(sender, args);
            case "decline" -> handleDecline(sender, args);
            case "leave" -> handleLeave(sender);
            case "markers" -> handleMarkers(sender);
            default -> sendHelp(sender);
        }
    }

    private void handleInvite(Player sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("Usage: /party invite <player>");
            return;
        }
        String targetName = args[1];
        Optional<Player> target = playerDirectory.findByName(targetName);
        if (target.isEmpty()) {
            sender.sendMessage("Player not found: " + targetName);
            return;
        }
        if (target.get().getId().equals(sender.getId())) {
            sender.sendMessage("You cannot invite yourself.");
            return;
        }
        partyManager.createInvite(sender, target.get());
        sender.sendMessage("Party invite sent to " + target.get().getDisplayName() + ".");
        target.get().sendMessage(sender.getDisplayName() + " invited you to a party. Use /party accept to join.");
    }

    private void handleAccept(Player sender, String[] args) {
        Collection<PartyInvite> invites = partyManager.getInvitesFor(sender.getId());
        if (invites.isEmpty()) {
            sender.sendMessage("You have no party invites.");
            return;
        }
        UUID partyId = invites.iterator().next().partyId();
        if (args.length > 1) {
            try {
                partyId = UUID.fromString(args[1]);
            } catch (IllegalArgumentException ex) {
                sender.sendMessage("Invalid party id.");
                return;
            }
        }
        Optional<PartyInvite> accepted = partyManager.acceptInvite(sender.getId(), partyId);
        if (accepted.isEmpty()) {
            sender.sendMessage("Invite not found.");
            return;
        }
        sender.sendMessage("You joined the party.");
    }

    private void handleDecline(Player sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("Usage: /party decline <partyId>");
            return;
        }
        try {
            UUID partyId = UUID.fromString(args[1]);
            partyManager.declineInvite(sender.getId(), partyId);
            sender.sendMessage("Party invite declined.");
        } catch (IllegalArgumentException ex) {
            sender.sendMessage("Invalid party id.");
        }
    }

    private void handleLeave(Player sender) {
        Optional<Party> party = partyManager.getPartyFor(sender);
        if (party.isEmpty()) {
            sender.sendMessage("You are not in a party.");
            return;
        }
        partyManager.removePlayerFromParty(sender);
        sender.sendMessage("You left the party.");
    }

    private void handleMarkers(Player sender) {
        Optional<Party> party = partyManager.getPartyFor(sender);
        if (party.isEmpty()) {
            sender.sendMessage("You are not in a party.");
            return;
        }
        int markerCount = markerSyncService.getMarkersForParty(party.get()).size();
        sender.sendMessage("Party markers: " + markerCount);
    }

    private void sendHelp(Player sender) {
        sender.sendMessage("Party commands: invite, accept, decline, leave, markers");
    }
}
