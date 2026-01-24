package com.example.hytale.party;

public final class PartyMemberLocation {
    private final String worldId;
    private final double x;
    private final double y;
    private final double z;

    public PartyMemberLocation(String worldId, double x, double y, double z) {
        this.worldId = worldId;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public String getWorldId() {
        return worldId;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }
}
