# Minebound

A menu-based RPG for **Minecraft 1.21.1 / NeoForge 21.1.262**, accessed through the Minebound Altar. No physical dungeons, vanilla equipment imports, or Minecraft combat changes.

Version 0.2 implements persistent **local single-player** characters, strategic commands, and manual bullet dodging. Shared multiplayer arenas and server authority remain future work.

## Play and build

Java 21 is required. The checked-in Gradle 9.2.1 wrapper downloads its own Gradle distribution.

```powershell
.\gradlew.bat runClient
.\gradlew.bat build
```

On Linux/macOS use `./gradlew`. The mod jar is written to `build/libs/minebound-0.2.0.jar`; tests are included in `build`.

Find the Altar in Functional Blocks, or craft it with cobblestone, redstone and an amethyst shard. A new character must choose a race, starting class, point-buy attributes, and confirm their sheet before entering stages. Existing characters open the world map.

- Four races: Human, Elf, Gnome, Undead; all racial effects work in combat.
- Five starting classes: Warrior, Rogue, Wizard, Druid, Paladin. Every class can invest in every path.
- Six attributes, live build previews, character sheet, ability summaries, and a common-root skill web.
- BASH, ability selection, DEFEND and RETREAT. WASD/arrows move the heart; Shift enables precision movement.
- Five distinct World 1 encounters, including three-phase Thorn Knight and Crownroot fights.
- First-clear XP, levels, attribute and skill points; repeat clears give no farmable XP.
- Your five supplied sprite sheets are stored intact in `art/source/world1`; the build generates transparent animated combat frames.

## Saves

Profiles live under `<Minecraft game directory>/config/minebound/profiles/<world-or-server hash>/<player UUID>.json`. They are isolated per local world or server address and UUID. Confirmation, investments, and victory save immediately; writes retain a previous `.bak`. Reopening an altar or restarting the game restores the character. Each encounter starts rested; unfinished battles are not resumable and closing a battle counts as retreat.

The old prototype only kept characters in memory and cannot recover that lost state. A version-1 exported progress file can be migrated with its stage unlocks preserved, derived XP/earned points, a `.v1.bak`, and mandatory character creation. Corrupt or future-version files produce a visible error instead of being reset.

See [combat and progression rules](docs/COMBAT_AND_CHARACTERS.md), [verification and in-game checklist](docs/TESTING.md), [sprite provenance and mapping](art/source/world1/README.md), and [future networking](docs/NETWORKING.md).
