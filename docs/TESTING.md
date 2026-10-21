# Testing Minebound 0.3

## Automated checks

```powershell
.\gradlew.bat clean build
```

The normal build runs **29 JUnit tests**: creation and all race/class combinations, exhaustive legal creation resource ranges, first-clear XP/leveling, legacy data, current-resource reloads, one-time/rollback-safe recovery, duplicate activity awards and budgets, graph connectivity/hybrids, Wizard multi-encounter resource balance, Warlock sustain limits, net spell costs, reset aggregate reload, targeting/camping, boss phases/durability, collision and sprite gutters.

An opt-in **real Minecraft client/integrated-server scenario** creates its own throwaway world and operates the actual screens and network handlers:

```powershell
.\gradlew.bat -PmineboundSmoke runClient
```

It verifies altar opening, story/creation and the single initial point, a graph purchase, server-resolved spell use, resource carryover after closing/reopening, eligible recovery, natural-ore event XP, personal hostile-kill XP, right-click harvest/replant deduplication, the normal fishing event path, disk reload, victory without refilling, out-of-combat goods after settlement, reset cancellation, full reset reload, and vanilla inventory isolation. The right-click and fishing fixtures post the corresponding server events rather than wait for random catches or install a third-party harvesting mod. A failing assertion makes the Gradle task fail. QA sources are excluded from normal builds.

Captured screenshots are written to `run/screenshots/minebound-*.png`; selected previews are checked into `docs/screenshots`. The full integrated scenario passed during this change. A dedicated `--initSettings` bootstrap also completed without a client-class error:

```powershell
.\gradlew.bat -PmineboundServerSmoke runServer
```

That last check initializes dedicated-server settings and exits; it does not validate remote gameplay or start a production world. GitHub Actions builds Java 21/NeoForge and runs JUnit, not the graphical scenario. Its commit-specific result is reported in the expansion PR.

## Verify preservation and saves

1. Back up a test world. Existing 0.2 host-local profiles should import automatically into `<world>/data/minebound/players/<UUID>.json`, retaining name, race, class, attributes, XP, old path bonuses/abilities and cleared stages. The old local files remain intact. Dedicated servers never import an arbitrary client save.
2. Create a new character through the altar, then reopen it. It must open the existing map and grant no extra point or goods.
3. Spend points, use a spell and take damage. Retreat, reopen, die/respawn in Minecraft, logout/rejoin and restart the server. Character state and current resources must remain; vanilla gear must not affect RPG damage.
4. First clears must unlock the next stage, grant XP/goods/lore once and save. Replay grants no repeat first-clear award. The original five World 1 enemies, HP/damage definitions and `Patterns.java` are retained.

## Test activity XP

Create the character first, then use **Survival Mode with a real player**. Creative, spectator and fake players intentionally receive no activity XP. Check the character sheet's Adventurer XP; vanilla XP is separate. Rewards are aggregated into a subtle notice every five seconds.

- **Mining:** mine naturally generated ore with a suitable tool. Diamond/emerald should award more than coal/copper. Mine plain stone: no XP. Place an ore block and break it: no XP. Replaced or already-rewarded ore positions remain excluded across reconnects and resets.
- **Combat:** personally kill a nearby natural hostile mob (a summoned zombie is suitable for an admin fixture). Mob farms tagged as spawner/dispenser-origin, indirect automation/fake players and remote kills should not award. Category budgets bound repeated kills.
- **Farming:** break mature wheat/carrot/potato/beetroot; immature crops give no XP. With a compatible right-click harvest/replant mod installed, harvest a mature crop by right click and verify one reward after its state changes. Repeated clicks without a mature-to-younger/removed transition give none. Test cancel-as-handled harvesting, and ensure break plus interaction callbacks do not double-count.
- **Fishing:** cast and catch normally after moving recently. The cast must last at least 80 ticks; catch rewards have a 200-tick cooldown, a daily cap and recent-movement requirement. Fake/autonomous rod owners are excluded.
- **Rates:** adjust `<world>/serverconfig/minebound-server.toml` in a test world. `activityXpRate=0` disables activity awards. Rates, level penalty and each category cap are configurable. Reconnect/restart must preserve consumed category budgets.

Generic right-click state-transition compatibility is implemented and covered by the integrated fixture. Specific third-party farming mods with deferred harvests, tile-entity-only maturity or nonstandard state properties still need compatibility tests/adapters. The detector checks the next few server ticks; it does not infer a harvest from a click alone.

## Test recovery and resources

1. Record current HP/PP and `lastRecoveryTick` in a test character. Spend resources and enter another stage: resources must carry unchanged into the new fight. A first-clear victory does not heal.
2. Reopen the altar repeatedly, reconnect, switch dimension, sleep and use `/time set`: none should award early recovery. Recovery uses Overworld `gameTime`, not daylight or wall-clock time.
3. Allow 24,000 Overworld simulation ticks to pass (approximately 20 minutes at 20 TPS). When outside an active encounter, the next recovery check restores both resources and displays a notice. A second immediate check must not restore anything again.
4. If the interval becomes eligible during a fight, recovery waits until the encounter is closed/settled. Paused/stopped single-player time does not advance the interval.
5. Use a map herb after a completed/failed battle: its healing must remain saved rather than being replaced by the old battle snapshot. Check Undead's healing reduction.
6. Wizard: try Arcane efficiency and INT improvements over consecutive first clears, using earned ethers strategically. The resource simulation verifies a spell-focused World 1 route without daily rest. Warlock: compare CHA-based BASH, Psychic Blade's next-BASH combo and the finite 12-PP daily pact budget. Retreat/re-entry must not refresh Still Mind or that budget.

## Test the Reset Relic

1. In a test world, enter Creative and obtain the Reset Relic from Functional Blocks or `/give @s minebound:reset_relic`.
2. Ordinary use and Survival use must do nothing. **Sneak + right-click in Creative** opens a confirmation lasting 20 seconds.
3. Cancel first; the character must remain. Reopen, confirm, and verify return to the opening story/creation.
4. Check all RPG identity, attributes, XP, nodes/legacy ranks, points, abilities, HP/PP, recovery/restoration tracking, goods, stage records and lore are reset. Logout/restart: the reset profile must load and require creation.
5. Compare Minecraft inventory, equipped gear, vanilla XP and advancements before/after. They must be untouched. Other players cannot be targeted by the reset request. Backups are retained for administrator recovery.

## Remaining acceptance work

- Remote dedicated-server play, real network latency, disconnect/respawn edge cases and multiple simultaneous individual sessions need broader testing. Full voluntary parties/readiness/shared hearts are not implemented.
- Playtest the entire World 1 sequence across all 24 race/class combinations and diverse hybrid routes. The core patterns are retained; status/skill interactions and the new persistent resource economy need human tuning.
- Verify all supported GUI scales, long names, keyboard narration, panning/zooming, and frames across the five supplied sheets on additional displays. Screenshots verify the primary development display, not every renderer.
- Multi-enemy area damage, curse spreading, more worlds, deeper spell combinations, audio feedback and fuller accessibility remain future content. Every visible purchasable node in this implementation has a working effect.
