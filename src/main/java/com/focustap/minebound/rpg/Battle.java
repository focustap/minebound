package com.focustap.minebound.rpg;

import java.util.ArrayList;
import java.util.List;

/** Single-player simulation. No rendering, widgets, key polling, or file IO. */
public final class Battle {
    public enum State { COMMAND, DODGE, WON, LOST, RETREATED }
    public final CharacterProfile profile;
    public final EnemyDefinition enemy;
    public final int stage;
    public final List<Projectile> bullets = new ArrayList<>();
    public State state = State.COMMAND;
    public int hp, pp, enemyHp, turn = 1, tick, iframe;
    public float heartX = Patterns.WIDTH / 2f, heartY = Patterns.HEIGHT / 2f;
    public String message;
    public boolean unyieldingUsed;
    private boolean guarding, quick;
    private int shield, bashCount;
    public int burnRounds, curseRounds, chilledRounds, weakenedRounds, rejuvenationRounds;
    public boolean meditationUsed;
    private int bladeCombo, reckless, dread;
    public Battle(CharacterProfile profile, int stage) {
        if (!profile.unlocked(stage)) throw new IllegalArgumentException("Create a character and unlock this stage first.");
        this.profile = profile; this.stage = stage; enemy = EnemyDefinition.WORLD_ONE.get(stage);
        if (profile.currentHp <= 0) throw new IllegalArgumentException("You are down. Recover with a Minebound good or daily rest.");
        hp = Math.clamp(profile.currentHp, 0, profile.maxHp()); pp = Math.clamp(profile.currentPp, 0, profile.maxPp()); enemyHp = enemy.hp();
        message = enemy.name() + " blocks your path!";
    }
    public int phase() { return enemy.phase(enemyHp); }
    public boolean bash() {
        if (state != State.COMMAND) return false;
        int damage = CombatRules.bash(profile, ++bashCount) + bladeCombo + afflictedBonus(); bladeCombo = 0;
        if (bashCount % 3 == 0 && profile.pactPpThisDay < 12 && pp < profile.maxPp()
                && (profile.startingClass == CharacterClass.WARLOCK || SkillGraph.bonus(profile,"pact") > 0)) {
            pp++; profile.pactPpThisDay++;
        }
        enemyHp = Math.max(0, enemyHp - damage);
        message = "BASH: " + damage + " damage.";
        next(); return true;
    }
    public boolean ability(Ability a) {
        if (state != State.COMMAND || !profile.abilities().contains(a)) return false;
        int cost = profile.cost(a);
        if (a == Ability.MEDITATE && (meditationUsed || profile.meditatedThisDay)) {
            message = "Still Mind was used during this recovery day."; return false;
        }
        if (pp < cost) { message = "Need " + cost + " PP for " + a.label() + "."; return false; }
        pp -= cost;
        int damage = CombatRules.abilityDamage(profile, a), healing = CombatRules.healing(profile, a);
        if (damage > 0) damage += afflictedBonus();
        if (a == Ability.BACKSTAB && (curseRounds > 0 || weakenedRounds > 0 || chilledRounds > 0)) damage += 5;
        enemyHp = Math.max(0, enemyHp - damage);
        hp = Math.min(profile.maxHp(), hp + healing);
        guarding = a == Ability.WARD || a == Ability.BARRIER || a == Ability.BULWARK;
        if (a == Ability.BARRIER || a == Ability.BULWARK || a == Ability.SMITE) shield += 3 + Math.max(0,profile.modifier(a.scaling)) + (SkillGraph.unlocks(profile,a) ? 2 : 0);
        quick = a == Ability.QUICK_CUT;
        boolean saved = enemySave(a);
        if (a == Ability.FIREBOLT && !saved) burnRounds = 3;
        if (a == Ability.FROSTBOLT && !saved) chilledRounds = 2;
        if (a == Ability.HEX && !saved) { curseRounds = 3; weakenedRounds = 3; }
        if (a == Ability.FEINT && !saved) weakenedRounds = 2;
        if (a == Ability.REJUVENATE) rejuvenationRounds = 3;
        if (a == Ability.RECKLESS_STRIKE) reckless = 1;
        if (a == Ability.PSYCHIC_BLADE) bladeCombo = 3 + Math.max(0,profile.modifier(Attribute.CHA)) + SkillGraph.bonus(profile,"pact");
        if (a == Ability.DRAIN) pp = Math.min(profile.maxPp(),pp + Math.min(1,Math.max(0,cost-1)));
        if (a == Ability.MEDITATE) { meditationUsed = true; profile.meditatedThisDay = true; profile.meditationRecoveryTick = profile.lastRecoveryTick; pp = Math.min(profile.maxPp(),pp + 4); }
        if (a == Ability.EMBER_NOVA && enemyHp == 0) pp = Math.min(profile.maxPp(),pp + Math.min(2,Math.max(0,cost-1)));
        message = a.label() + ": " + damage + " damage, " + healing + " healing.";
        next(); return true;
    }
    public boolean defend() {
        if (state != State.COMMAND) return false;
        guarding = true; shield = CombatRules.rallyShield(profile);
        message = "Guarding. Rally absorbs " + shield + " damage.";
        next(); return true;
    }
    public void retreat() { commitResources(); state = State.RETREATED; }
    public void commitResources() { profile.currentHp = hp; profile.currentPp = pp; }
    private int afflictedBonus() { return (curseRounds > 0 || burnRounds > 0 || weakenedRounds > 0 || chilledRounds > 0) ? SkillGraph.bonus(profile,"vulnerable") : 0; }
    /** Repeatable d20 save, independent of wall-clock/RNG and client input. */
    private boolean enemySave(Ability a) {
        int roll = Math.floorMod(stage * 7 + turn * 11 + a.ordinal() * 3,20) + 1;
        return roll + phase() >= 12 + profile.modifier(a.scaling);
    }
    public boolean good(String id) {
        if (state != State.COMMAND || !useGood(profile,id)) return false;
        hp = profile.currentHp; pp = profile.currentPp;
        message = "Used " + id + "."; next(); return true;
    }
    public static boolean useGood(CharacterProfile p, String id) {
        if (!p.created || p.goods.getOrDefault(id,0) <= 0) return false;
        if (id.equals("herb") && p.currentHp < p.maxHp()) {
            int heal = p.race == Race.UNDEAD ? 9 : 12; p.currentHp = Math.min(p.maxHp(),p.currentHp + heal);
        } else if (id.equals("ether") && p.currentPp < p.maxPp()) p.currentPp = Math.min(p.maxPp(),p.currentPp + 6);
        else return false;
        p.goods.compute(id,(key,count) -> count - 1); return true;
    }
    private void next() {
        commitResources();
        if (enemyHp <= 0) { state = State.WON; return; }
        state = State.DODGE; tick = 0; iframe = 0; bullets.clear();
        shield += SkillGraph.bonus(profile,"shield");
        dread = 0;
        if (stage >= 3 && turn % 3 == 0) {
            int save = Math.floorMod(turn * 7 + stage,20) + 1 + profile.modifier(Attribute.WIS) + SkillGraph.bonus(profile,"resist");
            if (save < 12) { dread = 1; message += " Dread: failed WIS save (+1 incoming)."; }
        }
        // Position carries between rounds: no teleporting into live hazards.
    }
    public void update(float inputX, float inputY, boolean focus) {
        if (state != State.DODGE) return;
        float length = (float)Math.sqrt(inputX * inputX + inputY * inputY);
        if (length > 1) { inputX /= length; inputY /= length; }
        float speed = profile.speed() * (quick ? 1.08f : 1) * (focus ? .55f : 1);
        float oldX = heartX, oldY = heartY;
        heartX = Math.clamp(heartX + inputX * speed, 6, Patterns.WIDTH - 6);
        heartY = Math.clamp(heartY + inputY * speed, 6, Patterns.HEIGHT - 6);
        tick++;
        if (iframe > 0) iframe--;
        Patterns.spawn(bullets, stage, turn, phase(), tick, heartX, heartY, heartX - oldX, heartY - oldY);
        for (Projectile b : bullets) {
            if (chilledRounds <= 0 || tick % 5 != 0 || b.delay > 0) b.tick();
            if (iframe == 0 && b.hits(heartX, heartY)) {
                int damage = CombatRules.incoming(profile, enemy.damage() + phase() - 1 + reckless + dread - (weakenedRounds > 0 ? 1 : 0), guarding);
                int absorbed = Math.min(shield, damage); shield -= absorbed; damage -= absorbed;
                hp = Math.max(0, hp - damage);
                if (hp == 0 && profile.race == Race.UNDEAD && !unyieldingUsed) {
                    hp = 1; unyieldingUsed = true;
                }
                iframe = 18;
                commitResources();
                if (hp == 0) { state = State.LOST; return; }
            }
        }
        bullets.removeIf(Projectile::expired);
        if (tick >= Patterns.LENGTH) {
            if (burnRounds > 0) { enemyHp = Math.max(0,enemyHp - 3); burnRounds--; }
            if (curseRounds > 0) {
                enemyHp = Math.max(0,enemyHp - 2); curseRounds--;
                if (SkillGraph.bonus(profile,"drain") > 0) hp = Math.min(profile.maxHp(),hp + 1);
            }
            if (chilledRounds > 0) chilledRounds--;
            if (weakenedRounds > 0) weakenedRounds--;
            if (rejuvenationRounds > 0) { hp = Math.min(profile.maxHp(),hp + (profile.race == Race.UNDEAD ? 2 : 3)); rejuvenationRounds--; }
            reckless = 0; commitResources();
            if (enemyHp == 0) { state = State.WON; return; }
            turn++; guarding = false; quick = false; shield = 0; bullets.clear(); state = State.COMMAND;
            message = "Choose your next command.";
        }
    }
}
