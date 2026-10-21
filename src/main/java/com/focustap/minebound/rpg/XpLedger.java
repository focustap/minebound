package com.focustap.minebound.rpg;

/** Persistent per-player category budgets and event cooldowns. Does not grant XP itself. */
public final class XpLedger {
    private XpLedger() {}
    public static int accept(CharacterProfile p,String category,String event,long tick,int raw,double rate,int dailyCap,long cooldown) {
        if (!p.created || raw <= 0 || rate <= 0 || dailyCap <= 0) return 0;
        String window="window:"+category, seen="event:"+category+":"+event;
        long start=p.xpWindows.getOrDefault(window,tick);
        if (tick < start) return 0;
        if (tick-start >= Recovery.DAY) { start=tick; p.xpWindowAwards.put(category,0); }
        p.xpWindows.put(window,start);
        Long previous=p.xpWindows.get(seen);
        if (previous != null && (tick < previous || tick-previous < cooldown)) return 0;
        int used=p.xpWindowAwards.getOrDefault(category,0);
        int award=Math.min(dailyCap-used,Math.max(1,(int)Math.round(raw*rate)));
        if (award <= 0) return 0;
        p.xpWindows.entrySet().removeIf(e -> e.getKey().startsWith("event:") && tick-e.getValue() > Recovery.DAY*2);
        if (p.xpWindows.size() > 8192 && !p.xpWindows.containsKey(seen)) return 0;
        p.xpWindows.put(seen,tick); p.xpWindowAwards.put(category,used+award);
        return award;
    }
    public static boolean harvested(int beforeAge,int maxAge,int afterAge,boolean sameCrop) {
        return beforeAge == maxAge && maxAge > 0 && (!sameCrop || afterAge < beforeAge);
    }
}
