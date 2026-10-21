package com.focustap.minebound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

final class StageMarker extends Button {
    private final int stage;
    private final boolean completed;
    StageMarker(int x,int y,int stage,boolean completed,Runnable select) {
        super(x,y,21,21,Component.literal("Stage 1-"+(stage+1)),b -> select.run(),DEFAULT_NARRATION);
        this.stage=stage; this.completed=completed;
        setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("1-"+(stage+1)+" / "+com.focustap.minebound.rpg.Story.stage(stage).name())));
    }
    @Override protected void renderWidget(GuiGraphics g,int mx,int my,float partial) {
        int color = !active ? 0xFF61526B : completed ? RetroScreen.FERN : isHoveredOrFocused() ? RetroScreen.CREAM : RetroScreen.GOLD;
        int cx=getX()+10,cy=getY()+10;
        for (int row=-9;row<=9;row++) { int half=9-Math.abs(row); g.fill(cx-half,cy+row,cx+half+1,cy+row+1,RetroScreen.INK); }
        if (stage==4) {
            g.fill(cx-7,cy-3,cx+8,cy+7,color);
            g.fill(cx-7,cy-7,cx-3,cy+1,color); g.fill(cx+4,cy-7,cx+8,cy+1,color); g.fill(cx-1,cy-8,cx+2,cy+1,color);
            g.fill(cx-1,cy+2,cx+2,cy+7,RetroScreen.INK);
        } else if (stage==2) {
            g.fill(cx-5,cy-6,cx+6,cy+5,color); g.fill(cx-3,cy+5,cx+4,cy+8,color);
            g.fill(cx-3,cy-2,cx+4,cy+1,RetroScreen.INK);
        } else if (stage==3) {
            g.fill(cx-7,cy-3,cx+8,cy+4,color); g.fill(cx-4,cy-6,cx+5,cy+7,color);
            g.fill(cx-2,cy-3,cx+3,cy+4,RetroScreen.INK); g.fill(cx,cy-1,cx+2,cy+2,RetroScreen.LAVENDER);
        } else if (stage==1) {
            for (int row=-7;row<=7;row++) { int half=7-Math.abs(row); g.fill(cx-half,cy+row,cx+half+1,cy+row+1,color); }
            g.fill(cx-1,cy-4,cx+2,cy+5,RetroScreen.INK);
        } else {
            g.fill(cx-6,cy-6,cx+7,cy+7,color);
            g.drawCenteredString(Minecraft.getInstance().font,Integer.toString(stage+1),cx,cy-3,RetroScreen.INK);
        }
    }
}
