package com.focustap.minebound.rpg;

import java.util.List;

/** Deterministic reusable formations in logical arena units, independent of screen size. */
public final class Patterns {
    public static final int WIDTH = 236, HEIGHT = 110, LENGTH = 204;
    public static final int GOLD = 0xFFE8B86C, RED = 0xFFE87A87, GREEN = 0xFF6EA484;
    private Patterns() {}
    public static String name(int stage, int turn, int phase, int tick) {
        return switch (style(stage, turn, phase, tick)) {
            case 0 -> "SEED VOLLEY - move after each flash";
            case 1 -> "CROSSFIRE - watch both edges";
            case 2 -> "THORN GATES - find the open lane";
            case 3 -> "FORESIGHT - change direction";
            default -> "ROOT BLOOM - leave the marked ground";
        };
    }
    private static int style(int stage, int turn, int phase, int tick) {
        int segment = Math.max(0, tick - 24) / 60;
        return switch (stage) {
            case 0 -> 0;
            case 1 -> segment % 2 == 0 ? 1 : 0;
            case 2 -> (turn + segment + phase - 2) % 3;
            case 3 -> segment % 2 == 0 ? 3 : 4;
            default -> Math.floorMod(turn + segment + phase - 2, 5);
        };
    }
    public static void spawn(List<Projectile> out, int stage, int turn, int phase, int tick,
                             float hx, float hy, float moveX, float moveY) {
        if (tick < 24 || tick > LENGTH - 22) return;
        int mode = style(stage, turn, phase, tick);
        int gap = stage == 0 ? 26 : Math.max(18, 26 - stage - phase);
        if ((tick - 24) % gap == 0) {
            int volley = (tick - 24) / gap + turn;
            float sx = switch (volley % 4) { case 0 -> -8; case 1, 3 -> 12 + volley * 67 % (WIDTH - 24); default -> WIDTH + 8; };
            float sy = switch (volley % 4) { case 0, 2 -> 12 + volley * 29 % (HEIGHT - 24); case 1 -> -8; default -> HEIGHT + 8; };
            float tx = hx, ty = hy;
            if (mode == 3 && volley % 2 == 0) {
                tx = Math.clamp(hx + moveX * 12, 6, WIDTH - 6);
                ty = Math.clamp(hy + moveY * 12, 6, HEIGHT - 6);
            }
            aimed(out, sx, sy, tx, ty, 2.15f + stage * .13f + (phase - 1) * .1f,
                    mode == 0 || mode == 1 || mode == 3 ? 3 : 1);
        }
        if (mode == 1 && tick % 28 == 0) crossfire(out, tick / 28, 2.6f + stage * .1f);
        if (mode == 2 && tick % 44 == 0) wall(out, tick / 44 + turn, phase);
        if (mode == 3 && tick % 36 == 0) wave(out, tick / 36);
        if (mode == 4 && tick % 32 == 0) denial(out, hx, hy);
        // Boss phases layer earlier lessons, with sparse cadence and wide wall gaps.
        if (stage == 4 && phase >= 2 && tick % 56 == 0 && mode != 2) wall(out, tick / 56 + turn, phase);
        if (stage == 4 && phase == 3 && tick % 50 == 0 && mode != 4) denial(out, hx, hy);
    }
    public static void aimed(List<Projectile> out, float sx, float sy, float tx, float ty, float speed, int count) {
        double direction = Math.atan2(ty - sy, tx - sx);
        for (int i = 0; i < count; i++) {
            double angle = direction + (i - (count - 1) / 2.0) * .26;
            out.add(new Projectile(sx, sy, (float)Math.cos(angle) * speed, (float)Math.sin(angle) * speed, 3, GOLD, 10, 180));
        }
    }
    public static void crossfire(List<Projectile> out, int volley, float speed) {
        float lane = 14 + volley * 31 % (HEIGHT - 28);
        out.add(new Projectile(-8, lane, speed, 0, 3, GREEN, 14, 150));
        out.add(new Projectile(WIDTH + 8, HEIGHT - lane, -speed, 0, 3, GREEN, 14, 150));
    }
    public static void wall(List<Projectile> out, int volley, int phase) {
        boolean vertical = volley % 2 == 0;
        int extent = vertical ? WIDTH : HEIGHT;
        float gapCenter = 24 + volley * 47 % (extent - 48);
        for (int lane = 6; lane < extent; lane += 10) {
            if (Math.abs(lane - gapCenter) < 24) continue;
            out.add(new Projectile(vertical ? lane : -8, vertical ? -8 : lane,
                    vertical ? 0 : 1.8f, vertical ? 1.8f : 0, 3, RED, 48, 180));
        }
    }
    public static void denial(List<Projectile> out, float hx, float hy) {
        out.add(new Projectile(hx, hy, 0, 0, 13, RED, 28, 32));
    }
    public static void wave(List<Projectile> out, int volley) {
        for (int lane = 8; lane < WIDTH; lane += 22) {
            if ((lane / 22 + volley) % 3 == 0) continue;
            out.add(new Projectile(lane, -8, (volley % 2 == 0 ? .25f : -.25f), 1.8f, 3, GREEN, 18, 100));
        }
    }
}
