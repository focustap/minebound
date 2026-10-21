# Current authority and future parties

Version 0.3 has real server ownership for each player's character and individual battle, in integrated single-player and on modded servers. Party invitations, ready-up and a shared four-heart arena remain a separate milestone.

The altar requests a server snapshot. The server selects the sender's UUID and loads its world-owned profile. Clients send bounded creation choices, node IDs, commands, item requests and movement directions; they cannot submit HP, damage, XP, completed stages, target player UUIDs or unlocked nodes. Creation budgets, connectivity, costs, stage access and combat state are checked by server rules. Movement is normalized, one input update is accepted per server tick, and stale input stops movement. The server advances bullets and resolves collisions.

Client rendering is isolated in dist-filtered wiring/screens. Common payload registration uses a client-installed snapshot consumer without resolving rendering classes on the dedicated side. Profile writes retain backups and are independent of vanilla player inventory, death/respawn data and equipment.

Current snapshots run at 20 Hz during dodge turns. There is no client prediction/reconciliation yet; remote latency/playability needs dedicated multiplayer testing. Individual simultaneous encounters are not co-op.

Future party work:

1. Server party membership (maximum four), voluntary invitations and explicit unanimous readiness.
2. A party-owned battle aggregate containing four independently controlled heart states and shared enemy/arena state.
3. Command readiness/timeouts, co-op scaling, targeting signifiers and synchronized phase transitions.
4. Snapshot interpolation/prediction with authoritative reconciliation and appropriate latency tolerance.
5. Disconnect/reconnect and safe readiness changes; no automatic pulling-in of nearby players.

Legacy import is limited to the integrated host's own trusted local 0.2 file. A dedicated server does not accept a client's exported character. Any future administrator import must validate and explicitly choose its trust policy.
