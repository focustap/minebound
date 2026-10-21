package com.focustap.minebound;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.storage.LevelResource;
import com.focustap.minebound.rpg.ProfileStore;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Client-only entry point. Gameplay remains local single-player, not synchronized co-op.
 */
public final class ClientEntry {
    private ClientEntry() {}

    public static void openAltar() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        try {
            String world = mc.getSingleplayerServer() != null
                    ? mc.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().toString()
                    : "server:" + (mc.getCurrentServer() == null ? "unknown" : mc.getCurrentServer().ip);
            String key = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(world.getBytes(StandardCharsets.UTF_8)));
            ProfileStore store = new ProfileStore(mc.gameDirectory.toPath().resolve("config/minebound/profiles")
                    .resolve(key).resolve(mc.player.getUUID() + ".json"));
            var profile = store.load();
            if (!profile.created) {
                profile.name = mc.player.getName().getString();
                mc.setScreen(new CharacterCreationScreen(store, profile));
            } else mc.setScreen(new MineboundScreen(store, profile));
        } catch (Exception e) {
            mc.setScreen(new net.minecraft.client.gui.screens.AlertScreen(() -> mc.setScreen(null),
                    net.minecraft.network.chat.Component.literal("Minebound profile could not be opened"),
                    net.minecraft.network.chat.Component.literal(e.getMessage())));
        }
    }
}
