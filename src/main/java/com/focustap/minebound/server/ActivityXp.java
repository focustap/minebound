package com.focustap.minebound.server;

import com.focustap.minebound.rpg.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Awards only from verified server transitions, not raw clicks. No vanilla XP is read or changed. */
public final class ActivityXp {
    private record Harvest(UUID player,ResourceLocation dimension,BlockPos pos,BlockState before,long tick,boolean crop) {}
    private record Activity(double x,double y,double z,long moved) {}
    private static final Map<String,Harvest> PENDING=new HashMap<>();
    private static final Map<UUID,Activity> ACTIVITY=new HashMap<>();
    private static final Map<UUID,Long> CASTS=new HashMap<>();
    private static final Map<UUID,Integer> NOTICES=new HashMap<>();
    private static final TagKey<Block> ORES=TagKey.create(net.minecraft.core.registries.Registries.BLOCK,ResourceLocation.fromNamespaceAndPath("c","ores"));
    private ActivityXp() {}
    private static boolean real(ServerPlayer p) { return !(p instanceof FakePlayer) && !p.isCreative() && !p.isSpectator(); }
    private static String key(ServerLevel level,BlockPos pos) { return level.dimension().location()+":"+pos.asLong(); }
    private static boolean ore(BlockState state) { return state.is(ORES); }
    private static IntegerProperty age(BlockState state) {
        // Compatible crops exposing a conventional age property are verified by state change.
        String id=BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        if (!(state.getBlock() instanceof CropBlock) && !state.is(net.minecraft.tags.BlockTags.CROPS)
                && !id.contains("crop") && !id.contains("wheat") && !id.contains("carrot") && !id.contains("potato") && !id.contains("beetroot")) return null;
        return state.getProperties().stream().filter(p -> p instanceof IntegerProperty && p.getName().equals("age")).map(p -> (IntegerProperty)p).findFirst().orElse(null);
    }
    private static boolean mature(BlockState state) {
        IntegerProperty a=age(state); return a != null && state.getValue(a).equals(Collections.max(a.getPossibleValues()));
    }
    private static void queue(ServerPlayer player,BlockPos pos,BlockState state,boolean crop) {
        if (!real(player)) return;
        var session=CharacterService.session(player); if (session == null || !session.profile.created) return;
        ServerLevel level=player.serverLevel();
        PENDING.put(key(level,pos),new Harvest(player.getUUID(),level.dimension().location(),pos.immutable(),state,CharacterService.now(player),crop));
    }
    public static void breakBlock(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer p) || event.isCanceled()) return;
        if (mature(event.getState())) queue(p,event.getPos(),event.getState(),true);
        else if (ore(event.getState())) queue(p,event.getPos(),event.getState(),false);
    }
    public static void rightClick(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer p && mature(event.getLevel().getBlockState(event.getPos())))
            queue(p,event.getPos(),event.getLevel().getBlockState(event.getPos()),true);
    }
    public static void placed(BlockEvent.EntityPlaceEvent event) {
        if (event.isCanceled() || !(event.getLevel() instanceof ServerLevel level) || !ore(event.getPlacedBlock())) return;
        ResourceHistory.get(level.getServer()).mark(key(level,event.getPos()));
    }
    public static void spawned(net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent event) {
        if (event.getSpawnType() == MobSpawnType.SPAWNER || event.getSpawnType() == MobSpawnType.DISPENSER)
            event.getEntity().getPersistentData().putBoolean("minebound_no_activity_xp",true);
    }
    public static void killed(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getSource().getEntity() instanceof ServerPlayer p) || !real(p)
                || !(event.getEntity() instanceof Monster mob) || mob.distanceToSqr(p) > 48*48
                || mob.getPersistentData().getBoolean("minebound_no_activity_xp")) return;
        int xp=mob.getMaxHealth() >= 100 ? 12 : mob.getMaxHealth() >= 35 ? 5 : 3;
        award(p,"mob",mob.getUUID().toString(),xp,XpConfig.MOB_CAP.get(),Recovery.DAY);
    }
    public static void useRod(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer p && real(p) && p.getItemInHand(event.getHand()).is(Items.FISHING_ROD) && p.fishing == null)
            CASTS.put(p.getUUID(),CharacterService.now(p));
    }
    public static void fished(ItemFishedEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof ServerPlayer p) || !real(p)) return;
        Long cast=CASTS.remove(p.getUUID()); Activity activity=ACTIVITY.get(p.getUUID()); long now=CharacterService.now(p);
        if (cast == null || now-cast < 80 || now-cast > 2400 || activity == null || now-activity.moved > 1200 || event.getDrops().isEmpty()) return;
        boolean fish=event.getDrops().stream().allMatch(s -> s.is(Items.COD)||s.is(Items.SALMON)||s.is(Items.PUFFERFISH)||s.is(Items.TROPICAL_FISH));
        award(p,"fish","manual_catch",fish ? 3 : 5,XpConfig.FISH_CAP.get(),200);
    }
    private static void award(ServerPlayer p,String category,String event,int raw,int cap,long cooldown) {
        if (!real(p)) return;
        var s=CharacterService.session(p); if (s == null) return;
        double rate=XpConfig.RATE.get()/(1+Math.max(0,s.profile.level()-1)*XpConfig.LEVEL_PENALTY.get());
        int xp=XpLedger.accept(s.profile,category,event,CharacterService.now(p),raw,rate,cap,cooldown);
        if (xp <= 0) return;
        s.profile.addXp(xp); s.dirty=true; NOTICES.merge(p.getUUID(),xp,Integer::sum);
    }
    public static void tick(ServerTickEvent.Post event) {
        var server=event.getServer(); long now=server.overworld().getGameTime();
        Iterator<Map.Entry<String,Harvest>> iterator=PENDING.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry=iterator.next(); Harvest h=entry.getValue(); if (now<=h.tick) continue; iterator.remove();
            ServerPlayer p=server.getPlayerList().getPlayer(h.player); if (p==null || !real(p) || now-h.tick > 3) continue;
            ServerLevel level=server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,h.dimension));
            if (level==null || !level.hasChunkAt(h.pos)) continue;
            BlockState after=level.getBlockState(h.pos);
            if (h.crop) {
                IntegerProperty beforeAge=age(h.before), afterAge=age(after);
                boolean same=after.getBlock()==h.before.getBlock() && afterAge!=null;
                if (beforeAge!=null && XpLedger.harvested(h.before.getValue(beforeAge),Collections.max(beforeAge.getPossibleValues()),same ? after.getValue(afterAge) : 0,same))
                    award(p,"farm",entry.getKey(),2,XpConfig.FARM_CAP.get(),1200);
            } else if (!after.equals(h.before) && !ore(after)) {
                ResourceHistory history=ResourceHistory.get(server);
                if (history.blocked(entry.getKey())) continue;
                history.mark(entry.getKey());
                String path=BuiltInRegistries.BLOCK.getKey(h.before.getBlock()).getPath();
                int xp=path.contains("ancient_debris") ? 20 : path.contains("emerald") ? 15 : path.contains("diamond") ? 12
                        : path.contains("gold") ? 5 : path.contains("iron")||path.contains("lapis")||path.contains("redstone") ? 3 : 2;
                award(p,"ore",entry.getKey(),xp,XpConfig.ORE_CAP.get(),Recovery.DAY);
            }
        }
        if (now%20!=0) return;
        for (ServerPlayer p:server.getPlayerList().getPlayers()) {
            Activity previous=ACTIVITY.get(p.getUUID());
            boolean moved=previous==null || p.distanceToSqr(previous.x,previous.y,previous.z) > .5;
            ACTIVITY.put(p.getUUID(),new Activity(p.getX(),p.getY(),p.getZ(),moved ? now : previous.moved));
            if (now%100==0 && NOTICES.containsKey(p.getUUID())) {
                var s=CharacterService.session(p); if (s==null) continue;
                s.notice="+"+NOTICES.remove(p.getUUID())+" Adventurer XP";
                CharacterService.flush(p,s); CharacterService.sync(p,s,"UPDATE");
            }
        }
    }
    public static void clear() { PENDING.clear(); ACTIVITY.clear(); CASTS.clear(); NOTICES.clear(); }
}
