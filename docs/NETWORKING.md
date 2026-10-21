# Authoritative multiplayer architecture (planned)

Version 0.2 implements persistent **local single-player** gameplay. It is not a secure or synchronized multiplayer simulation. The `rpg` package separates the simulation from Minecraft rendering and input; the server can own these rules later. Current JSON saves are client-local and must not be trusted by a future server.

## Target model
- Server owns party membership, invitations, unanimous ready state, stage unlocks, player attributes, skill points, Minebound inventory, HP/PP, enemy AI, timers, projectile seeds and all damage.
- Client owns UI and sends intents (skill purchase, battle command, movement direction/inputs, ready change). **Never accept raw damage or unlocked skills from clients.**
- For shared dodge phase, run authoritative simulation at **20 ticks/sec** using integer/sub-pixel positions, predictable projectile patterns and small collision shapes.
- Broadcast arena snapshots plus seed, tick number, heart positions, attack phase, HP/PP. Clients interpolate/predict only locally and smoothly reconcile corrections.
- Players can move independently; attacks intended for specific hearts should use visual signifiers, but everyone inhabits the same box.
- Disconnect/reconnect policy: disconnected hearts become invulnerable/removed after short grace, and the server pauses or continues based on configurable policy; avoid griefing via ready-state races.
- Validate input rate and movement speed. Use conservative lag compensation and keep encounters beatable despite network jitter.

## Milestones
1. Move local profiles to persistent server-side player data (per-UUID), validate creation/skill intents, and define explicit import policy for client saves.
2. Custom NeoForge payloads and server/clients sync.
3. Party invitations + ready up; leader-controlled stage selection.
4. Server turn phases with timeouts so party members cannot hold the game forever.
5. Simultaneous shared dodge and collision validation.
6. Reconnect, co-op scaling and automated tests.

Do not ship a fully local simulation on public servers.
