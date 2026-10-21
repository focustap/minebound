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
    private static final int PHASE_LENGTH = 135;

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

    private record Bullet(float x, float y, float speed) {
        Bullet advance() { return new Bullet(x, y + speed, speed); }
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

    @Override
    public void tick() {
        super.tick();
        if (view != View.DODGE) return;

        float velocity = 2.2f + profile.roguePassives * 0.05f;
        if (left) heartX -= velocity;
        if (right) heartX += velocity;
        if (up) heartY -= velocity;
        if (down) heartY += velocity;
        heartX = Mth.clamp(heartX, 6, ARENA_WIDTH - 6);
        heartY = Mth.clamp(heartY, 6, ARENA_HEIGHT - 6);

        phaseTicks++;
        if (iframe > 0) iframe--;
        // Repeatable, visible patterns. No hidden RNG hit roll.
        int gap = Math.max(7, 17 - stage * 2);
        if (phaseTicks % gap == 0) {
            float x = 6 + (phaseTicks * (23 + stage * 7) % (ARENA_WIDTH - 12));
            bullets.add(new Bullet(x, 0, 1.7f + stage * 0.35f));
            if (stage >= 2 && phaseTicks % (gap * 2) == 0) {
                bullets.add(new Bullet(ARENA_WIDTH - x, 0, 1.5f + stage * 0.35f));
            }
        }

        List<Bullet> next = new ArrayList<>();
        for (Bullet b : bullets) {
            Bullet moved = b.advance();
            if (moved.y > ARENA_HEIGHT + 4) continue;
            next.add(moved);
            if (iframe == 0 && Math.abs(moved.x - heartX) < 6 && Math.abs(moved.y - heartY) < 6) {
                hp = Math.max(0, hp - (defending ? 3 : 8));
                iframe = 15;
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

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
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
        super.render(gfx, mouseX, mouseY, partialTick);
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
        gfx.drawCenteredString(font, "A strange opponent approaches!", x + 150, y + 74, CREAM);
        label(gfx, "YOU", x + 13, y + 112, LAVENDER);
        label(gfx, "HP " + hp + "    PP " + pp, x + 58, y + 112, CREAM);
        label(gfx, status, x + 12, y + 126, GOLD);
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
            gfx.fill(bx - 3, by - 3, bx + 3, by + 3, RED);
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
