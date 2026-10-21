package com.focustap.minebound;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;

/**
 * Client-only class referenced lazily from AltarBlock via DistExecutor.
 * The UI is currently a local, transient interaction prototype.
 */
public final class ClientEntry {
    private ClientEntry() {}

    public static void openAltar() {
        Minecraft.getInstance().setScreen(new MineboundScreen());
    }
}
