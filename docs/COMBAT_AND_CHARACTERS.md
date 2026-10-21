# Character and combat implementation

## Ownership and modules

`rpg/CharacterProfile` owns a character's race/class, six base attributes, five path ranks, XP, unspent points and cleared-stage bitset. `Race`, `CharacterClass`, `Ability`, and `EnemyDefinition` are extensible definitions. `CombatRules` contains damage, healing and mitigation formulas. `Battle` runs a 20-tick simulation without Minecraft dependencies; `Patterns` generates constant-velocity `Projectile` objects with delayed activation. `ProfileStore` handles versioned JSON and atomic local writes. `CharacterCreationScreen`, `RetroScreen`, `MineboundScreen`, and `EnemySprites` handle presentation and input.

The simulation is local single-player. It accepts commands and normalized movement rather than reading keys or widgets; these boundaries can later be owned by a server. No networking, party readiness, synchronized hearts, or trusted multiplayer persistence is claimed.

## Creation and resources

Base attributes start at 8. Spend 12 points (13 for Human), one per increase, up to 14 before bonuses. Race adds +1 DEX/INT/CON for Elf/Gnome/Undead. Starting class adds +2 to its primary attribute; Paladin also adds +1 STR and WIS. Displayed stats include those bonuses. Attribute modifier is `floor((stat - 10) / 2)`.

| Class | Base HP | Base PP | Starting ability | Working starting passive |
|---|---:|---:|---|---|
| Warrior | 29 | 8 | Cleave (STR) | BASH +2 |
| Rogue | 25 | 9 | Quick Cut (DEX) | Every third BASH gains 3 + positive DEX modifier damage |
| Wizard | 25 | 14 | Spark (INT) | Offensive spells +2 |
| Druid | 27 | 11 | Bramble, Mend (WIS) | Restorative spells heal +2 |
| Paladin | 31 | 8 | Ward (WIS) | Guarding reduces incoming damage by another 1 |

Max HP is base HP + CON - 8 + 2 per level beyond 1 + Paladin path rank. Max PP is base PP + combined positive INT/WIS modifiers (capped at 3) + Elf's 1 PP + Wizard path rank. Every legal creation allocation stays within **25–40 HP and 8–18 PP**, exhaustively tested. New encounters restore resources; PP never regenerates during a fight.

Elf adds 4% movement; Gnome reduces every ability's cost by 1 (minimum 1). Undead survives a fatal hit at 1 HP once per battle and receives 25% less restorative healing. Human's additional point is spent during creation.

## Combat

BASH is 7 + STR modifier + Warrior path rank, with class passive adjustments. Offensive abilities deal 11 + 2 times their scaling modifier + their path rank, with Wizard's spell passive where applicable. Bramble also heals; Mend is a dedicated heal; Ward heals and guards; Quick Cut adds 8% heart speed during the next dodge phase. Ability costs are 3–4 PP before Gnome efficiency.

CON reduces incoming damage by one per four CON above 10. Guard halves the result; Paladin's passive applies afterward. Minimum guarded damage is 2 before the finite rally shield. DEFEND's rally absorbs 1 + positive CHA modifier damage across that phase. Healing uses WIS and Druid rank. Spending a turn to defend never restores PP.

Heart speed starts at 2.05 logical units/tick, adjusts modestly with DEX, Elf and Rogue path, and is capped at 2.85. Diagonal input is normalized, and Shift multiplies speed by .55. Collision uses a three-unit heart hit radius and swept bullet segments. A hit grants 18 ticks of invulnerability. No random passive evasion or homing projectiles.

## World 1

| Stage | Enemy | HP | Base hit damage | First-clear XP | Pattern identity |
|---|---|---:|---:|---:|---|
| 1-1 | Moss Sprite | 42 | 6 | 60 | Aimed three-shot spreads from cycling edges |
| 1-2 | Briarling | 57 | 7 | 90 | Crossfire alternating with aimed spreads |
| 1-3 | Thorn Knight | 108 | 8 | 120 | Rotating aimed, crossfire and gate formations |
| 1-4 | Mire Oracle | 82 | 8 | 150 | Predictive spreads/waves alternating with marked ground |
| 1-5 | Crownroot | 186 | 9 | 240 | All formations; phase 2 adds gates, phase 3 adds ground hazards |

Boss phase changes occur at two-thirds and one-third HP. Each phase increases damage by 1 and slightly increases shot speed/cadence. Pattern selection rotates with turn, segment, and boss phase. Every dodge phase lasts 204 ticks, with a 24-tick initial grace interval. Gates provide 48 ticks of warning and at least a 48-unit nominal opening; ground hazards mark the heart's position for 28 ticks before activation. Spreads lock aim at launch; occasional predictive shots lead recent velocity by 12 ticks. Hollow outlines are harmless warnings. Patterns stop spawning near the end of the phase.

Automated checks demonstrate gap routes and a clean dodge against aimed spreads, and reject stationary camping at sampled arena positions. They do not constitute a proof that every dynamically targeted boss sequence is avoidable. Full fights require skilled in-game tuning.

## Progression and multiclassing

Confirmation grants one skill point. Level L requires total XP `30 * (L - 1) * L` (60 / 180 / 360 / 600 for levels 2–5). Every gained level gives two attribute points and one skill point. Base attributes can be improved to 18. All five first-clear rewards total 660 XP, reaching level 5. Replay grants no XP, points, or additional unlock rewards. No Minecraft-action XP hook is installed.

Every path is open from the common root. Each has three ranks, with a second rank requiring two investments anywhere in the web. Rank 2 teaches that path's ability (Druid also teaches Mend); the starting class already knows its own abilities. Rank 3 strengthens passives and ability scaling. The permanent starting passive remains tied to the original class.

Per rank: Warrior gives +1 BASH, Rogue +.05 movement, Wizard +1 PP, Druid +1 restorative healing, Paladin +1 HP. This is a small connected foundation, not a finished large skill tree. More worlds, nodes and earned XP sources can expand it later.
