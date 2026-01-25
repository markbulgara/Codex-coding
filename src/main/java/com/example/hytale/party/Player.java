package com.example.hytale.party;

import java.util.UUID;

/**
 * Minimal representation of a player for the sample mod logic.
 */
public interface Player {
    UUID getId();

    String getDisplayName();

    void sendMessage(String message);
}
