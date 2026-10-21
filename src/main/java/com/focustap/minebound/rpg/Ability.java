package com.focustap.minebound.rpg;

public enum Ability {
    CLEAVE(CharacterClass.WARRIOR, Attribute.STR, 4, "Heavy physical strike."),
    QUICK_CUT(CharacterClass.ROGUE, Attribute.DEX, 3, "Precise physical strike; faster next dodge."),
    SPARK(CharacterClass.WIZARD, Attribute.INT, 4, "Offensive magic."),
    BRAMBLE(CharacterClass.DRUID, Attribute.WIS, 4, "Nature damage and a small heal."),
    WARD(CharacterClass.PALADIN, Attribute.WIS, 3, "Restore HP and halve next attack damage."),
    MEND(CharacterClass.DRUID, Attribute.WIS, 4, "Restore HP; consumes a turn.");

    public final CharacterClass path;
    public final Attribute scaling;
    public final int cost;
    public final String description;
    Ability(CharacterClass path, Attribute scaling, int cost, String description) {
        this.path = path; this.scaling = scaling; this.cost = cost; this.description = description;
    }
    public String label() { return name().replace('_', ' '); }
}
