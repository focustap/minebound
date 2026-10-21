package com.focustap.minebound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Original pixel-framed control, retaining vanilla narration/focus/keyboard behavior. */
final class RpgButton extends Button {
    RpgButton(int x,int y,int width,String label,Runnable action) {
        super(x,y,width,20,Component.literal(label),b -> action.run(),DEFAULT_NARRATION);
    }
    @Override protected void renderWidget(GuiGraphics g,int mx,int my,float partial) {
        int edge = active ? (isHoveredOrFocused() ? RetroScreen.GOLD : RetroScreen.LAVENDER) : 0xFF51485E;
        g.fill(getX(),getY(),getX()+width,getY()+height,edge);
        g.fill(getX()+1,getY()+1,getX()+width-1,getY()+height-1,RetroScreen.INK);
        g.fill(getX()+3,getY()+3,getX()+width-3,getY()+height-3,active && isHoveredOrFocused() ? RetroScreen.PLUM : 0xFF211C32);
        if (active && isHoveredOrFocused()) g.fill(getX()+4,getY()+8,getX()+6,getY()+12,RetroScreen.GOLD);
        g.drawCenteredString(Minecraft.getInstance().font,getMessage(),getX()+width/2,getY()+6,active ? RetroScreen.CREAM : 0xFF81768C);
    }
}
