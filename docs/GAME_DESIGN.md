# Minebound: game design notes (draft v0.1)

## Core experience
Placing/right-clicking the altar opens a GUI. **No RPG levels are navigated in Minecraft space.** Stages are nodes in a Mario-inspired world map. Press a node to enter a sequence of turn-based encounters; the UI is the game. Stage 1-3 is a miniboss; 1-5 is the castle/world boss.

A new character gets **one skill point upon first activation** of an altar. Their character is not tied to Minecraft's combat attributes or inventory. Main progression is intended to reward regular Minecraft play **without importing equipment or items**. Actual XP sources and anti-AFK/automation abuse rules still need a balance decision.

## Menu commands
- **BASH:** Basic physical attack, improved by STR and relevant passive nodes.
- **ABILITY:** Use acquired active skills, consuming PP; broad possibilities across multiclass branches.
- **GOODS:** Use separate Minebound consumables; not Minecraft inventory.
- **DEFEND:** Sacrifice offense for reduced damage during the next bullet phase; tactical, not invincibility.
- **RETREAT:** Leave the stage, with loss of unfinished stage progress.

## Stats, multiclassing and passives
STR physical offense, DEX mobility/precision, CON HP/mitigation, INT offensive magic, WIS healing/warding, CHA support/debuffs. D20 checks/AC are reserved for enemy and support effects where useful; manual dodge should be more important than passive avoidance RNG.

No class selection lock. Begin at the center of an Enshrouded-inspired web; traverse Warrior, Rogue, Mage, Healer, Tank, and support nodes. Most investments are modest passives; about **five points spent in a path before unlocking a new major active skill**. Hybrid Rogue+Mage is a first-class build, not an exploit.

## Combat
1. Up to 4 players independently select BASH/ABILITY/GOODS/DEFEND/RETREAT.
2. Server resolves damage/status/PP/skills.
3. **One synchronized bullet arena** appears with one different-colored heart per party member.
4. All hearts move simultaneously via keyboard/controller input; clients render interpolated movements and visual effects.
5. Server validates movement and projectile collisions (lag compensation with conservative hitboxes).
6. HP changes separately for each character; round repeats.

Each damage pattern has telegraphs and a possible clean dodge; no unavoidable spam. Dexterity passives can gently influence movement or cooldowns without deleting the skill challenge. Consider support abilities that shield another heart and tank abilities that intercept projectiles.

## Multiplayer
Players invite or join voluntarily, select a map node, ready up, and only then enter. Never force nearby players into the encounter. Party leader selects stages; no one starts until everyone agrees. World map unlock and character skill builds are saved per player with co-op completion rules to decide.

## Visual art
Refer to ART_DIRECTION.md. Use original pixel portraits and pixel interface flourishes with a palette of plum/ink, parchment, indigo and fern green. We take mechanical inspiration from classic RPGs without copying copyrighted assets.

## MVP acceptance criteria
- Play on Minecraft 1.21.1 NeoForge.
- Altar is craftable, placeable, and opens GUI.
- Stage map routes straight to menus without teleporting anywhere.
- Original retro-styled BASH/ABILITY/GOODS/DEFEND/RETREAT battle GUI.
- Manual heart movement and collidable projectiles.
- Separate RPG stats; no vanilla gear influence.

**The current prototype is local and transient**: it meets only the visual/interaction subset, not persistence, multiplayer or all combat mechanics.
