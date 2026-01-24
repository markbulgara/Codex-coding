# Party Marker Sync Mod (Hytale)

This repository contains a starter Hytale mod that synchronizes map markers between party members and adds a party invite workflow. The implementation is API-agnostic, so you can wire it into the official Hytale modding hooks once they are available.

## Features
- Party invite, accept, decline, leave, and marker count commands.
- Party membership management with leader reassignment.
- Map marker sync that broadcasts placed/updated/removed markers to party members.
- Party member indicator updates to render a green dot hovering above party member names.
- Expiring invites to prevent stale invitations.

## Wiring To Hytale
Hook the following entry points to your Hytale mod API events:
- `PartyMod#onPlayerDisconnected`
- `PartyMod#onMarkerPlaced`
- `PartyMod#onMarkerUpdated`
- `PartyMod#onMarkerRemoved`
- `PartyMod#onPlayerMoved`
- `PartyCommand#handleCommand`

Provide an implementation of `PlayerDirectory` so the mod can resolve online players by id or name, plus a `PartyIndicatorRenderer` to draw the green dot indicator.

## Command Examples
- `/party invite <player>`
- `/party accept [partyId]`
- `/party decline <partyId>`
- `/party leave`
- `/party markers`
