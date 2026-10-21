package com.focustap.minebound.rpg;

/** Constant velocity after launch. delay is a visible, harmless telegraph. */
public final class Projectile {
    public float x, y;
    public final float vx, vy, radius;
    public final int color;
    public int delay, life;
    private float previousX, previousY;
    public Projectile(float x, float y, float vx, float vy, float radius, int color, int delay, int life) {
        this.x = x; this.y = y; this.vx = vx; this.vy = vy; this.radius = radius;
        this.color = color; this.delay = delay; this.life = life;
        previousX = x; previousY = y;
    }
    public void tick() {
        previousX = x; previousY = y;
        if (delay > 0) { delay--; return; }
        x += vx; y += vy; life--;
    }
    public boolean hits(float hx, float hy) {
        if (delay > 0) return false;
        float dx = x - previousX, dy = y - previousY, length = dx * dx + dy * dy;
        float t = length == 0 ? 0 : Math.clamp(((hx - previousX) * dx + (hy - previousY) * dy) / length, 0f, 1f);
        float ex = previousX + t * dx - hx, ey = previousY + t * dy - hy;
        return ex * ex + ey * ey < (radius + 3) * (radius + 3);
    }
    public boolean expired() { return life <= 0 || x < -24 || y < -24 || x > Patterns.WIDTH + 24 || y > Patterns.HEIGHT + 24; }
}
