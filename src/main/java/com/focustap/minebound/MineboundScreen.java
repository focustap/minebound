package com.focustap.minebound;

import com.focustap.minebound.rpg.*;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import com.focustap.minebound.net.Protocol;

/** Menu/controller only. Character rules, persistence and combat live in separate systems. */
public final class MineboundScreen extends RetroScreen {
    private enum View { MAP, SHEET, DETAILS, BATTLE, ABILITIES, GOODS, LORE, DODGE, RESULTS }
    private CharacterProfile profile;
    private View view = View.MAP;
    private Battle battle;
    private boolean up, down, left, right, focus;
    private String status = "Choose an unlocked stage.";
    private int animation;

    private long recoveryRemaining;
    private int abilityPage, detailsPage, lorePage;
    private int selectedStage, noticeTicks;
    private static final int[] MAP_X={49,76,151,207,251}, MAP_Y={122,83,96,86,74};
    public MineboundScreen(CharacterProfile profile, Battle battle) {
        super("Minebound"); this.profile = profile; this.battle = battle;
    }
    @Override String pageLabel() { return view == View.MAP ? "WORLD 1 / "+Integer.bitCount(profile.clearedMask)+" OF 5" : view.name(); }
    @Override protected void init() {
        if (!profile.created) { minecraft.setScreen(new CharacterCreationScreen(profile)); return; }
        rebuild();
    }
    private void navigate(View next) {
        view = next; up = down = left = right = focus = false; rebuild();
    }
    private void rebuild() {
        clearWidgets();
        setFocused(null); setDragging(false);
        switch (view) {
            case MAP -> {
                for (int i = 0; i < 5; i++) {
                    final int stage = i;
                    var marker = addRenderableWidget(new StageMarker(x()+MAP_X[i]-10,y()+MAP_Y[i]-10,i,
                            (profile.clearedMask & (1<<i)) != 0,() -> { selectedStage=stage; noticeTicks=0; rebuild(); }));
                    marker.active=profile.unlocked(stage);
                }
                button("ENTER",220,165,78,() -> Protocol.send("START",Integer.toString(selectedStage))).active=profile.unlocked(selectedStage);
                button("SHEET",12,199,67,() -> navigate(View.SHEET));
                button("SKILLS",85,199,67,() -> minecraft.setScreen(new SkillWebScreen(this,profile)));
                button("GOODS",158,199,67,() -> navigate(View.GOODS));
                button("LORE",231,199,67,() -> navigate(View.LORE));
            }
            case SHEET -> {
                for (Attribute a : Attribute.values()) {
                    int col = a.ordinal() % 3, row = a.ordinal() / 3;
                    var increase = button("+", 78 + col * 96, 112 + row * 25, 22, () -> {
                        Protocol.send("ATTRIBUTE",a.name());
                    });
                    increase.active = profile.attributePoints > 0 && profile.attributes[a.ordinal()] < 18;
                    increase.setTooltip(Tooltip.create(Component.literal(a.description)));
                }
                button("BONUSES", 12, 199, 90, () -> navigate(View.DETAILS));
                button("SKILL WEB", 110, 199, 90, () -> minecraft.setScreen(new SkillWebScreen(this,profile)));
                button("MAP", 208, 199, 90, () -> navigate(View.MAP));
            }
            case DETAILS -> {
                button("<",12,199,32,() -> { detailsPage=Math.max(0,detailsPage-1); rebuild(); });
                button("SHEET",50,199,100,() -> navigate(View.SHEET));
                button("MAP",160,199,100,() -> navigate(View.MAP));
                button(">",266,199,32,() -> { detailsPage=Math.min((profile.abilities().size()-1)/6,detailsPage+1); rebuild(); });
            }
            case BATTLE -> {
                var bash=button("BASH", 12, 167, 90, () -> Protocol.send("BASH",""));
                bash.setTooltip(Tooltip.create(Component.literal(CombatRules.bash(profile,1)+" damage / "+CombatRules.bashScaling(profile)+" modifier. Free.")));
                button("ABILITY", 110, 167, 90, () -> navigate(View.ABILITIES));
                button("DEFEND", 208, 167, 90, () -> Protocol.send("DEFEND",""));
                button("GOODS", 12, 196, 138, () -> navigate(View.GOODS));
                button("RETREAT", 160, 196, 138, () -> { Protocol.send("RETREAT",""); navigate(View.MAP); });
            }
            case ABILITIES -> {
                var abilities = profile.abilities();
                for (int i = abilityPage * 6; i < Math.min(abilities.size(),abilityPage * 6 + 6); i++) {
                    Ability a = abilities.get(i);
                    var ability = button(a.label() + " " + profile.cost(a) + " PP", 12, 46 + (i % 6) * 23, 141, () -> {
                        Protocol.send("ABILITY",a.name());
                    });
                    ability.active = battle.pp >= profile.cost(a);
                    ability.setTooltip(Tooltip.create(Component.literal(a.description + " Scales with " + a.scaling + ".")));
                }
                button("<", 12, 199, 50, () -> { abilityPage = Math.max(0,abilityPage - 1); rebuild(); });
                button("BACK", 105, 199, 100, () -> navigate(View.BATTLE));
                button(">", 248, 199, 50, () -> { abilityPage = Math.min((abilities.size()-1)/6,abilityPage + 1); rebuild(); });
            }
            case GOODS -> {
                var herb=button("HERB x" + profile.goods.getOrDefault("herb",0),12,85,138,() -> Protocol.send("GOOD","herb"));
                herb.active=profile.goods.getOrDefault("herb",0)>0 && profile.currentHp<profile.maxHp();
                herb.setTooltip(Tooltip.create(Component.literal(profile.currentHp>=profile.maxHp() ? "HP is already full." : profile.goods.getOrDefault("herb",0)==0 ? "Earn herbs from first clears." : "Restore HP. In combat this uses a turn.")));
                var ether=button("ETHER x" + profile.goods.getOrDefault("ether",0),160,85,138,() -> Protocol.send("GOOD","ether"));
                ether.active=profile.goods.getOrDefault("ether",0)>0 && profile.currentPp<profile.maxPp();
                ether.setTooltip(Tooltip.create(Component.literal(profile.currentPp>=profile.maxPp() ? "PP is already full." : profile.goods.getOrDefault("ether",0)==0 ? "Earn ethers from boss first clears." : "Restore 6 PP. In combat this uses a turn.")));
                button("BACK",105,199,100,() -> navigate(battle != null && battle.state == Battle.State.COMMAND ? View.BATTLE : View.MAP));
            }
            case LORE -> {
                button("<",12,199,50,() -> { lorePage=Math.max(0,lorePage-1); rebuild(); });
                button("MAP",105,199,100,() -> navigate(View.MAP));
                button(">",248,199,50,() -> { lorePage=Math.min(4,lorePage+1); rebuild(); });
            }
            case RESULTS -> button("WORLD MAP", 80, 199, 150, () -> navigate(View.MAP));
            case DODGE -> { }
        }
    }
    void accept(Protocol.Snapshot snapshot) {
        Battle.State before = battle == null ? null : battle.state;
        profile = snapshot.profile(); battle = snapshot.battle(); recoveryRemaining = snapshot.recoveryRemaining();
        if (!snapshot.notice().isEmpty()) { status = snapshot.notice(); noticeTicks=160; }
        if (battle != null) {
            if (battle.state == Battle.State.DODGE && view != View.DODGE) navigate(View.DODGE);
            else if ((battle.state == Battle.State.WON || battle.state == Battle.State.LOST) && view == View.DODGE) navigate(View.RESULTS);
            else if (battle.state == Battle.State.WON && before != Battle.State.WON) navigate(View.RESULTS);
            else if (battle.state == Battle.State.COMMAND && before != Battle.State.COMMAND) navigate(View.BATTLE);
        }
        if (view != View.DODGE) rebuild();
    }
    @Override public void tick() {
        super.tick(); animation++; if (noticeTicks>0) noticeTicks--;
        if (view != View.DODGE) return;
        Protocol.send("INPUT","",(right ? 1 : 0) - (left ? 1 : 0),(down ? 1 : 0) - (up ? 1 : 0),focus);
    }
    @Override void drawPage(GuiGraphics g) {
        switch (view) {
            case MAP -> drawMap(g);
            case SHEET -> drawSheet(g);
            case DETAILS -> drawDetails(g);
            case GOODS -> {
                text(g,"MINEBOUND GOODS",12,30,GOLD);
                wrap(g,"Herb: restore 12 HP (9 for Undead). Ether: restore 6 PP. In combat, using a good consumes your turn.",12,48,284,CREAM);
                panel(g,12,117,286,74);
                wrap(g,status,18,124,274,3,LAVENDER);
                text(g,"Recovery in " + (recoveryRemaining+19)/20 + " Overworld seconds",18,171,FERN);
            }
            case LORE -> {
                text(g,"THE ALTAR'S MEMORY",12,30,GOLD);
                text(g,Story.stage(lorePage).name(),12,50,FERN);
                panel(g,12,69,286,103);
                wrap(g,profile.lore.contains("stage_"+lorePage) ? Story.stage(lorePage).lore()
                        : "This page is still missing. Clear stage 1-"+(lorePage+1)+" to recover its memory.",18,78,274,9,LAVENDER);
                text(g,"MEMORY "+(lorePage+1)+" / 5",12,178,GOLD);
            }
            case BATTLE -> drawBattle(g);
            case ABILITIES -> drawAbilities(g);
            case DODGE -> drawDodge(g);
            case RESULTS -> {
                text(g, battle.state == Battle.State.WON ? "VICTORY!" : "DEFEAT", 120, 32, battle.state == Battle.State.WON ? FERN : RED);
                EnemySprites.draw(g,battle.enemy.id(),x()+155,y()+72,animation);
                wrap(g, status, 18, 104, 274, 5, CREAM);
                text(g, "Level " + profile.level() + " / " + profile.xp + " XP", 18, 165, GOLD);
                text(g, "Attributes: " + profile.attributePoints + " / Skills: " + profile.skillPoints, 18, 182, LAVENDER);
            }
        }
    }
    private void drawMap(GuiGraphics g) {
        Art.draw(g,"woods",x()+7,y()+27,296,119);
        g.fill(x()+9,y()+28,x()+301,y()+43,0xDD171426);
        text(g, "WORLD 1: WHISPERING WOODS", 12, 31, CREAM);
        panel(g,9,148,292,46);
        text(g,"1-"+(selectedStage+1)+" / "+Story.stage(selectedStage).name(),12,152,GOLD);
        wrap(g,noticeTicks>0 ? status : Story.stage(selectedStage).description(),14,167,197,2,LAVENDER);
        // Dotted winding route follows the illustrated clearings, with completed links illuminated.
        for (int i=0;i<4;i++) for (int dot=1;dot<8;dot++) {
            int px=MAP_X[i]+(MAP_X[i+1]-MAP_X[i])*dot/8;
            int py=MAP_Y[i]+(MAP_Y[i+1]-MAP_Y[i])*dot/8 + (int)(Math.sin(dot*Math.PI/8)*5);
            g.fill(x()+px,y()+py,x()+px+2,y()+py+2,profile.unlocked(i+1) ? GOLD : 0xFF6D5E78);
        }
    }
    private void drawSheet(GuiGraphics g) {
        text(g, profile.name + " / " + profile.race.label + " " + profile.startingClass.label, 12, 32, CREAM);
        text(g, "LEVEL " + profile.level() + "  XP " + profile.xp + "/" + CharacterProfile.threshold(profile.level() + 1), 12, 45, GOLD);
        int hp = profile.currentHp;
        int pp = profile.currentPp;
        text(g, "HP " + hp + "/" + profile.maxHp() + "  PP " + pp + "/" + profile.maxPp(), 12, 60, CREAM);
        text(g, "Points: " + profile.attributePoints + " ATTR / " + profile.skillPoints + " SKILL", 12, 75, GOLD);
        int levelStart=CharacterProfile.threshold(profile.level()), levelEnd=CharacterProfile.threshold(profile.level()+1);
        g.fill(x()+12,y()+86,x()+296,y()+90,PLUM);
        g.fill(x()+12,y()+86,x()+12+Math.clamp((profile.xp-levelStart)*284/Math.max(1,levelEnd-levelStart),0,284),y()+90,FERN);
        text(g,"BASH "+CombatRules.bash(profile,1)+" / "+CombatRules.bashScaling(profile)+mod(profile.modifier(CombatRules.bashScaling(profile)))
                +"    PRIMARY: "+profile.startingClass.primary,12,95,FERN);
        for (Attribute a : Attribute.values()) {
            int dx=12+a.ordinal()%3*96,dy=110+a.ordinal()/3*25;
            panel(g,dx,dy,64,23);
            text(g,a+" "+profile.stat(a),dx+4,dy+3,CREAM);
            text(g,"MOD "+mod(profile.modifier(a)),dx+4,dy+13,LAVENDER);
        }
        wrap(g,profile.race.passive,12,164,284,2,LAVENDER);
        text(g,profile.hybridIdentity().isEmpty() ? "Build: "+profile.startingClass.label : "Build: "+profile.hybridIdentity(),12,188,GOLD);
        // Full ability effects and class/path bonuses are inspectable in the skill web.
    }
    private void drawDetails(GuiGraphics g) {
        text(g, "ACTIVE & PASSIVE BONUSES", 12, 30, GOLD);
        wrap(g, profile.startingClass.passive, 12, 47, 284, FERN);
        wrap(g, profile.race.passive, 12, 73, 284, LAVENDER);
        text(g, "ABILITY / STAT / DAMAGE / HEAL / PP", 12, 104, CREAM);
        int row = 120;
        for (Ability a : profile.abilities().stream().skip(detailsPage*6L).limit(6).toList()) {
            label(g,a.label(),12,row,139,GOLD);
            text(g,a.scaling+" / "+CombatRules.abilityDamage(profile,a)+" / "+CombatRules.healing(profile,a)+" / "+profile.cost(a),155,row,FERN);
            row += 12;
        }
        // Aggregate path ranks are applied in the derived values; inspect individual paths in the web.
        String investments=profile.nodes.stream().map(SkillGraph.NODES::get).filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.groupingBy(SkillGraph.Node::branch,java.util.stream.Collectors.counting()))
                .entrySet().stream().sorted(java.util.Map.Entry.<String,Long>comparingByValue().reversed())
                .limit(2).map(e -> e.getKey()+" "+e.getValue()).collect(java.util.stream.Collectors.joining(" / "));
        text(g,investments.isEmpty() ? "No specialization nodes purchased." : investments,12,189,LAVENDER);
    }
    private void drawBattle(GuiGraphics g) {
        // The supplied enemy sprite stands alone on a plain combat stage.
        g.fill(x()+12,y()+58,x()+298,y()+121,0xFF100E1B);
        g.fill(x()+78,y()+119,x()+232,y()+120,0xFF51455F);
        text(g, battle.enemy.name() + " / TURN " + battle.turn, 12, 30, CREAM);
        text(g, "FOE " + battle.enemyHp + "/" + battle.enemy.hp() + "  PHASE " + battle.phase(), 12, 45, RED);
        EnemySprites.draw(g, battle.enemy.id(), x() + 155, y() + 94, animation);
        text(g, "HP " + battle.hp + "/" + profile.maxHp() + "   PP " + battle.pp + "/" + profile.maxPp(), 12, 127, CREAM);
        g.fill(x()+12,y()+137,x()+145,y()+140,PLUM);
        g.fill(x()+12,y()+137,x()+12+battle.hp*133/Math.max(1,profile.maxHp()),y()+140,FERN);
        g.fill(x()+160,y()+137,x()+298,y()+140,PLUM);
        g.fill(x()+160,y()+137,x()+160+battle.pp*138/Math.max(1,profile.maxPp()),y()+140,LAVENDER);
        wrap(g,battle.message,12,144,285,2,GOLD);
    }
    private void drawAbilities(GuiGraphics g) {
        text(g, "ABILITIES / PP " + battle.pp + "/" + profile.maxPp(), 12, 30, GOLD);
        int i = 0;
        for (Ability a : profile.abilities().stream().skip(abilityPage*6L).limit(6).toList()) {
            text(g,a.scaling+" "+mod(profile.modifier(a.scaling))+" / "+CombatRules.abilityDamage(profile,a)+" DMG",164,48+i*23,FERN);
            String secondary=a==Ability.MEDITATE ? "+4 PP / day" : CombatRules.healing(profile,a)>0 ? "+"+CombatRules.healing(profile,a)+" HP"
                    : a==Ability.CANTRIP ? "+1 DMG / modifier" : CombatRules.abilityDamage(profile,a)>0 ? "+2 DMG / modifier" : "GUARD / shield";
            text(g,secondary,164,58+i*23,LAVENDER);
            i++;
        }
        text(g,"STAT MOD = floor((stat - 10) / 2)",12,187,LAVENDER);
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
        Protocol.send("CLOSE","");
        super.onClose();
    }
}
