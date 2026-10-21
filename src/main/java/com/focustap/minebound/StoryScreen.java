package com.focustap.minebound;

import com.focustap.minebound.net.Protocol;
import com.focustap.minebound.rpg.*;
import net.minecraft.client.gui.GuiGraphics;

final class StoryScreen extends RetroScreen {
    private int page;
    StoryScreen(CharacterProfile profile) { super("The seal remembers"); }
    @Override protected void init() {
        clearWidgets();
        button("SKIP",12,199,90,() -> Protocol.send("STORY","intro"));
        button(page == Story.OPENING.size()-1 ? "BEGIN" : "CONTINUE",160,199,138,() -> {
            if (page == Story.OPENING.size()-1) Protocol.send("STORY","intro");
            else { page++; init(); }
        });
    }
    @Override void drawPage(GuiGraphics g) {
        text(g,"THE SEAL REMEMBERS",12,32,GOLD);
        wrap(g,Story.OPENING.get(page),18,67,274,CREAM);
        text(g,(page+1) + " / " + Story.OPENING.size(),12,181,LAVENDER);
    }
    @Override public void onClose() { Protocol.send("CLOSE",""); super.onClose(); }
}
