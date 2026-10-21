package com.focustap.minebound.rpg;

public enum Ability {
    CLEAVE(CharacterClass.WARRIOR, Attribute.STR, 4, "Heavy physical strike."),
    QUICK_CUT(CharacterClass.ROGUE, Attribute.DEX, 3, "Precise physical strike; faster next dodge."),
    SPARK(CharacterClass.WIZARD, Attribute.INT, 2, "Offensive magic."),
    BRAMBLE(CharacterClass.DRUID, Attribute.WIS, 4, "Nature damage and a small heal."),
    WARD(CharacterClass.PALADIN, Attribute.WIS, 3, "Restore HP and halve next attack damage."),
    MEND(CharacterClass.DRUID, Attribute.WIS, 4, "Restore HP; consumes a turn."),
    PSYCHIC_BLADE(CharacterClass.WARLOCK, Attribute.CHA, 2, "Melee pact strike; primes the next BASH."),
    FIREBOLT(CharacterClass.WIZARD, Attribute.INT, 3, "Fire damage; burns for three command rounds."),
    FROSTBOLT(CharacterClass.WIZARD, Attribute.INT, 3, "Ice damage; slows the next dodge formation."),
    ARCANE_MISSILES(CharacterClass.WIZARD, Attribute.INT, 4, "Amplified arcane strike."),
    BARRIER(CharacterClass.WIZARD, Attribute.INT, 3, "Guard and gain an arcane shield for one phase."),
    EMBER_NOVA(CharacterClass.WARLOCK, Attribute.CHA, 5, "Destructive burst; refunds PP on a finishing blow."),
    HEX(CharacterClass.WARLOCK, Attribute.CHA, 3, "Curse deals damage over time and weakens the enemy."),
    DRAIN(CharacterClass.WARLOCK, Attribute.CHA, 3, "Life drain; also restores 1 PP (never a net gain)."),
    BACKSTAB(CharacterClass.ROGUE, Attribute.DEX, 3, "Precise strike; stronger against a debuffed enemy."),
    FEINT(CharacterClass.ROGUE, Attribute.DEX, 2, "Damage and weaken the next enemy attack."),
    BULWARK(CharacterClass.WARRIOR, Attribute.CON, 3, "Guard with a strong finite shield."),
    RECKLESS_STRIKE(CharacterClass.WARRIOR, Attribute.STR, 3, "Heavy strike; next phase deals +1 incoming damage."),
    REJUVENATE(CharacterClass.DRUID, Attribute.WIS, 5, "Three rounds of limited restorative healing."),
    SMITE(CharacterClass.PALADIN, Attribute.WIS, 4, "Holy damage and a modest shield."),
    CANTRIP(CharacterClass.WIZARD, Attribute.INT, 0, "Small free spell, unlocked deep in Arcane; consumes a turn."),
    MEDITATE(CharacterClass.WIZARD, Attribute.WIS, 0, "Once per recovery day, restore 4 PP and face an enemy turn.");

    public final CharacterClass path;
    public final Attribute scaling;
    public final int cost;
    public final String description;
    Ability(CharacterClass path, Attribute scaling, int cost, String description) {
        this.path = path; this.scaling = scaling; this.cost = cost; this.description = description;
    }
    public String label() { return name().replace('_', ' '); }
}
