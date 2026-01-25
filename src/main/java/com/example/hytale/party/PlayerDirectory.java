package com.example.hytale.party;

import java.util.Optional;
import java.util.UUID;

public interface PlayerDirectory {
    Optional<Player> findById(UUID playerId);

    Optional<Player> findByName(String displayName);
}
