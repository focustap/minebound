# Minebound

A menu-driven RPG for **Minecraft 1.21.1 / NeoForge 21.1.262 / Java 21**. The Minebound Altar opens the RPG; it never generates dungeons or imports vanilla equipment into combat.

Version **0.3.1** builds on the complete 0.2 overhaul and 0.3 expansion. The original World 1 enemies, stage unlocks, manual dodge engine and pattern generator are retained.

## Implemented

- Server-owned character creation and profiles: four races, six starting classes (including Warlock), attributes, Adventurer levels/XP, current HP/PP, goods, skills, stage completion and lore.
- HP/PP carry across battles, altar entry, death and logout. Full recovery requires 24,000 elapsed Overworld simulation ticks; sleeping and `/time` cannot mint recoveries.
- Survival XP for natural ores, personal hostile kills, mature crops (including compatible right-click harvest/replant transitions), and fishing. Persistent category budgets, deduplication and source restrictions limit abuse.
- **88 connected skill nodes / 14 specializations**, readable branch views with previous/next controls, whole-web overview, panning/zoom, visible lock reasons, hover details and confirmed purchases. Starting class never locks a family.
- Wizard Arcane/Fire/Ice and Warlock Bladeslinger/Destruction/Affliction, plus Warrior/Rogue/Druid/Paladin paths. Warlock starts with CHA-based **Eldritch Blast**; Psychic Blade is an optional Bladeslinger unlock. Status effects, saves, finite barriers, sustain limits and cross-family bonuses affect combat.
- BASH / ABILITY / GOODS / DEFEND / RETREAT. Wizard starts with 40 base PP and Spark costs 2 before efficiency; Warlock has CHA-based melee combos and bounded PP recovery.
- Original forest map, clean obsidian panels and pixel controls, explicit ability scaling and stat modifiers. Combat shows the animated supplied enemy sprite on a plain stage; book art no longer sits behind text/navigation.
- Custom two-block altar: carved deepslate pedestal, gold arch and suspended amethyst crystal. Both halves open the RPG. Older single-block altars still work; re-place one to assemble the full model.
- Intro story, World 1 stage dialogue, boss payoff, collectible lore, and a server-confirmed Creative Reset Relic.

Shared four-heart parties, invitations/readiness and co-op encounter synchronization are **not implemented**. Players currently have individually authoritative battles, including on servers. No fake party UI is provided.

## Run and build

```powershell
.\gradlew.bat runClient
.\gradlew.bat clean build
```

On Linux/macOS use `./gradlew`. The normal build runs 31 JUnit tests and creates `build/libs/minebound-0.3.1.jar`. Install the same mod on server and clients (network protocol 4).

Place/craft an Altar with two blocks of vertical clearance or find it in Creative Functional Blocks. Create a character, select a map landmark, then press ENTER. WASD/arrows move the heart; Shift enables precision movement. Use the skill web's arrows to browse branches left to right, ALL for the overview, drag/scroll to pan/zoom, then select a node and confirm its purchase.

## Saves and migration

Authoritative files live in `<world>/data/minebound/players/<UUID>.json`, with atomic replacement and `.bak` retention. They are independent of Minecraft player inventory and XP. Server activity XP configuration is in `<world>/serverconfig/minebound-server.toml`.

On the integrated single-player host, an existing 0.2 local profile is imported from its previous per-world/UUID location if no server profile exists. Original files and legacy path ranks/abilities remain intact. Dedicated servers never accept a client-uploaded profile. Exported version-1 progress and version-2 profiles have explicit migration paths; corrupt/future saves are not silently reset.

See [rules and balance](docs/COMBAT_AND_CHARACTERS.md), [testing XP, recovery and reset](docs/TESTING.md), [server ownership and future parties](docs/NETWORKING.md), and [art generation prompts/provenance](art/UI_ART.md).

## In-game previews

These are screenshots from the automated Minecraft client scenario, not mockups.

![World 1 map](docs/screenshots/world-map.png)
![Connected skill web](docs/screenshots/skill-web.png)
![Character sheet](docs/screenshots/character-sheet.png)

More: [creation](docs/screenshots/creation.png), [battle](docs/screenshots/battle.png), [ability scaling](docs/screenshots/abilities.png), [two-block altar](docs/screenshots/altar.png), [reset confirmation](docs/screenshots/reset.png).
