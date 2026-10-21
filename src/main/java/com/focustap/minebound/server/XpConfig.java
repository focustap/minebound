package com.focustap.minebound.server;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class XpConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue RATE, LEVEL_PENALTY;
    public static final ModConfigSpec.IntValue ORE_CAP, MOB_CAP, FARM_CAP, FISH_CAP;
    static {
        ModConfigSpec.Builder b=new ModConfigSpec.Builder();
        RATE=b.comment("Adventurer XP multiplier for survival activities; zero disables activity XP.").defineInRange("activityXpRate",1d,0d,10d);
        LEVEL_PENALTY=b.comment("Additional XP divisor per level beyond 1. Level thresholds remain quadratic.").defineInRange("levelXpPenalty",.025d,0d,1d);
        ORE_CAP=b.defineInRange("oreXpPerDay",120,0,100000);
        MOB_CAP=b.defineInRange("hostileXpPerDay",100,0,100000);
        FARM_CAP=b.defineInRange("farmingXpPerDay",60,0,100000);
        FISH_CAP=b.defineInRange("fishingXpPerDay",40,0,100000);
        SPEC=b.build();
    }
    private XpConfig() {}
}
