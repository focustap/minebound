# Verification

Run `./gradlew build` (Windows: `.\gradlew.bat build`) on Java 21. JUnit tests use pure simulation and temporary profile directories, without launching Minecraft. The sprite generation task is reproducible and runs before resources are packaged.

Automated coverage:

- Creation gate, point-buy min/max/budget, one-time grant, all 20 race/class combinations, and exhaustive legal point-buy HP/PP ranges.
- Racial PP efficiency, Elf movement, Human budget, Undead fatal-hit survival and healing tradeoff.
- Attribute scaling, class passive differences, finite PP, healing, defensive mitigation, and CHA rally shielding.
- All five first clears, stage unlocks, repeat-reward rejection, levels, point spending, and learning another class's abilities.
- Persistent round trip, independent world/player files, backups, legacy migration, and rejection of corrupt/future data.
- Stationary hearts get hit and lose on every stage; cycling spawn coverage includes the whole arena; targeting locks at launch and prediction uses velocity.
- Boss phase changes and at least five commands per boss for every level-1 starting class.
- Swept collision, normalized diagonal speed, harmless telegraphs, open gate routes and a scripted clean spread dodge.
- Corners and center cannot remain safe for an entire phase; all 18 generated sprite frames have transparency, visible pixels and packaged resources.

A development Minecraft client was launched successfully on NeoForge 21.1.262 / Minecraft 1.21.1, including resource reload and renderer initialization. This is a startup smoke check, not an interactive altar playthrough.

## Required in-game acceptance pass

1. Create a fresh world, place/craft an altar, and verify stages cannot be entered before confirmation. Cancel, reopen, and try all race/class selections, back navigation and point changes; compare live HP/PP/damage with the final sheet.
2. Confirm with a custom name. Verify one skill point, immediate map access, character attributes, bonuses and ability descriptions. Test tooltips, keyboard focus, minimum GUI resolution, window resize and GUI scale changes.
3. Spend the initial skill point, exit Minecraft, relaunch the same world and confirm the build and name persist. Compare another world/player. Back up and restore a profile; verify invalid JSON displays an error without replacement.
4. Play 1-1 through 1-5 at intended levels using each class. Observe that stationary hearts are punished, outlines provide sufficient warning, shift movement is useful, all edges matter, and each new stage teaches its mechanic. Check for paths through overlapping gates and ground hazards, especially at arena corners and in Crownroot phase 3.
5. Confirm class abilities spend the advertised PP and use the correct stat; try insufficient PP, all racial effects, Mend, Bramble, Quick Cut, Ward and DEFEND. Undead should survive exactly one fatal hit per fight. Verify no goods or vanilla equipment affect RPG stats.
6. Trigger both boss transitions and check that changes are readable. Defeat, retreat, Escape-close and re-enter; no unfinished fight may grant rewards. Confirm first-clear XP/unlocks save, level points work, and replay does not re-award them.
7. Check animated frames for Wobbly Thing, Rabbick, Rudinn, Ponman and Jevil: no backgrounds or sheet labels should appear. Confirm transparency and crisp rendering on different GUI scales.
8. Attempt clean dodges across complete fights, including level-1 Crownroot. The automated fixtures establish specific escape routes and durability, not a global no-hit solvability proof. Tune cadence, warning duration and overlap from actual play results.

Future work: server-owned profiles and battle simulation, shared hearts/party readiness, a larger skill graph, additional worlds, reviewed Minecraft progression hooks, sound, goods and broader accessibility. No multiplayer acceptance is claimed by this change.
