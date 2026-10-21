package com.focustap.minebound.rpg;

import java.util.Arrays;
import java.util.List;

/** Domain state has no Minecraft/client dependencies; a future server can own it. */
public final class CharacterProfile {
    public static final int SCHEMA = 2;
    public boolean created;
    public String name = "Adventurer";
    public Race race = Race.HUMAN;
    public CharacterClass startingClass = CharacterClass.WARRIOR;
    public final int[] attributes = {8, 8, 8, 8, 8, 8};
    public final int[] paths = new int[5];
    public int xp, attributePoints, skillPoints, clearedMask;

    public int creationRemaining() {
        return race.budget() - Arrays.stream(attributes).map(v -> v - 8).sum();
    }
    public boolean allocateCreation(Attribute attribute, int delta) {
        int i = attribute.ordinal(), next = attributes[i] + delta;
        if (created || Math.abs(delta) != 1 || next < 8 || next > 14 || (delta > 0 && creationRemaining() <= 0)) return false;
        attributes[i] = next;
        return true;
    }
    public boolean confirm() {
        if (created || creationRemaining() != 0 || name == null || name.isBlank() || name.length() > 24
                || Arrays.stream(attributes).anyMatch(value -> value < 8 || value > 14)) return false;
        created = true;
        skillPoints = level();
        return true;
    }
    public int stat(Attribute attribute) {
        int bonus = race.bonus == attribute ? 1 : 0;
        if (startingClass.primary == attribute) bonus += 2;
        if (startingClass == CharacterClass.PALADIN && (attribute == Attribute.STR || attribute == Attribute.WIS)) bonus++;
        return attributes[attribute.ordinal()] + bonus;
    }
    public int modifier(Attribute attribute) { return Math.floorDiv(stat(attribute) - 10, 2); }
    public int rank(CharacterClass path) { return paths[path.ordinal()]; }
    public int level() {
        int level = 1;
        while (level < 20 && xp >= threshold(level + 1)) level++;
        return level;
    }
    public static int threshold(int level) { return 30 * (level - 1) * level; }
    public int maxHp() { return startingClass.baseHp + stat(Attribute.CON) - 8 + (level() - 1) * 2 + rank(CharacterClass.PALADIN); }
    public int maxPp() {
        return startingClass.basePp + Math.min(3, Math.max(0, modifier(Attribute.INT)) + Math.max(0, modifier(Attribute.WIS)))
                + (race == Race.ELF ? 1 : 0) + rank(CharacterClass.WIZARD);
    }
    public float speed() {
        return Math.min(2.85f, (2.05f + Math.max(-1, modifier(Attribute.DEX)) * .055f
                + rank(CharacterClass.ROGUE) * .05f) * (race == Race.ELF ? 1.04f : 1));
    }
    public int cost(Ability ability) { return Math.max(1, ability.cost - (race == Race.GNOME ? 1 : 0)); }
    public List<Ability> abilities() {
        return Arrays.stream(Ability.values()).filter(a -> a.path == startingClass || rank(a.path) >= 2).toList();
    }
    public boolean spendAttribute(Attribute a) {
        if (!created || attributePoints <= 0 || attributes[a.ordinal()] >= 18) return false;
        attributes[a.ordinal()]++; attributePoints--; return true;
    }
    public boolean spendSkill(CharacterClass path) {
        int invested = Arrays.stream(paths).sum();
        if (!created || skillPoints <= 0 || rank(path) >= 3 || (rank(path) >= 1 && invested < 2)) return false;
        paths[path.ordinal()]++; skillPoints--; return true;
    }
    public boolean unlocked(int stage) { return created && stage >= 0 && stage < 5 && (stage == 0 || (clearedMask & (1 << (stage - 1))) != 0); }
    /** First-clear awards only: no repeat-stage or vanilla-action XP farm. */
    public int victory(int stage) {
        if (!unlocked(stage) || (clearedMask & (1 << stage)) != 0) return 0;
        int before = level();
        clearedMask |= 1 << stage;
        int award = EnemyDefinition.WORLD_ONE.get(stage).xp();
        xp += award;
        int gained = level() - before;
        skillPoints += gained;
        attributePoints += gained * 2;
        return award;
    }
}
