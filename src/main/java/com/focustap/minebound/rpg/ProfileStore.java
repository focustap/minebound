package com.focustap.minebound.rpg;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Local-only persistence, isolated per Minecraft world/server and player UUID. */
public final class ProfileStore {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    public final Path file;
    public ProfileStore(Path file) { this.file = file; }
    public CharacterProfile load() throws IOException {
        if (!Files.exists(file)) return new CharacterProfile();
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            int schema = root.has("schema") ? root.get("schema").getAsInt() : 1;
            if (schema < 1 || schema > CharacterProfile.SCHEMA) throw new IllegalArgumentException("Unsupported profile version " + schema);
            CharacterProfile p = new CharacterProfile();
            if (schema == 1) {
                // The old prototype never saved characters. Accept exported legacy progress,
                // require creation, and derive earned points from completed stages.
                int unlocked = bounded(root, "unlockedStage", 0, 4, 0);
                p.clearedMask = (1 << unlocked) - 1;
                for (int i = 0; i < unlocked; i++) p.xp += EnemyDefinition.WORLD_ONE.get(i).xp();
                p.attributePoints = (p.level() - 1) * 2;
                Files.copy(file, file.resolveSibling(file.getFileName() + ".v1.bak"), StandardCopyOption.REPLACE_EXISTING);
                return p;
            }
            p.created = root.get("created").getAsBoolean();
            p.name = root.get("name").getAsString();
            if (p.name.isBlank() || p.name.length() > 24) throw new IllegalArgumentException("Invalid character name");
            p.race = Race.valueOf(root.get("race").getAsString());
            p.startingClass = CharacterClass.valueOf(root.get("startingClass").getAsString());
            int[] attributes = JSON.fromJson(root.get("attributes"), int[].class);
            int[] paths = JSON.fromJson(root.get("paths"), int[].class);
            if (attributes.length != 6 || (paths.length != 5 && paths.length != 6)) throw new IllegalArgumentException("Invalid stat arrays");
            for (int i = 0; i < 6; i++) {
                if (attributes[i] < 8 || attributes[i] > 18) throw new IllegalArgumentException("Invalid attribute");
                p.attributes[i] = attributes[i];
            }
            for (int i = 0; i < paths.length; i++) {
                if (paths[i] < 0 || paths[i] > 3) throw new IllegalArgumentException("Invalid skill rank");
                p.paths[i] = paths[i];
            }
            p.xp = bounded(root, "xp", 0, 1000000, 0);
            p.skillPoints = bounded(root, "skillPoints", 0, 100, 0);
            p.attributePoints = bounded(root, "attributePoints", 0, 100, 0);
            p.clearedMask = bounded(root, "clearedMask", 0, 31, 0);
            if ((p.clearedMask & (p.clearedMask + 1)) != 0) throw new IllegalArgumentException("Non-sequential stage progress");
            if (schema >= 3) {
                p.currentHp = bounded(root,"currentHp",p.created ? 0 : -1,10000,p.created ? p.maxHp() : -1);
                p.currentPp = bounded(root,"currentPp",p.created ? 0 : -1,10000,p.created ? p.maxPp() : -1);
                p.lastRecoveryTick = root.has("lastRecoveryTick") ? root.get("lastRecoveryTick").getAsLong() : -1;
                p.meditationRecoveryTick = root.has("meditationRecoveryTick") ? root.get("meditationRecoveryTick").getAsLong() : -2;
                p.pactPpThisDay = bounded(root,"pactPpThisDay",0,12,0);
                p.introSeen = root.has("introSeen") && root.get("introSeen").getAsBoolean();
                if (root.has("nodes")) for (var node : root.getAsJsonArray("nodes")) {
                    if (!SkillGraph.NODES.containsKey(node.getAsString())) throw new IllegalArgumentException("Unknown skill node");
                    p.nodes.add(node.getAsString());
                }
                if (root.has("lore")) for (var entry : root.getAsJsonArray("lore")) p.lore.add(entry.getAsString());
                if (root.has("goods")) for (var entry : root.getAsJsonObject("goods").entrySet()) {
                    int amount = entry.getValue().getAsInt();
                    if (amount < 0 || amount > 9999) throw new IllegalArgumentException("Invalid goods count");
                    p.goods.put(entry.getKey(),amount);
                }
                if (root.has("xpWindows")) for (var e : root.getAsJsonObject("xpWindows").entrySet()) p.xpWindows.put(e.getKey(),e.getValue().getAsLong());
                if (root.has("xpWindowAwards")) for (var e : root.getAsJsonObject("xpWindowAwards").entrySet()) p.xpWindowAwards.put(e.getKey(),e.getValue().getAsInt());
                // Node bonuses affect capacity, so clamp only after graph state is loaded.
                p.currentHp = Math.min(p.currentHp,p.maxHp());
                p.currentPp = Math.min(p.currentPp,p.maxPp());
            } else if (p.created) {
                p.currentHp = p.maxHp(); p.currentPp = p.maxPp(); p.introSeen = true;
            }
            return p;
        } catch (RuntimeException e) {
            // Never silently reset or overwrite unreadable/future data.
            throw new IOException("Cannot read " + file + ": " + e.getMessage(), e);
        }
    }
    private static int bounded(JsonObject root, String key, int min, int max, int fallback) {
        int value = root.has(key) ? root.get(key).getAsInt() : fallback;
        if (value < min || value > max) throw new IllegalArgumentException("Invalid " + key);
        return value;
    }
    public void save(CharacterProfile p) throws IOException {
        Files.createDirectories(file.getParent());
        JsonObject root = JSON.toJsonTree(p).getAsJsonObject();
        root.addProperty("schema", CharacterProfile.SCHEMA);
        Path pending = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(pending, JSON.toJson(root), StandardCharsets.UTF_8);
        if (Files.exists(file)) Files.copy(file, file.resolveSibling(file.getFileName() + ".bak"), StandardCopyOption.REPLACE_EXISTING);
        try {
            Files.move(pending, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(pending, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
