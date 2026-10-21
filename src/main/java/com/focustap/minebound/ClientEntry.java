package com.focustap.minebound;

import com.focustap.minebound.net.Protocol;
import com.focustap.minebound.rpg.*;
import net.minecraft.client.Minecraft;

/** Only this client entry point and dist-filtered wiring reference Minecraft rendering. */
public final class ClientEntry {
    private ClientEntry() {}
    public static void receive(Protocol.Snapshot snapshot) {
        Minecraft mc = Minecraft.getInstance();
        if (snapshot.mode().equals("RESET_CONFIRM")) { mc.setScreen(new ResetConfirmationScreen()); return; }
        if (snapshot.mode().equals("OPEN") || mc.screen instanceof CharacterCreationScreen || mc.screen instanceof StoryScreen) {
            if (!snapshot.profile().introSeen) {
                if (!(mc.screen instanceof StoryScreen)) mc.setScreen(new StoryScreen(snapshot.profile()));
            } else if (!snapshot.profile().created) {
                if (mc.screen instanceof CharacterCreationScreen creation) creation.serverNotice(snapshot.notice());
                else mc.setScreen(new CharacterCreationScreen(snapshot.profile()));
            } else {
                MineboundScreen screen = new MineboundScreen(snapshot.profile(),snapshot.battle());
                mc.setScreen(screen); screen.accept(snapshot);
            }
        } else if (mc.screen instanceof MineboundScreen screen) screen.accept(snapshot);
        else if (mc.screen instanceof SkillWebScreen screen) screen.accept(snapshot);
        else if (!snapshot.notice().isEmpty()) mc.gui.setOverlayMessage(net.minecraft.network.chat.Component.literal(snapshot.notice()),false);
    }
}
