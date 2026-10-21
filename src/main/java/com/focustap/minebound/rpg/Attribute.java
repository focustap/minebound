package com.focustap.minebound.rpg;

public enum Attribute {
    STR("Physical damage and CLEAVE. +1 BASH damage per two STR."),
    DEX("Heart speed, QUICK CUT, and Rogue's third-hit precision bonus."),
    CON("+1 max HP per CON. One damage reduction per four CON above 10."),
    INT("SPARK damage and maximum PP. Spell damage +2 per two INT."),
    WIS("BRAMBLE damage, restorative healing, WARD, and maximum PP."),
    CHA("Warlock spell damage +2 per modifier. Pact BASH uses higher STR/CHA. Improves Drain healing, spell saves and DEFEND shield.");
    public final String description;
    Attribute(String description) { this.description = description; }
}
