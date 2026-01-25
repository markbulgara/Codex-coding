package com.example.hytale.party;

import java.time.Instant;
import java.util.UUID;

public record PartyInvite(UUID partyId, UUID senderId, UUID recipientId, Instant sentAt) {
}
