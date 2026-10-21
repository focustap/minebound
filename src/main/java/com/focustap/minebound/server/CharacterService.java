package com.focustap.minebound.server;

import com.focustap.minebound.net.Protocol;
import com.focustap.minebound.rpg.*;
import java.nio.file.Path;
import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Server-only ownership: UUID is always taken from the sender, never from a packet. */
public final class CharacterService {
    private static final Map<UUID,Session> SESSIONS = new HashMap<>();
    public static final class Session {
        public final ProfileStore store;
        public CharacterProfile profile;
        public Battle battle;
        public boolean open, resetArmed, dirty, settled;
        public long resetExpires, inputTick = -1, actionTick = -1;
        public float inputX, inputY;
        public boolean focus;
        public String notice = "";
        Session(ProfileStore store, CharacterProfile profile) { this.store = store; this.profile = profile; }
    }
    private CharacterService() {}
    public static Session session(ServerPlayer player) {
        Session cached = SESSIONS.get(player.getUUID());
        if (cached != null) return cached;
        Path world = player.server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
        ProfileStore store = new ProfileStore(world.resolve("data/minebound/players").resolve(player.getUUID()+".json"));
        try {
            CharacterProfile p;
            if (!java.nio.file.Files.exists(store.file) && player.server.isSingleplayer()
                    && player.server.getSingleplayerProfile() != null && player.server.getSingleplayerProfile().getId().equals(player.getUUID())) {
                // Only the integrated host's previous local save is trusted for migration.
                String key = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                        .digest(world.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                ProfileStore legacy = new ProfileStore(FMLPaths.GAMEDIR.get().resolve("config/minebound/profiles").resolve(key).resolve(player.getUUID()+".json"));
                p = legacy.load();
                if (p.created) store.save(p); // original local files remain intact
            } else p = store.load();
            if (!p.created) p.name = player.getGameProfile().getName();
            if (p.lastRecoveryTick < 0 && p.created) p.lastRecoveryTick = now(player);
            Session result = new Session(store,p); SESSIONS.put(player.getUUID(),result); return result;
        } catch (Exception e) {
            player.displayClientMessage(Component.literal("Minebound could not read your profile. Existing data was kept. " + e.getMessage()),false);
            return null;
        }
    }
    public static long now(ServerPlayer player) { return player.server.overworld().getGameTime(); }
    public static void open(ServerPlayer player) {
        Session s = session(player); if (s == null) return;
        s.open = true; recover(player,s); sync(player,s,"OPEN");
    }
    public static void armReset(ServerPlayer player) {
        Session s = session(player); if (s == null) return;
        if (s.battle != null) { s.battle.retreat(); s.battle = null; }
        s.resetArmed = true; s.resetExpires = now(player) + 400; s.open = true;
        sync(player,s,"RESET_CONFIRM");
    }
    public static void command(ServerPlayer player, Protocol.Command c) {
        Session s = session(player); if (s == null) return;
        long now = now(player);
        if (c.op().equals("INPUT")) {
            if (!s.open || s.battle == null || s.inputTick == now || !Float.isFinite(c.x()) || !Float.isFinite(c.y())) return;
            s.inputX = Math.clamp(c.x(),-1,1); s.inputY = Math.clamp(c.y(),-1,1); s.focus = c.focus(); s.inputTick = now; return;
        }
        if (!s.open || s.actionTick == now) return;
        s.actionTick = now;
        try {
            switch (c.op()) {
                case "CLOSE" -> { if (s.battle != null) s.battle.retreat(); s.battle = null; s.open = false; s.resetArmed = false; }
                case "STORY" -> { s.profile.introSeen = true; s.profile.lore.add("altar"); }
                case "CREATE" -> {
                    if (s.profile.created) break;
                    Protocol.Create draft = Protocol.JSON.fromJson(c.data(),Protocol.Create.class);
                    CharacterProfile p = new CharacterProfile(); p.name = draft.name(); p.race = Race.valueOf(draft.race());
                    p.startingClass = CharacterClass.valueOf(draft.startingClass());
                    if (draft.attributes() == null || draft.attributes().length != 6) throw new IllegalArgumentException("Six attributes are required.");
                    System.arraycopy(draft.attributes(),0,p.attributes,0,6);
                    if (!p.confirm()) throw new IllegalArgumentException("Invalid name or point allocation.");
                    // Preserve imported first-clear progress and earned points while creating its identity.
                    p.clearedMask = s.profile.clearedMask; p.xp = s.profile.xp;
                    p.attributePoints = s.profile.attributePoints; p.skillPoints = p.level(); p.introSeen = true;
                    p.lore.addAll(s.profile.lore); Recovery.initialize(p,now); s.profile = p;
                    s.notice = "The altar remembers you. One starting skill point granted.";
                }
                case "START" -> {
                    if (active(s)) break;
                    recover(player,s); s.battle = new Battle(s.profile,Integer.parseInt(c.data()));
                    s.settled=false;
                    s.notice = Story.stage(s.battle.stage).intro();
                    s.battle.message = s.notice;
                }
                case "BASH" -> { if (active(s)) s.battle.bash(); }
                case "ABILITY" -> { if (active(s)) s.battle.ability(Ability.valueOf(c.data())); }
                case "DEFEND" -> { if (active(s)) s.battle.defend(); }
                case "RETREAT" -> { if (s.battle != null) s.battle.retreat(); s.battle = null; }
                case "GOOD" -> { if (active(s)) s.battle.good(c.data()); else Battle.useGood(s.profile,c.data()); }
                case "SKILL" -> { if (!active(s) && !SkillGraph.purchase(s.profile,c.data())) s.notice = "Node is locked, already known, or too expensive."; }
                case "ATTRIBUTE" -> { if (!active(s)) s.profile.spendAttribute(Attribute.valueOf(c.data())); }
                case "RESET_CANCEL" -> s.resetArmed = false;
                case "RESET_CONFIRM" -> {
                    if (!s.resetArmed || now > s.resetExpires || !player.isCreative()) {
                        s.resetArmed=false; s.notice="Reset confirmation expired or is no longer permitted.";
                        sync(player,s,"OPEN"); return;
                    }
                    // Entire RPG aggregate is replaced; no Minecraft player fields are touched.
                    s.profile = new CharacterProfile(); s.profile.name = player.getGameProfile().getName();
                    s.battle = null; s.resetArmed = false; s.notice = "Minebound character reset.";
                    flush(player,s); sync(player,s,"OPEN"); return;
                }
                default -> { return; }
            }
            resolve(s); s.dirty = true; flush(player,s); sync(player,s,"UPDATE");
        } catch (RuntimeException e) { s.notice = "Minebound: " + e.getMessage(); sync(player,s,"UPDATE"); }
    }
    public static boolean active(Session s) { return s.battle != null && (s.battle.state == Battle.State.COMMAND || s.battle.state == Battle.State.DODGE); }
    private static void resolve(Session s) {
        if (s.battle == null || s.settled) return;
        s.battle.commitResources();
        if (s.battle.state == Battle.State.WON) {
            s.settled=true;
            int xp = s.profile.victory(s.battle.stage);
            if (xp > 0) s.notice = "+" + xp + " Adventurer XP. " + Story.stage(s.battle.stage).defeat();
        } else if (s.battle.state == Battle.State.LOST) {
            s.settled=true;
            s.notice="Your heart falls silent. Use an earned Minebound herb on the map, or return after daily recovery.";
        }
    }
    private static void recover(ServerPlayer player, Session s) {
        if (Recovery.check(s.profile,now(player),active(s))) {
            s.notice = "A full Overworld day has passed. HP and PP restored."; s.dirty = true;
        }
    }
    public static void sync(ServerPlayer player, Session s, String mode) {
        long remaining = s.profile.created ? Math.max(0,Recovery.DAY - Math.max(0,now(player)-s.profile.lastRecoveryTick)) : Recovery.DAY;
        Protocol.sync(player,new Protocol.Snapshot(s.profile,s.battle,mode,s.notice,remaining)); s.notice = "";
    }
    public static void flush(ServerPlayer player, Session s) {
        try { s.store.save(s.profile); s.dirty = false; }
        catch (java.io.IOException e) { s.notice = "Profile save failed; previous file retained."; System.getLogger("minebound").log(System.Logger.Level.ERROR,"Profile save failed",e); }
    }
    public static void tick(ServerTickEvent.Post event) {
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            Session s = SESSIONS.get(player.getUUID()); if (s == null) continue;
            long tick = now(player);
            if (s.battle != null && s.battle.state == Battle.State.DODGE) {
                if (!player.isAlive()) { s.battle.retreat(); s.battle = null; }
                else {
                    boolean stale = tick - s.inputTick > 10;
                    s.battle.update(stale ? 0 : s.inputX,stale ? 0 : s.inputY,s.focus);
                    resolve(s); s.dirty = true;
                    if (s.open) sync(player,s,"UPDATE");
                }
            }
            if (tick % 20 == 0) {
                recover(player,s);
                if (s.dirty) { flush(player,s); if (s.open) sync(player,s,"UPDATE"); }
            }
        }
    }
    public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer player) {
            Session s = SESSIONS.remove(player.getUUID());
            if (s != null) { if (s.battle != null) s.battle.commitResources(); flush(player,s); }
        }
    }
    public static void stopped(ServerStoppedEvent e) { SESSIONS.clear(); ActivityXp.clear(); }
}
