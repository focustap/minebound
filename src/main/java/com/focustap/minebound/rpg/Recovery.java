package com.focustap.minebound.rpg;

/** Monotonic Overworld gameTime, NOT dayTime: sleep and /time cannot mint recoveries. */
public final class Recovery {
    public static final long DAY = 24_000;
    private Recovery() {}
    public static boolean check(CharacterProfile p, long now, boolean inBattle) {
        if (!p.created) return false;
        if (p.lastRecoveryTick < 0 || now < p.lastRecoveryTick) { p.lastRecoveryTick = now; return false; }
        if (inBattle || now - p.lastRecoveryTick < DAY) return false;
        p.currentHp = p.maxHp(); p.currentPp = p.maxPp(); p.lastRecoveryTick = now;
        p.pactPpThisDay = 0;
        return true;
    }
    public static void initialize(CharacterProfile p, long now) {
        p.currentHp = p.maxHp(); p.currentPp = p.maxPp(); p.lastRecoveryTick = now;
        p.pactPpThisDay = 0;
    }
}
