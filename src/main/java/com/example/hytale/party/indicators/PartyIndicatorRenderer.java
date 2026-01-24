package com.example.hytale.party.indicators;

import com.example.hytale.party.PartyMemberLocation;
import com.example.hytale.party.Player;

public interface PartyIndicatorRenderer {
    void showPartyMemberIndicator(Player viewer, Player target, PartyMemberLocation location);
}
