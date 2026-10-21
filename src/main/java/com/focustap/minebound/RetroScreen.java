package com.focustap.minebound;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Shared crisp retro canvas; widgets are drawn after the opaque background. */
abstract class RetroScreen extends Screen {
    static final int INK = 0xFF171426, PLUM = 0xFF2B2444, CREAM = 0xFFF3E9D2;
    static final int GOLD = 0xFFE8B86C, FERN = 0xFF6EA484, RED = 0xFFE87A87, LAVENDER = 0xFFCDBDE9;
    RetroScreen(String title) { super(Component.literal(title)); }
    int x() { return (width - 310) / 2; }
    int y() { return (height - 230) / 2; }
    Button button(String label, int dx, int dy, int w, Runnable action) {
        return addRenderableWidget(new RpgButton(x()+dx,y()+dy,w,label,action));
    }
    void text(GuiGraphics g, String s, int dx, int dy, int color) {
        label(g,s,dx,dy,298-dx,color);
    }
    void label(GuiGraphics g,String s,int dx,int dy,int maxWidth,int color) {
        String value=font.width(s)<=maxWidth ? s : font.plainSubstrByWidth(s,Math.max(1,maxWidth-font.width("...")))+"...";
        g.drawString(font,value,x()+dx,y()+dy,color,false);
    }
    void wrap(GuiGraphics g, String s, int dx, int dy, int w, int color) {
        wrap(g,s,dx,dy,w,Math.max(1,(196-dy)/10),color);
    }
    void wrap(GuiGraphics g,String s,int dx,int dy,int w,int maxLines,int color) {
        var lines=font.split(Component.literal(s),Math.min(w,298-dx));
        for (int i=0;i<Math.min(lines.size(),maxLines);i++) {
            g.drawString(font,lines.get(i),x()+dx,y()+dy+i*10,color,false);
            if (i==maxLines-1 && lines.size()>maxLines) {
                g.fill(x()+dx+w-12,y()+dy+i*10,x()+dx+w,y()+dy+i*10+9,INK);
                g.drawString(font,"...",x()+dx+w-12,y()+dy+i*10,color,false);
            }
        }
    }
    void panel(GuiGraphics g,int dx,int dy,int w,int h) {
        g.fill(x()+dx,y()+dy,x()+dx+w,y()+dy+h,0xFF393149);
        g.fill(x()+dx+1,y()+dy+1,x()+dx+w-1,y()+dy+h-1,0xFF1E1A2C);
    }
    static String mod(int value) { return value>=0 ? "+"+value : Integer.toString(value); }
    String pageLabel() { return "THE ALTAR"; }
    @Override public boolean mouseClicked(double mx,double my,int button) {
        boolean handled=super.mouseClicked(mx,my,button);
        // An onPress can replace its widgets or open another screen. Do not retain that stale focus.
        if (getFocused()!=null && !children().contains(getFocused())) { setFocused(null); setDragging(false); }
        return handled;
    }
    @Override public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
        g.fillGradient(0,0,width,height,0xFF100E1B,0xFF211A30);
        g.fill(x()+3,y()+4,x()+313,y()+234,0xFF090810);
        g.fill(x(),y(),x()+310,y()+230,0xFF51455F);
        g.fill(x()+1,y()+1,x()+309,y()+229,INK);
        g.fill(x()+6,y()+25,x()+304,y()+26,0xFF51455F);
        for (int cx : new int[]{x(),x()+302}) for (int cy : new int[]{y(),y()+222}) {
            g.fill(cx,cy,cx+8,cy+1,GOLD); g.fill(cx,cy,cx+1,cy+8,GOLD);
        }
        text(g,"MINEBOUND",12,11,GOLD);
        label(g,pageLabel(),167,11,131,LAVENDER);
        // Content owns its own bounded panels; illustrated scenery appears only on the map.
        drawPage(g);
    }
    abstract void drawPage(GuiGraphics g);
    @Override public boolean isPauseScreen() { return false; }
}
