package com.example.hytale.party;

import java.util.UUID;

/**
 * Minimal map marker model used for sync logic.
 */
public final class MapMarker {
    private final UUID id;
    private final String name;
    private final MarkerLocation location;
    private final MarkerType type;

    public MapMarker(UUID id, String name, MarkerLocation location, MarkerType type) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.type = type;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public MarkerLocation getLocation() {
        return location;
    }

    public MarkerType getType() {
        return type;
    }
}
