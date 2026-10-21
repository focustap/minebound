package com.focustap.minebound.rpg;

public enum Race {
    HUMAN("Human", null, "Adaptable: +1 point to spend at creation."),
    ELF("Elf", Attribute.DEX, "Nimble: +1 DEX, +4% heart speed, +1 PP."),
    GNOME("Gnome", Attribute.INT, "Resourceful: +1 INT; spells cost 1 less PP (min 1)."),
    UNDEAD("Undead", Attribute.CON, "Unyielding: +1 CON; survive one fatal hit per battle; healing -25%.");

    public final String label;
    public final Attribute bonus;
    public final String passive;
    Race(String label, Attribute bonus, String passive) {
        this.label = label; this.bonus = bonus; this.passive = passive;
    }
    public int budget() { return this == HUMAN ? 13 : 12; }
}
