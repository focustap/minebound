package com.focustap.minebound.rpg;

public final class CombatRules {
    private CombatRules() {}
    public static int bash(CharacterProfile p, int attackNumber) {
        int scaling = p.startingClass == CharacterClass.WARLOCK || SkillGraph.bonus(p,"pact") > 0
                ? Math.max(p.modifier(Attribute.STR), p.modifier(Attribute.CHA)) : p.modifier(Attribute.STR);
        int damage = 7 + scaling + p.rank(CharacterClass.WARRIOR) + SkillGraph.bonus(p,"bash");
        if (p.startingClass == CharacterClass.WARRIOR) damage += 2;
        if (p.startingClass == CharacterClass.ROGUE && attackNumber % 3 == 0)
            damage += 3 + Math.max(0, p.modifier(Attribute.DEX));
        if (attackNumber % 3 == 0) damage += SkillGraph.bonus(p,"precision");
        return Math.max(3, damage);
    }
    public static int abilityDamage(CharacterProfile p, Ability a) {
        if (a == Ability.WARD || a == Ability.MEND || a == Ability.BARRIER || a == Ability.BULWARK
                || a == Ability.REJUVENATE || a == Ability.MEDITATE) return 0;
        if (a == Ability.CANTRIP) return Math.max(3, 3 + p.modifier(Attribute.INT));
        int damage = 11 + 2 * p.modifier(a.scaling) + p.rank(a.path) + (SkillGraph.unlocks(p,a) ? 2 : 0);
        if (p.startingClass == CharacterClass.WIZARD && (a == Ability.SPARK || a == Ability.BRAMBLE)) damage += 2;
        if (a.scaling == Attribute.INT || a.scaling == Attribute.CHA || a.scaling == Attribute.WIS) damage += SkillGraph.bonus(p,"spell");
        if (a == Ability.ARCANE_MISSILES || a == Ability.EMBER_NOVA || a == Ability.RECKLESS_STRIKE) damage += 5;
        return Math.max(5, damage);
    }
    public static int healing(CharacterProfile p, Ability a) {
        int heal = switch (a) { case BRAMBLE -> 3; case WARD -> 5; case MEND -> 10; case DRAIN -> 6; default -> 0; };
        if (heal == 0) return 0;
        heal += Math.max(0, p.modifier(a == Ability.DRAIN ? Attribute.CHA : Attribute.WIS)) + p.rank(CharacterClass.DRUID) + SkillGraph.bonus(p,"heal")
                + (SkillGraph.unlocks(p,a) ? 2 : 0);
        if (p.startingClass == CharacterClass.DRUID) heal += 2;
        return p.race == Race.UNDEAD ? Math.max(1, heal * 3 / 4) : heal;
    }
    public static int incoming(CharacterProfile p, int raw, boolean guarding) {
        int damage = raw - Math.max(0, p.modifier(Attribute.CON)) / 2;
        if (guarding) damage = (damage + 1) / 2 - (p.startingClass == CharacterClass.PALADIN ? 1 : 0);
        return Math.max(2, damage);
    }
    /** CHA contributes to the finite rally bonus on DEFEND. */
    public static int rallyShield(CharacterProfile p) { return 1 + Math.max(0, p.modifier(Attribute.CHA)); }
}
