# Minebound 0.3 rules and balance

## Preservation and ownership

This expansion descends from `0414feb` (the 0.2 overhaul). `Patterns.java` and the five World 1 enemy HP/damage/first-clear definitions are unchanged. The supplied source sheets remain intact. Battle commands now run on the server with validated movement inputs; profiles and resource changes are stored under the server world, not accepted from a client.

`rpg` contains character definitions, the skill graph, recovery/XP rules, combat calculations, narrative records and the existing battle simulation. `server/CharacterService` owns sessions and persistence. `server/ActivityXp` verifies Minecraft events and transitions; `Protocol` carries bounded intents and server snapshots. Client screens render those snapshots and send choices. Client-only wiring is dist-filtered and common altar/server classes never reference Minecraft rendering.

## Characters

Four races retain their previous effects: Human's extra creation point, Elf's +1 DEX/+4% movement/+1 PP, Gnome's +1 INT and spell-cost efficiency, and Undead's +1 CON/one fatal-hit survival per fight/25% less restorative healing.

Point buy still uses base 8–14, 12 points (13 Human), and the existing class/race bonuses. Starting identities are Warrior, Rogue, Wizard, Druid, Paladin and Warlock. New Warlock has 27 base HP, 18 base PP, +2 CHA, Psychic Blade and a third-BASH PP passive. Wizard retains 25 base HP but now has 40 base PP, Spark costs 2, and max PP grows by 4 per level. Warlock grows by 2 PP/level; others by 1. Capacity growth never refills current resources.

Warlock BASH scales from the higher of STR/CHA and can combo with Psychic Blade. Pact PP regeneration is limited to 12 per recovery day, persists across fights, and cannot be farmed indefinitely through retreat. Still Mind restores 4 PP once per recovery day, with its use preserved through logout, retreat and clock rollback. Life Drain and finishing refunds always spend at least 1 net PP after efficiency. Threadlight is a late Arcane free cantrip, separate from BASH and much weaker than paid spells.

The 0.2 HP formula, modest DEX speed adjustments, normalized diagonals, guard mitigation, CHA rally and finite PP remain. All legal creation allocations retain 25–40 HP. Martial/support PP remains 8–18; Warlock can reach 22 and Wizard 44 at creation.

## Persistent resources and goods

Battle starts read current HP/PP. Victory, retreat, altar reopening, level-up and node purchase do not refill them. A finished battle is settled once, so old snapshots cannot overwrite subsequent healing. An unfinished battle is abandoned on close/logout, preserving resources already spent or lost.

Every character receives one starting herb. First clears award one herb (two from Crownroot), with an ether from Thorn Knight and Crownroot. Herbs restore 12 HP (9 Undead); ethers restore 6 PP. Goods can be used on the map, including reviving a downed RPG character. In combat they consume a command and trigger a dodge phase. These goods never read or modify vanilla inventory.

Recovery uses Overworld `gameTime`, not `dayTime` or wall-clock time. At least 24,000 ticks must elapse after the last full recovery, regardless of dimension. Sleep and `/time set` alter daylight, not this eligibility. Recovery waits until the character leaves an active encounter and is announced in the interface. A clock rollback rebases the timer without granting recovery or refreshing restoration budgets. Repeated opens/reconnects cannot claim a second recovery.

## Connected web

There are 88 nodes across 14 specializations:

| Family | Branches |
|---|---|
| Warrior | Guardian, Berserker |
| Rogue | Assassin, Trickster |
| Wizard | Arcane, Fire, Ice |
| Druid | Nature, Restoration |
| Paladin | Holy Defender, Retribution |
| Warlock | Psychic Blade, Destruction, Affliction |

Each main branch has five attribute/passive nodes before an active. Nodes require a purchased connection; lateral bridges at the middle of branches provide hybrid routes. Major actives also require five purchased passives globally. Additional deep actives cost two points. Every family is accessible from the common root; initial viewport focus follows the starting class. Previously purchased 0.2 path ranks/abilities continue to work alongside the new graph.

Effects include attributes, BASH/precision/spell damage, HP/PP capacity, efficiency, finite shields, movement, healing, status resistance, pact combos, curse leech and damage against afflicted enemies. Burn/curse deal timed damage; chill slows projectile advancement modestly; weakness reduces attack damage; rejuvenation has a finite three-round duration. Enemy saves use a deterministic d20 calculation and the relevant casting attribute. Later encounters can test WIS/Resolve against Dread. Manual dodging determines whether ordinary projectiles hit; there is no random passive dodge roll.

The graph is functional but still an initial content/balance set. Multi-enemy explosions, curse spreading and a more extensive combinatorial spell system need future multi-enemy encounters; no decorative inactive nodes claim those mechanics.

## Adventurer progression

Level L requires total XP `30 * (L - 1) * L`; level-ups grant one skill point and two attribute points. First clears still award 60/90/120/150/240 XP, once each. Survival activity XP is a separate source and never consumes vanilla XP.

| Activity | Default raw award | Default per-player 24,000-tick budget |
|---|---|---|
| Natural ore | 2 common, 3 iron/lapis/redstone, 5 gold, 12 diamond, 15 emerald, 20 ancient debris | 120 XP |
| Personal hostile kill | 3 normal, 5 tougher, 12 boss-health targets | 100 XP |
| Verified mature crop harvest | 2 | 60 XP |
| Fishing | 3 fish / 5 other catches | 40 XP |

Awards are scaled by `activityXpRate / (1 + (level - 1) * levelXpPenalty)`, with default penalty .025. Budgets, event cooldowns and duplicate records persist per player; ore placement/mining history is world-owned. Stones, generators, fake players, Creative, spectators, tagged spawner/dispenser mobs, and remote kills are excluded. Fishing requires a normal server-observed cast, minimum cast duration, recent movement and a catch cooldown. These restrictions limit farms but are not a claim that software can distinguish every human from a sophisticated bot.

Mature crops are captured at highest-priority right-click interaction, including interactions a harvesting mod later cancels as handled. The following server tick verifies mature-to-younger/removed crop state. Repeated clicks without a transition award nothing, and break/right-click events share position cooldowns. Vanilla `CropBlock`, crop tags and conventional named crop age properties are supported. Mods with delayed/nonstandard crop storage require specific compatibility work.
