package com.focustap.minebound.rpg;

public enum CharacterClass {
    WARRIOR("Warrior", Attribute.STR, 29, 8, "Training: BASH +2 damage.", "CLEAVE"),
    ROGUE("Rogue", Attribute.DEX, 25, 9, "Precision: every third BASH gains DEX damage.", "QUICK CUT"),
    WIZARD("Wizard", Attribute.INT, 25, 40, "Channeling: offensive spells +2 damage.", "SPARK"),
    DRUID("Druid", Attribute.WIS, 27, 11, "Growth: restorative spells heal 2 extra HP.", "BRAMBLE"),
    PALADIN("Paladin", Attribute.CON, 31, 8, "Bulwark: take 1 less damage while guarding.", "WARD"),
    WARLOCK("Warlock", Attribute.CHA, 27, 18, "Pact blade: third BASH restores 1 PP, up to 12/day.", "PSYCHIC BLADE");

    public final String label, passive, ability;
    public final Attribute primary;
    public final int baseHp, basePp;
    CharacterClass(String label, Attribute primary, int hp, int pp, String passive, String ability) {
        this.label = label; this.primary = primary; this.baseHp = hp; this.basePp = pp;
        this.passive = passive; this.ability = ability;
    }
}
