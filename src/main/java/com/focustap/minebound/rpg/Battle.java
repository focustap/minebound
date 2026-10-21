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
    public Battle(CharacterProfile profile, int stage) {
        if (!profile.unlocked(stage)) throw new IllegalArgumentException("Create a character and unlock this stage first.");
        this.profile = profile; this.stage = stage; enemy = EnemyDefinition.WORLD_ONE.get(stage);
        hp = profile.maxHp(); pp = profile.maxPp(); enemyHp = enemy.hp();
        message = enemy.name() + " blocks your path!";
    }
    public int phase() { return enemy.phase(enemyHp); }
    public boolean bash() {
        if (state != State.COMMAND) return false;
        int damage = CombatRules.bash(profile, ++bashCount);
        enemyHp = Math.max(0, enemyHp - damage);
        message = "BASH: " + damage + " damage.";
        next(); return true;
    }
    public boolean ability(Ability a) {
        if (state != State.COMMAND || !profile.abilities().contains(a)) return false;
        int cost = profile.cost(a);
        if (pp < cost) { message = "Need " + cost + " PP for " + a.label() + "."; return false; }
        pp -= cost;
        int damage = CombatRules.abilityDamage(profile, a), healing = CombatRules.healing(profile, a);
        enemyHp = Math.max(0, enemyHp - damage);
        hp = Math.min(profile.maxHp(), hp + healing);
        guarding = a == Ability.WARD;
        quick = a == Ability.QUICK_CUT;
        message = a.label() + ": " + damage + " damage, " + healing + " healing.";
        next(); return true;
    }
    public boolean defend() {
        if (state != State.COMMAND) return false;
        guarding = true; shield = CombatRules.rallyShield(profile);
        message = "Guarding. Rally absorbs " + shield + " damage.";
        next(); return true;
    }
    public void retreat() { state = State.RETREATED; }
    private void next() {
        if (enemyHp <= 0) { state = State.WON; return; }
        state = State.DODGE; tick = 0; iframe = 0; bullets.clear();
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
            b.tick();
            if (iframe == 0 && b.hits(heartX, heartY)) {
                int damage = CombatRules.incoming(profile, enemy.damage() + phase() - 1, guarding);
                int absorbed = Math.min(shield, damage); shield -= absorbed; damage -= absorbed;
                hp = Math.max(0, hp - damage);
                if (hp == 0 && profile.race == Race.UNDEAD && !unyieldingUsed) {
                    hp = 1; unyieldingUsed = true;
                }
                iframe = 18;
                if (hp == 0) { state = State.LOST; return; }
            }
        }
        bullets.removeIf(Projectile::expired);
        if (tick >= Patterns.LENGTH) {
            turn++; guarding = false; quick = false; shield = 0; bullets.clear(); state = State.COMMAND;
            message = "Choose your next command.";
        }
    }
}
