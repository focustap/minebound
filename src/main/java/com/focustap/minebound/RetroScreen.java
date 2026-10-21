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
        return addRenderableWidget(Button.builder(Component.literal(label), b -> action.run())
                .bounds(x() + dx, y() + dy, w, 20).build());
    }
    void text(GuiGraphics g, String s, int dx, int dy, int color) {
        g.drawString(font, s, x() + dx, y() + dy, color, false);
    }
    void wrap(GuiGraphics g, String s, int dx, int dy, int w, int color) {
        g.drawWordWrap(font, Component.literal(s), x() + dx, y() + dy, w, color);
    }
    @Override public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
        g.fill(0, 0, width, height, INK);
        g.fill(x(), y(), x() + 310, y() + 230, CREAM);
        g.fill(x() + 3, y() + 3, x() + 307, y() + 227, PLUM);
        g.fill(x() + 6, y() + 6, x() + 304, y() + 224, INK);
        text(g, "MINEBOUND", 12, 12, GOLD);
        drawPage(g);
    }
    abstract void drawPage(GuiGraphics g);
    @Override public boolean isPauseScreen() { return false; }
}
