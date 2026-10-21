package com.focustap.minebound;

import com.focustap.minebound.net.Protocol;
import net.minecraft.client.gui.GuiGraphics;

final class ResetConfirmationScreen extends RetroScreen {
    ResetConfirmationScreen() { super("Reset Minebound character"); }
    @Override protected void init() {
        button("CANCEL",12,199,138,() -> { Protocol.send("RESET_CANCEL",""); minecraft.setScreen(null); });
        button("ERASE MINEBOUND",160,199,138,() -> Protocol.send("RESET_CONFIRM",""));
    }
    @Override void drawPage(GuiGraphics g) {
        text(g,"RESET RELIC",12,32,RED);
        wrap(g,"Permanently erase YOUR Minebound character? Race, class, attributes, XP, skills, goods, lore, recovery and stage progress will all be lost.",12,65,284,CREAM);
        wrap(g,"Your normal Minecraft inventory, equipment, XP, advancements and world are unaffected.",12,126,284,FERN);
        text(g,"Confirmation expires after 20 seconds.",12,172,LAVENDER);
    }
    @Override public void onClose() { Protocol.send("CLOSE",""); super.onClose(); }
}
