package com.focustap.minebound.server;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** World-owned ore position denylist, survives logout/restart and player resets. */
public final class ResourceHistory extends SavedData {
    private final Set<String> unavailable=new HashSet<>();
    public ResourceHistory() {}
    private static ResourceHistory load(CompoundTag tag,HolderLookup.Provider lookup) {
        ResourceHistory data=new ResourceHistory();
        ListTag entries=tag.getList("positions",Tag.TAG_STRING);
        for (int i=0;i<entries.size();i++) data.unavailable.add(entries.getString(i));
        return data;
    }
    public static ResourceHistory get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(ResourceHistory::new,ResourceHistory::load),"minebound_resource_history");
    }
    public boolean blocked(String key) { return unavailable.contains(key); }
    public void mark(String key) { if (unavailable.add(key)) setDirty(); }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider lookup) {
        ListTag list=new ListTag(); unavailable.stream().sorted().forEach(s -> list.add(StringTag.valueOf(s))); tag.put("positions",list); return tag;
    }
}
