package com.focustap.minebound;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Original retro-styled, menu-only local gameplay prototype.
 * Battles and skill allocations are NOT yet server-side/synchronized.
 */
public final class MineboundScreen extends Screen {
    private static final int INK = 0xFF171426;
    private static final int PLUM = 0xFF2B2444;
    private static final int LAVENDER = 0xFFCDBDE9;
    private static final int CREAM = 0xFFF3E9D2;
    private static final int GOLD = 0xFFE8B86C;
    private static final int FERN = 0xFF6EA484;
    private static final int RED = 0xFFE87A87;

    private static final String[] ENEMIES = {
        "MOSS SPRITE", "BRIARLING", "THORN KNIGHT", "MIRE ORACLE", "CROWNROOT"
    };
    private static final int[] ENEMY_HEALTH = {28, 38, 54, 78, 102};
    private static final int ARENA_WIDTH = 236;
    private static final int ARENA_HEIGHT = 90;
    private static final int PHASE_LENGTH = 160;
    private static final int HIT_INVULNERABILITY_TICKS = 12;

    private enum View { MAP, SKILLS, BATTLE, DODGE, RESULTS }
    private View view = View.MAP;
    private final ClientProfile profile = ClientProfile.DEMO;
    private final List<Bullet> bullets = new ArrayList<>();

    private int stage = 0;
    private int enemyHp;
    private int hp = 134;
    private int pp = 36;
    private int turn = 1;
    private int phaseTicks = 0;
    private int iframe = 0;
    private boolean defending;
    private boolean won;
    private boolean up, down, left, right;
    private float heartX = ARENA_WIDTH / 2f;
    private float heartY = ARENA_HEIGHT * 0.75f;
    private String status = "Choose an unlocked stage.";

    private record Bullet(float x, float y, float vx, float vy, int radius, int color) {
        Bullet advance() { return new Bullet(x + vx, y + vy, vx, vy, radius, color); }
    }

    public MineboundScreen() {
        super(Component.literal("Minebound"));
    }

    private int leftEdge() { return (width - 300) / 2; }
    private int topEdge() { return (height - 204) / 2; }

    @Override
    protected void init() {
        rebuildButtons();
    }

    private void add(String label, int x, int y, int w, int h, Runnable action) {
        addRenderableWidget(Button.builder(Component.literal(label), button -> action.run())
                .bounds(x, y, w, h).build());
    }

    private void rebuildButtons() {
        clearWidgets();
        int x = leftEdge(), y = topEdge();
        switch (view) {
            case MAP -> {
                for (int i = 0; i < 5; i++) {
                    final int target = i;
                    add("1-" + (i + 1), x + 8 + i * 57, y + 71, 52, 24,
                            () -> startStage(target));
                }
                add("SKILL TREE", x + 9, y + 154, 135, 22, () -> navigate(View.SKILLS));
                add("EXIT", x + 156, y + 154, 135, 22, this::onClose);
            }
            case SKILLS -> {
                add("+ ROGUE", x + 10, y + 115, 135, 22, () -> spendSkill(true));
                add("+ MAGE", x + 155, y + 115, 135, 22, () -> spendSkill(false));
                add("BACK TO MAP", x + 68, y + 160, 164, 20, () -> navigate(View.MAP));
            }
            case BATTLE -> {
                add("BASH", x + 14, y + 140, 86, 20, () -> attack(false));
                add("ABILITY", x + 107, y + 140, 86, 20, () -> attack(true));
                add("GOODS", x + 200, y + 140, 86, 20, () -> status = "No Minebound goods yet.");
                add("DEFEND", x + 40, y + 166, 102, 20, this::defend);
                add("RETREAT", x + 158, y + 166, 102, 20, this::retreat);
            }
            case RESULTS -> {
                add("WORLD MAP", x + 75, y + 157, 150, 22, () -> navigate(View.MAP));
            }
            case DODGE -> { /* No buttons. Movement by arrow keys or WASD. */ }
        }
    }

    private void navigate(View next) {
        view = next;
        rebuildButtons();
    }

    private void spendSkill(boolean rogue) {
        boolean ok = rogue ? profile.spendRogue() : profile.spendMage();
        status = ok ? (rogue ? "DEX increased. Rogue path advanced." :
                "INT increased. Mage path advanced.") : "No skill points left.";
    }

    private void startStage(int target) {
        if (target > profile.unlockedStage) {
            status = "That stage is still locked.";
            return;
        }
        stage = target;
        hp = 134 + profile.con() * 6;
        pp = 36;
        enemyHp = ENEMY_HEALTH[stage];
        defending = false;
        turn = 1;
        status = ENEMIES[stage] + " blocks your path!";
        navigate(View.BATTLE);
    }

    private void attack(boolean ability) {
        int damage;
        if (ability) {
            if (profile.mageAbility() && pp >= 7) {
                damage = 16 + profile.intelligence() * 2;
                pp -= 7;
                status = "You cast SPARK for " + damage + " damage!";
            } else if (profile.rogueAbility() && pp >= 5) {
                damage = 14 + profile.dex() * 2;
                pp -= 5;
                status = "You use RIPOSTE for " + damage + " damage!";
            } else {
                status = "Unlock an active skill with 5 points in a path.";
                return;
            }
        } else {
            damage = 9 + profile.str() * 2;
            status = "BASH hits for " + damage + " damage!";
        }
        enemyHp = Math.max(0, enemyHp - damage);
        if (enemyHp == 0) {
            finish(true);
        } else {
            beginDodge();
        }
    }

    private void defend() {
        defending = true;
        status = "You brace for the next attack!";
        beginDodge();
    }

    private void retreat() {
        status = "You retreated from stage 1-" + (stage + 1) + ".";
        navigate(View.MAP);
    }

    private void beginDodge() {
        bullets.clear();
        phaseTicks = 0;
        iframe = 0;
        heartX = ARENA_WIDTH / 2f;
        heartY = ARENA_HEIGHT * 0.75f;
        navigate(View.DODGE);
    }

    private void finish(boolean victory) {
        won = victory;
        if (victory) {
            profile.unlockedStage = Math.max(profile.unlockedStage, Math.min(stage + 1, 4));
            status = "Stage 1-" + (stage + 1) + " cleared!";
        } else {
            status = "Your party was defeated. Try again.";
        }
        navigate(View.RESULTS);
    }

    private void launch(float x, float y, float vx, float vy, int radius, int color) {
        bullets.add(new Bullet(x, y, vx, vy, radius, color));
    }

    /** Fires toward the heart's position at launch, not where it moves afterward. */
    private void launchAimed(float x, float y, float speed, int color) {
        float dx = heartX - x;
        float dy = heartY - y;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length > 0.001f) {
            launch(x, y, dx / length * speed, dy / length * speed, 3, color);
        }
    }

    /** Distinct, learnable bullet patterns spanning the FULL arena, even at stage 1. */
    private void spawnDodgePattern() {
        // Staggered falling pairs: use a volley counter and a coprime stride.
        // The old phaseTicks multiplier made Briarling advance only 2px per
        // volley, accidentally trapping all of its projectiles on the left.
        int rainGap = Math.max(7, 12 - stage);
        if (phaseTicks % rainGap == 0) {
            int volley = phaseTicks / rainGap;
            float x = 8 + (volley * 67 + stage * 29) % (ARENA_WIDTH - 16);
            float speed = 2.9f + stage * 0.23f;
            launch(x, -6, 0, speed, 3, RED);
            if (stage > 0 || volley % 2 == 0) {
                launch(ARENA_WIDTH - x, -12, 0, speed + 0.2f, 3, RED);
            }
        }

        // Gold projectiles home in on the heart's location when fired.
        // Moving after they launch is always a valid way to dodge them.
        int aimedGap = Math.max(15, 25 - stage * 2);
        if (phaseTicks % aimedGap == 0) {
            int volley = phaseTicks / aimedGap;
            float x = 12 + (volley * 89 + stage * 43) % (ARENA_WIDTH - 24);
            launchAimed(x, -8, 3.25f + stage * 0.18f, GOLD);
        }

        // Crossfire forces movement vertically, instead of allowing players
        // to camp between the vertical rain lanes. Alternate both entrances.
        int sideGap = Math.max(12, 25 - stage * 3);
        if (phaseTicks % sideGap == 0) {
            int volley = phaseTicks / sideGap;
            float lane = 10 + (volley * 29 + stage * 9) % (ARENA_HEIGHT - 20);
            boolean fromLeft = volley % 2 == 0;
            float horizontalSpeed = 3.6f + stage * 0.25f;
            launch(fromLeft ? -7 : ARENA_WIDTH + 7, lane,
                    fromLeft ? horizontalSpeed : -horizontalSpeed, 0, 3, FERN);
            if (stage >= 2) {
                launch(fromLeft ? ARENA_WIDTH + 7 : -7, ARENA_HEIGHT - lane,
                        fromLeft ? -horizontalSpeed : horizontalSpeed, 0, 3, FERN);
            }
        }

        // Briarling onward: occasional thorns aimed from either side.
        if (stage >= 1 && phaseTicks % Math.max(20, 37 - stage * 3) == 0) {
            int volley = phaseTicks / Math.max(20, 37 - stage * 3);
            boolean fromLeft = volley % 2 == 0;
            float y = 9 + (volley * 23 + stage * 13) % (ARENA_HEIGHT - 18);
            launchAimed(fromLeft ? -8 : ARENA_WIDTH + 8, y,
                    3.8f + stage * 0.2f, GOLD);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (view != View.DODGE) return;

        float velocity = 2.2f + profile.roguePassives * 0.05f;
        float moveX = (right ? 1 : 0) - (left ? 1 : 0);
        float moveY = (down ? 1 : 0) - (up ? 1 : 0);
        if (moveX != 0 && moveY != 0) {
            moveX *= 0.70710677f;
            moveY *= 0.70710677f;
        }
        heartX = Mth.clamp(heartX + moveX * velocity, 6, ARENA_WIDTH - 6);
        heartY = Mth.clamp(heartY + moveY * velocity, 6, ARENA_HEIGHT - 6);

        phaseTicks++;
        if (iframe > 0) iframe--;
        spawnDodgePattern();

        List<Bullet> next = new ArrayList<>();
        for (Bullet b : bullets) {
            Bullet moved = b.advance();
            if (moved.x < -12 || moved.x > ARENA_WIDTH + 12
                    || moved.y < -16 || moved.y > ARENA_HEIGHT + 12) continue;
            next.add(moved);
            float collisionRadius = moved.radius + 4f;
            if (iframe == 0 && Math.abs(moved.x - heartX) < collisionRadius
                    && Math.abs(moved.y - heartY) < collisionRadius) {
                hp = Math.max(0, hp - (defending ? 3 : 8));
                iframe = HIT_INVULNERABILITY_TICKS;
                if (hp == 0) {
                    finish(false);
                    return;
                }
            }
        }
        bullets.clear();
        bullets.addAll(next);

        if (phaseTicks >= PHASE_LENGTH) {
            defending = false;
            turn++;
            status = "You survived the attack. Choose a command.";
            navigate(View.BATTLE);
        }
    }

    private static boolean directionKey(int code) {
        return code == GLFW.GLFW_KEY_UP || code == GLFW.GLFW_KEY_DOWN
                || code == GLFW.GLFW_KEY_LEFT || code == GLFW.GLFW_KEY_RIGHT
                || code == GLFW.GLFW_KEY_W || code == GLFW.GLFW_KEY_A
                || code == GLFW.GLFW_KEY_S || code == GLFW.GLFW_KEY_D;
    }

    private void setDirection(int key, boolean pressed) {
        if (key == GLFW.GLFW_KEY_W || key == GLFW.GLFW_KEY_UP) up = pressed;
        if (key == GLFW.GLFW_KEY_S || key == GLFW.GLFW_KEY_DOWN) down = pressed;
        if (key == GLFW.GLFW_KEY_A || key == GLFW.GLFW_KEY_LEFT) left = pressed;
        if (key == GLFW.GLFW_KEY_D || key == GLFW.GLFW_KEY_RIGHT) right = pressed;
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (view == View.DODGE && directionKey(key)) {
            setDirection(key, true);
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    @Override
    public boolean keyReleased(int key, int scan, int modifiers) {
        if (directionKey(key)) {
            setDirection(key, false);
            return view == View.DODGE || super.keyReleased(key, scan, modifiers);
        }
        return super.keyReleased(key, scan, modifiers);
    }

    private void panel(GuiGraphics gfx, int x, int y, int w, int h) {
        gfx.fill(x, y, x + w, y + h, CREAM);
        gfx.fill(x + 3, y + 3, x + w - 3, y + h - 3, PLUM);
        gfx.fill(x + 6, y + 6, x + w - 6, y + h - 6, INK);
    }

    private void label(GuiGraphics gfx, String value, int x, int y, int color) {
        gfx.drawString(font, value, x, y, color, false);
    }

    /**
     * Minecraft 1.21.1 Screen.render() invokes renderBackground() before it draws
     * interactive widgets. Draw the entire opaque Minebound canvas here instead
     * of drawing it before super.render(): the latter triggers Minecraft's
     * background blur AFTER our art/text, smearing every custom element while
     * the vanilla buttons remain sharp.
     *
     * Intentionally do not call super.renderBackground(): our fully opaque
     * background requires no world blur or vanilla menu backdrop.
     */
    @Override
    public void renderBackground(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        gfx.fill(0, 0, width, height, INK);
        int x = leftEdge(), y = topEdge();
        panel(gfx, x, y, 300, 204);
        label(gfx, "MINEBOUND", x + 12, y + 10, GOLD);

        switch (view) {
            case MAP -> renderMap(gfx, x, y);
            case SKILLS -> renderSkills(gfx, x, y);
            case BATTLE -> renderBattle(gfx, x, y);
            case DODGE -> renderDodge(gfx, x, y);
            case RESULTS -> {
                gfx.drawCenteredString(font, won ? "VICTORY!" : "DEFEAT", x + 150, y + 78, won ? FERN : RED);
                gfx.drawCenteredString(font, status, x + 150, y + 106, CREAM);
            }
        }
        if (view == View.MAP || view == View.SKILLS) {
            gfx.drawCenteredString(font, status, x + 150, y + 188, LAVENDER);
        }
        // Inherited Screen.render() draws the buttons AFTER this method.
    }

    private void renderMap(GuiGraphics gfx, int x, int y) {
        label(gfx, "WORLD 1: WHISPERING WOODS", x + 12, y + 29, CREAM);
        label(gfx, "Select a stage to enter combat.", x + 12, y + 43, LAVENDER);
        gfx.fill(x + 26, y + 101, x + 272, y + 104, GOLD);
        for (int i = 0; i < 5; i++) {
            int color = i < profile.unlockedStage ? FERN :
                    i == profile.unlockedStage ? GOLD : 0xFF656074;
            gfx.fill(x + 24 + i * 57, y + 99, x + 35 + i * 57, y + 109, color);
        }
        label(gfx, "1-3 MINI BOSS", x + 21, y + 122, LAVENDER);
        label(gfx, "1-5 CASTLE BOSS", x + 166, y + 122, LAVENDER);
    }

    private void renderSkills(GuiGraphics gfx, int x, int y) {
        label(gfx, "FREE MULTICLASS SKILL WEB", x + 12, y + 30, CREAM);
        label(gfx, "POINTS: " + profile.skillPoints, x + 12, y + 45, GOLD);
        label(gfx, "STR " + profile.str() + "  DEX " + profile.dex() +
                "  CON " + profile.con(), x + 12, y + 62, CREAM);
        label(gfx, "INT " + profile.intelligence() + "  WIS " + profile.wis() +
                "  CHA " + profile.cha(), x + 12, y + 78, CREAM);
        label(gfx, "ROGUE " + profile.roguePassives + "/5", x + 14, y + 103, FERN);
        label(gfx, "MAGE " + profile.magePassives + "/5", x + 161, y + 103, LAVENDER);
        label(gfx, "5 passives unlock RIPOSTE / SPARK.", x + 12, y + 149, GOLD);
    }

    private void renderBattle(GuiGraphics gfx, int x, int y) {
        label(gfx, "TURN " + turn + "   " + ENEMIES[stage], x + 12, y + 31, CREAM);
        label(gfx, "Enemy HP: " + enemyHp, x + 12, y + 45, RED);
        gfx.fill(x + 10, y + 59, x + 290, y + 107, PLUM);
        drawEnemyPortrait(gfx, x + 150, y + 83);
        label(gfx, "YOU", x + 13, y + 112, LAVENDER);
        label(gfx, "HP " + hp + "    PP " + pp, x + 58, y + 112, CREAM);
        label(gfx, status, x + 12, y + 126, GOLD);
    }

    /**
     * Tiny original pixel portraits built with crisp rectangles, avoiding external
     * IP assets. These are temporary designs until full PNG sprites are produced.
     */
    private void drawEnemyPortrait(GuiGraphics gfx, int centerX, int centerY) {
        int main = switch (stage) {
            case 0 -> FERN;
            case 1 -> 0xFF668B59;
            case 2 -> 0xFF6EA484;
            case 3 -> 0xFF8E77B6;
            default -> 0xFF927455;
        };
        int eye = switch (stage) {
            case 0 -> GOLD;
            case 1 -> RED;
            case 2 -> RED;
            case 3 -> GOLD;
            default -> 0xFFE9CC82;
        };
        // Shadow, body, crown/hood, and eyes.
        gfx.fill(centerX - 21, centerY + 14, centerX + 21, centerY + 17, INK);
        gfx.fill(centerX - 16, centerY - 10, centerX + 16, centerY + 11, main);
        gfx.fill(centerX - 13, centerY - 13, centerX + 13, centerY - 8, main);
        gfx.fill(centerX - 10, centerY + 10, centerX + 10, centerY + 15, main);
        gfx.fill(centerX - 11, centerY - 4, centerX - 4, centerY + 1, INK);
        gfx.fill(centerX + 4, centerY - 4, centerX + 11, centerY + 1, INK);
        gfx.fill(centerX - 9, centerY - 3, centerX - 5, centerY, eye);
        gfx.fill(centerX + 5, centerY - 3, centerX + 9, centerY, eye);
        gfx.fill(centerX - 4, centerY + 5, centerX + 4, centerY + 7, INK);
        if (stage == 2) { // The Thorn Knight's distinct gold antlers.
            gfx.fill(centerX - 19, centerY - 17, centerX - 13, centerY - 9, GOLD);
            gfx.fill(centerX + 13, centerY - 17, centerX + 19, centerY - 9, GOLD);
            gfx.fill(centerX - 8, centerY - 20, centerX + 8, centerY - 15, GOLD);
        } else if (stage == 4) { // Crownroot's sprawling branch crown.
            gfx.fill(centerX - 24, centerY - 21, centerX + 24, centerY - 15, FERN);
            gfx.fill(centerX - 19, centerY - 26, centerX - 9, centerY - 12, FERN);
            gfx.fill(centerX + 9, centerY - 26, centerX + 19, centerY - 12, FERN);
        } else if (stage == 3) {
            gfx.fill(centerX - 8, centerY - 18, centerX + 8, centerY - 11, LAVENDER);
            gfx.fill(centerX - 3, centerY - 22, centerX + 3, centerY - 18, GOLD);
        } else if (stage == 1) {
            gfx.fill(centerX - 19, centerY + 4, centerX - 14, centerY + 16, RED);
            gfx.fill(centerX + 14, centerY + 4, centerX + 19, centerY + 16, RED);
        } else {
            gfx.fill(centerX - 8, centerY - 17, centerX - 2, centerY - 11, GOLD);
            gfx.fill(centerX + 2, centerY - 17, centerX + 8, centerY - 11, GOLD);
        }
    }

    private void renderDodge(GuiGraphics gfx, int x, int y) {
        label(gfx, ENEMIES[stage] + " ATTACKS!", x + 12, y + 29, CREAM);
        label(gfx, "HP " + hp + " / PP " + pp, x + 190, y + 29, GOLD);
        int arenaX = x + 32, arenaY = y + 52;
        gfx.fill(arenaX - 3, arenaY - 3, arenaX + ARENA_WIDTH + 3,
                arenaY + ARENA_HEIGHT + 3, CREAM);
        gfx.fill(arenaX, arenaY, arenaX + ARENA_WIDTH,
                arenaY + ARENA_HEIGHT, INK);
        for (Bullet bullet : bullets) {
            int bx = arenaX + Math.round(bullet.x), by = arenaY + Math.round(bullet.y);
            gfx.fill(bx - bullet.radius, by - bullet.radius,
                    bx + bullet.radius, by + bullet.radius, bullet.color);
        }
        if (iframe % 4 < 2) {
            int hx = arenaX + Math.round(heartX), hy = arenaY + Math.round(heartY);
            // Hand-drawn, original tiny pixel heart.
            gfx.fill(hx - 5, hy - 3, hx - 1, hy + 1, RED);
            gfx.fill(hx + 1, hy - 3, hx + 5, hy + 1, RED);
            gfx.fill(hx - 5, hy, hx + 5, hy + 3, RED);
            gfx.fill(hx - 3, hy + 3, hx + 3, hy + 5, RED);
            gfx.fill(hx - 1, hy + 5, hx + 1, hy + 7, RED);
        }
        gfx.drawCenteredString(font, "DODGE WITH WASD / ARROWS", x + 150, y + 164, GOLD);
        gfx.drawCenteredString(font, "Time remaining: " +
                ((PHASE_LENGTH - phaseTicks + 19) / 20) + "s", x + 150, y + 179, LAVENDER);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
