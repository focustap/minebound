package com.focustap.minebound;

import com.focustap.minebound.rpg.*;
import java.io.IOException;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/** Menu/controller only. Character rules, persistence and combat live in separate systems. */
public final class MineboundScreen extends RetroScreen {
    private enum View { MAP, SHEET, DETAILS, SKILLS, BATTLE, ABILITIES, DODGE, RESULTS }
    private final ProfileStore store;
    private final CharacterProfile profile;
    private View view = View.MAP;
    private CharacterClass selectedPath = CharacterClass.WARRIOR;
    private Battle battle;
    private boolean up, down, left, right, focus;
    private String status = "Choose an unlocked stage.";
    private int animation;

    public MineboundScreen(ProfileStore store, CharacterProfile profile) {
        super("Minebound"); this.store = store; this.profile = profile;
    }
    @Override protected void init() {
        if (!profile.created) { minecraft.setScreen(new CharacterCreationScreen(store, profile)); return; }
        rebuild();
    }
    private void navigate(View next) {
        view = next; up = down = left = right = focus = false; rebuild();
    }
    private void rebuild() {
        clearWidgets();
        switch (view) {
            case MAP -> {
                for (int i = 0; i < 5; i++) {
                    final int stage = i;
                    button("1-" + (i + 1), 12 + i * 58, 76, 53, () -> {
                        battle = new Battle(profile, stage); navigate(View.BATTLE);
                    }).active = profile.unlocked(stage);
                }
                button("CHARACTER", 12, 152, 138, () -> navigate(View.SHEET));
                button("SKILL WEB", 160, 152, 138, () -> navigate(View.SKILLS));
                button("EXIT", 105, 184, 100, this::onClose);
            }
            case SHEET -> {
                for (Attribute a : Attribute.values()) {
                    int col = a.ordinal() % 3, row = a.ordinal() / 3;
                    var increase = button("+", 78 + col * 96, 112 + row * 25, 22, () -> {
                        if (profile.spendAttribute(a)) save(); rebuild();
                    });
                    increase.active = profile.attributePoints > 0 && profile.attributes[a.ordinal()] < 18;
                    increase.setTooltip(Tooltip.create(Component.literal(a.description)));
                }
                button("BONUSES", 12, 199, 90, () -> navigate(View.DETAILS));
                button("SKILL WEB", 110, 199, 90, () -> navigate(View.SKILLS));
                button("MAP", 208, 199, 90, () -> navigate(View.MAP));
            }
            case DETAILS -> {
                button("ATTRIBUTES", 12, 199, 138, () -> navigate(View.SHEET));
                button("WORLD MAP", 160, 199, 138, () -> navigate(View.MAP));
            }
            case SKILLS -> {
                for (CharacterClass path : CharacterClass.values()) {
                    button(path.label.substring(0, 3).toUpperCase() + " " + profile.rank(path), 12 + path.ordinal() * 58,
                            58, 53, () -> { selectedPath = path; rebuild(); });
                }
                button("INVEST +1", 12, 173, 138, () -> {
                    if (profile.spendSkill(selectedPath)) { save(); status = selectedPath.label + " path advanced."; }
                    rebuild();
                }).active = profile.skillPoints > 0 && profile.rank(selectedPath) < 3
                        && (profile.rank(selectedPath) == 0 || java.util.Arrays.stream(profile.paths).sum() >= 2);
                button("CHARACTER", 160, 173, 138, () -> navigate(View.SHEET));
                button("WORLD MAP", 105, 199, 100, () -> navigate(View.MAP));
            }
            case BATTLE -> {
                button("BASH", 12, 167, 90, () -> { battle.bash(); afterCommand(); });
                button("ABILITY", 110, 167, 90, () -> navigate(View.ABILITIES));
                button("DEFEND", 208, 167, 90, () -> { battle.defend(); afterCommand(); });
                button("RETREAT", 105, 196, 100, () -> { battle.retreat(); status = "Retreated. No rewards."; navigate(View.MAP); });
            }
            case ABILITIES -> {
                var abilities = profile.abilities();
                for (int i = 0; i < abilities.size(); i++) {
                    Ability a = abilities.get(i);
                    var ability = button(a.label() + " " + profile.cost(a) + " PP", 12, 46 + i * 23, 141, () -> {
                        battle.ability(a); afterCommand();
                    });
                    ability.active = battle.pp >= profile.cost(a);
                    ability.setTooltip(Tooltip.create(Component.literal(a.description + " Scales with " + a.scaling + ".")));
                }
                button("BACK", 105, 199, 100, () -> navigate(View.BATTLE));
            }
            case RESULTS -> button("WORLD MAP", 80, 192, 150, () -> navigate(View.MAP));
            case DODGE -> { }
        }
    }
    private boolean save() {
        try { store.save(profile); return true; }
        catch (IOException e) { status = "Save failed. Check folder permissions."; return false; }
    }
    @Override public void removed() {
        // Retry failed writes on exit; the last valid file is retained as .bak.
        if (profile.created) save();
        super.removed();
    }
    private void afterCommand() {
        if (battle.state == Battle.State.WON) {
            int award = profile.victory(battle.stage);
            status = award == 0 ? "Practice clear. Already claimed rewards." : "+" + award + " XP! Level " + profile.level() + ".";
            save(); navigate(View.RESULTS);
        } else if (battle.state == Battle.State.DODGE) navigate(View.DODGE);
        else navigate(View.BATTLE);
    }
    @Override public void tick() {
        super.tick(); animation++;
        if (view != View.DODGE) return;
        battle.update((right ? 1 : 0) - (left ? 1 : 0), (down ? 1 : 0) - (up ? 1 : 0), focus);
        if (battle.state == Battle.State.LOST) { status = "Defeated. Adjust your build or try again."; navigate(View.RESULTS); }
        else if (battle.state == Battle.State.COMMAND) navigate(View.BATTLE);
    }
    @Override void drawPage(GuiGraphics g) {
        switch (view) {
            case MAP -> drawMap(g);
            case SHEET -> drawSheet(g);
            case DETAILS -> drawDetails(g);
            case SKILLS -> drawSkills(g);
            case BATTLE -> drawBattle(g);
            case ABILITIES -> drawAbilities(g);
            case DODGE -> drawDodge(g);
            case RESULTS -> {
                text(g, battle.state == Battle.State.WON ? "VICTORY!" : "DEFEAT", 120, 60, battle.state == Battle.State.WON ? FERN : RED);
                wrap(g, status, 18, 93, 274, CREAM);
                text(g, "Level " + profile.level() + " / " + profile.xp + " XP", 18, 128, GOLD);
                text(g, "Attributes: " + profile.attributePoints + " / Skills: " + profile.skillPoints, 18, 145, LAVENDER);
            }
        }
    }
    private void drawMap(GuiGraphics g) {
        text(g, "WORLD 1: WHISPERING WOODS", 12, 31, CREAM);
        text(g, profile.name + " / Lv " + profile.level() + " " + profile.startingClass.label, 12, 49, LAVENDER);
        for (int i = 0; i < 5; i++) {
            int color = (profile.clearedMask & (1 << i)) != 0 ? FERN : profile.unlocked(i) ? GOLD : PLUM;
            g.fill(x() + 28 + i * 58, y() + 103, x() + 40 + i * 58, y() + 113, color);
        }
        text(g, "1-3 MINIBOSS       1-5 WORLD BOSS", 12, 128, GOLD);
        wrap(g, status, 12, 211, 285, LAVENDER);
    }
    private void drawSheet(GuiGraphics g) {
        text(g, profile.name + " / " + profile.race.label + " " + profile.startingClass.label, 12, 30, CREAM);
        text(g, "LEVEL " + profile.level() + "  XP " + profile.xp + "/" + CharacterProfile.threshold(profile.level() + 1), 12, 45, GOLD);
        int hp = battle == null ? profile.maxHp() : battle.hp;
        int pp = battle == null ? profile.maxPp() : battle.pp;
        text(g, "HP " + hp + "/" + profile.maxHp() + "  PP " + pp + "/" + profile.maxPp(), 12, 60, CREAM);
        text(g, "Points: " + profile.attributePoints + " ATTR / " + profile.skillPoints + " SKILL", 12, 75, GOLD);
        text(g, "BASH " + CombatRules.bash(profile, 1) + "  SPEED " + String.format(java.util.Locale.ROOT, "%.2f", profile.speed())
                + "  CLEARS " + Integer.bitCount(profile.clearedMask) + "/5", 12, 92, FERN);
        for (Attribute a : Attribute.values()) text(g, a + " " + profile.stat(a), 12 + a.ordinal() % 3 * 96, 118 + a.ordinal() / 3 * 25, CREAM);
        wrap(g, profile.race.passive, 12, 164, 284, LAVENDER);
        // Full ability effects and class/path bonuses are inspectable in the skill web.
    }
    private void drawSkills(GuiGraphics g) {
        text(g, "MULTICLASS WEB / POINTS " + profile.skillPoints, 12, 30, GOLD);
        text(g, "COMMON ROOT ---- EVERY PATH IS OPEN", 12, 45, FERN);
        CharacterClass c = selectedPath;
        text(g, c.label + "  Rank " + profile.rank(c) + "/3", 12, 85, CREAM);
        String bonus = switch(c) {
            case WARRIOR -> "+1 BASH damage per rank";
            case ROGUE -> "+0.05 heart speed per rank";
            case WIZARD -> "+1 max PP per rank";
            case DRUID -> "+1 restorative healing per rank";
            case PALADIN -> "+1 max HP per rank";
        };
        text(g, bonus, 12, 100, FERN);
        wrap(g, "Rank 2: learn " + c.ability + (c == CharacterClass.DRUID ? " + MEND" : "")
                + ". Rank 2 needs two points anywhere in the web. Rank 3 improves scaling.", 12, 115, 284, LAVENDER);
        wrap(g, "Starting passive: " + profile.startingClass.passive, 12, 147, 284, GOLD);
    }
    private void drawDetails(GuiGraphics g) {
        text(g, "CHARACTER / ACTIVE & PASSIVE BONUSES", 12, 30, GOLD);
        wrap(g, profile.startingClass.passive, 12, 47, 284, FERN);
        wrap(g, profile.race.passive, 12, 73, 284, LAVENDER);
        text(g, "UNLOCKED ABILITIES: DAMAGE / HEAL / PP", 12, 104, CREAM);
        int row = 120;
        for (Ability a : profile.abilities()) {
            text(g, a.label(), 12, row, GOLD);
            text(g, CombatRules.abilityDamage(profile, a) + " / " + CombatRules.healing(profile, a)
                    + " / " + profile.cost(a), 181, row, FERN);
            row += 12;
        }
        // Aggregate path ranks are applied in the derived values; inspect individual paths in the web.
        text(g, "W " + profile.paths[0] + " / R " + profile.paths[1] + " / M " + profile.paths[2]
                + " / D " + profile.paths[3] + " / P " + profile.paths[4], 12, 189, LAVENDER);
    }
    private void drawBattle(GuiGraphics g) {
        text(g, battle.enemy.name() + " / TURN " + battle.turn, 12, 30, CREAM);
        text(g, "FOE " + battle.enemyHp + "/" + battle.enemy.hp() + "  PHASE " + battle.phase(), 12, 45, RED);
        EnemySprites.draw(g, battle.enemy.id(), x() + 155, y() + 94, animation);
        text(g, "HP " + battle.hp + "/" + profile.maxHp() + "   PP " + battle.pp + "/" + profile.maxPp(), 12, 127, CREAM);
        wrap(g, battle.message, 12, 143, 285, GOLD);
    }
    private void drawAbilities(GuiGraphics g) {
        text(g, "ABILITIES / PP " + battle.pp + "/" + profile.maxPp(), 12, 30, GOLD);
        int i = 0;
        for (Ability a : profile.abilities()) {
            text(g, CombatRules.abilityDamage(profile, a) + " DMG / " + CombatRules.healing(profile, a) + " HP", 164, 51 + i * 23, FERN);
            i++;
        }
        text(g, "Hover an ability to inspect its effect.", 12, 187, LAVENDER);
    }
    private void drawDodge(GuiGraphics g) {
        text(g, battle.enemy.name() + " / PHASE " + battle.phase(), 12, 30, CREAM);
        text(g, "HP " + battle.hp + "   PP " + battle.pp, 203, 30, GOLD);
        int ax = x() + 37, ay = y() + 59;
        g.fill(ax - 3, ay - 3, ax + Patterns.WIDTH + 3, ay + Patterns.HEIGHT + 3, CREAM);
        g.fill(ax, ay, ax + Patterns.WIDTH, ay + Patterns.HEIGHT, INK);
        for (Projectile p : battle.bullets) {
            int bx = ax + Math.round(p.x), by = ay + Math.round(p.y), r = Math.round(p.radius);
            // Clip edge telegraphs to the arena border, so warnings remain visible.
            bx = Math.clamp(bx, ax, ax + Patterns.WIDTH); by = Math.clamp(by, ay, ay + Patterns.HEIGHT);
            int color = p.delay > 0 ? 0xFF706380 : p.color;
            if (p.delay > 0) {
                g.renderOutline(bx - r - 1, by - r - 1, r * 2 + 2, r * 2 + 2, color);
            } else g.fill(bx - r, by - r, bx + r, by + r, color);
        }
        if (battle.iframe % 4 < 2) {
            int hx = ax + Math.round(battle.heartX), hy = ay + Math.round(battle.heartY);
            g.fill(hx - 4, hy - 3, hx - 1, hy + 2, RED);
            g.fill(hx + 1, hy - 3, hx + 4, hy + 2, RED);
            g.fill(hx - 3, hy, hx + 3, hy + 4, RED);
            g.fill(hx - 1, hy + 3, hx + 1, hy + 6, RED);
        }
        text(g, Patterns.name(battle.stage, battle.turn, battle.phase(), battle.tick), 12, 181, GOLD);
        text(g, "WASD / ARROWS   SHIFT: precision", 12, 198, LAVENDER);
        text(g, "Hollow = warning / " + ((Patterns.LENGTH - battle.tick + 19) / 20) + " seconds left", 12, 213, LAVENDER);
    }
    private boolean direction(int key, boolean pressed) {
        switch (key) {
            case GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_UP -> up = pressed;
            case GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_DOWN -> down = pressed;
            case GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_LEFT -> left = pressed;
            case GLFW.GLFW_KEY_D, GLFW.GLFW_KEY_RIGHT -> right = pressed;
            case GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT -> focus = pressed;
            default -> { return false; }
        }
        return true;
    }
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (view == View.DODGE && direction(key, true)) return true;
        return super.keyPressed(key, scan, modifiers);
    }
    @Override public boolean keyReleased(int key, int scan, int modifiers) {
        boolean handled = direction(key, false);
        return handled || super.keyReleased(key, scan, modifiers);
    }
    @Override public void onClose() {
        // Closing during combat is a retreat. Rewards only come from a completed victory.
        if (battle != null && (view == View.DODGE || view == View.BATTLE || view == View.ABILITIES)) battle.retreat();
        super.onClose();
    }
}
