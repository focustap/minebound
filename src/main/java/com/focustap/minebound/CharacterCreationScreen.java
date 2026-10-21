package com.focustap.minebound;

import com.focustap.minebound.rpg.*;
import java.io.IOException;
import com.focustap.minebound.net.Protocol;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/** Reversible creation draft. Only confirmation grants the first skill point. */
final class CharacterCreationScreen extends RetroScreen {
    private final CharacterProfile profile;
    private int step;
    private String error = "";
    private boolean pending;
    CharacterCreationScreen(CharacterProfile profile) {
        super("Create your Minebound character"); this.profile = profile;
    }
    void serverNotice(String value) { error = value; pending = false; rebuild(); }
    @Override protected void init() { rebuild(); }
    private void rebuild() {
        clearWidgets();
        if (step == 0) {
            for (Race race : Race.values()) {
                button((profile.race == race ? "> " : "") + race.label, 12, 49 + race.ordinal() * 26, 112, () -> {
                    profile.race = race;
                    // Switching away from Human refunds the extra point from the last allocation.
                    while (profile.creationRemaining() < 0) {
                        for (int i = 5; i >= 0; i--) if (profile.attributes[i] > 8) { profile.attributes[i]--; break; }
                    }
                    rebuild();
                });
            }
        } else if (step == 1) {
            for (CharacterClass path : CharacterClass.values()) {
                button((profile.startingClass == path ? "> " : "") + path.label, 12, 45 + path.ordinal() * 25, 112,
                        () -> { profile.startingClass = path; rebuild(); });
            }
        } else if (step == 2) {
            for (Attribute a : Attribute.values()) {
                int row = 48 + a.ordinal() * 22;
                button("-", 103, row, 24, () -> { profile.allocateCreation(a, -1); rebuild(); }).active = profile.attributes[a.ordinal()] > 8;
                var increase = button("+", 132, row, 24, () -> { profile.allocateCreation(a, 1); rebuild(); });
                increase.active = profile.creationRemaining() > 0 && profile.attributes[a.ordinal()] < 14;
                increase.setTooltip(Tooltip.create(Component.literal(a.description)));
            }
        } else {
            EditBox name = new EditBox(font, x() + 12, y() + 45, 280, 20, Component.literal("Character name"));
            name.setMaxLength(24); name.setValue(profile.name); name.setResponder(value -> profile.name = value);
            addRenderableWidget(name);
        }
        button(step == 0 ? "EXIT" : "BACK", 12, 200, 85, () -> {
            if (step == 0) onClose(); else { step--; rebuild(); }
        });
        button(step == 3 ? "CONFIRM" : "NEXT", 207, 200, 91, () -> {
            if (step < 3) { step++; rebuild(); return; }
            if (pending) return;
            if (profile.creationRemaining() != 0 || profile.name.isBlank()) { error = "Spend all points and enter a name."; return; }
            pending = true;
            Protocol.send("CREATE",Protocol.JSON.toJson(new Protocol.Create(profile.name,profile.race.name(),profile.startingClass.name(),profile.attributes)));
            rebuild();
        }).active = !pending && (step != 2 || profile.creationRemaining() == 0);
    }
    @Override void drawPage(GuiGraphics g) {
        text(g, (step + 1) + "/4  " + new String[]{"CHOOSE RACE", "CHOOSE STARTING CLASS", "POINT BUY", "REVIEW CHARACTER"}[step], 12, 30, CREAM);
        if (step == 0) {
            text(g, profile.race.label.toUpperCase(), 137, 50, GOLD);
            wrap(g, profile.race.passive, 137, 69, 156, LAVENDER);
            wrap(g, "Every race can learn every class path.", 137, 122, 156, CREAM);
        } else if (step == 1) {
            CharacterClass c = profile.startingClass;
            text(g, "Primary: " + c.primary + " +2", 137, 48, GOLD);
            wrap(g, c.passive, 137, 66, 156, LAVENDER);
            wrap(g, "Starts with " + c.ability + (c == CharacterClass.DRUID ? " and MEND" : "") + ".", 137, 104, 156, FERN);
            text(g, "HP " + profile.maxHp() + " / PP " + profile.maxPp(), 137, 145, CREAM);
            wrap(g, "Your starting class never locks other paths.", 137, 162, 156, LAVENDER);
        } else if (step == 2) {
            for (Attribute a : Attribute.values()) {
                int row = 54 + a.ordinal() * 22;
                text(g, a + " " + profile.stat(a), 14, row, CREAM);
            }
            text(g, "LEFT: " + profile.creationRemaining(), 171, 52, GOLD);
            text(g, "HP: " + profile.maxHp(), 171, 73, FERN);
            text(g, "PP: " + profile.maxPp(), 171, 90, LAVENDER);
            text(g, "BASH: " + CombatRules.bash(profile, 1), 171, 107, CREAM);
            text(g, "SPEED: " + String.format(java.util.Locale.ROOT, "%.2f", profile.speed()), 171, 124, CREAM);
            wrap(g, "1 point per increase. Base 8-14; race and class bonuses included at left.", 171, 146, 122, LAVENDER);
            text(g, "STR damage / DEX mobility / CON HP", 12, 183, LAVENDER);
        } else {
            text(g, profile.race.label + " / " + profile.startingClass.label + " / Level 1", 12, 75, GOLD);
            text(g, "HP " + profile.maxHp() + "    PP " + profile.maxPp() + "    Skills: " + profile.level(), 12, 92, CREAM);
            StringBuilder stats = new StringBuilder();
            for (Attribute a : Attribute.values()) stats.append(a).append(' ').append(profile.stat(a)).append("  ");
            wrap(g, stats.toString(), 12, 109, 285, CREAM);
            wrap(g, profile.race.passive, 12, 137, 285, LAVENDER);
            wrap(g, profile.startingClass.passive, 12, 164, 285, FERN);
        }
        if (!error.isEmpty()) text(g, error, 12, 188, RED);
    }
    @Override public void onClose() { Protocol.send("CLOSE",""); super.onClose(); }
}
