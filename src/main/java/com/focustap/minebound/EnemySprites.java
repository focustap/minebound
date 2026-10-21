package com.focustap.minebound;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Stage identity and art identity stay separate; swap art without changing combat. */
final class EnemySprites {
    private record Sprite(String sheet, int frames, int width, int height) {}
    private EnemySprites() {}
    static void draw(GuiGraphics g, String enemy, int centerX, int centerY, int ticks) {
        Sprite s = switch (enemy) {
            case "moss_sprite" -> new Sprite("wobbly_thing", 4, 39, 33);
            case "briarling" -> new Sprite("rabbick", 3, 37, 38);
            case "thorn_knight" -> new Sprite("rudinn", 4, 35, 40);
            case "mire_oracle" -> new Sprite("ponman", 4, 29, 43);
            case "crownroot" -> new Sprite("jevil", 3, 42, 41);
            default -> throw new IllegalArgumentException("Unknown enemy art: " + enemy);
        };
        s = new Sprite(s.sheet,s.frames,s.width+4,s.height+4);
        int frame = (ticks / 8) % s.frames;
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath("minebound", "textures/enemy/world1/" + s.sheet + "_" + frame + ".png");
        g.blit(texture, centerX - s.width / 2, centerY - s.height / 2, 0, 0, s.width, s.height, s.width, s.height);
    }
}
