package com.focustap.minebound;

import com.focustap.minebound.net.Protocol;
import com.focustap.minebound.rpg.*;
import com.focustap.minebound.server.CharacterService;
import com.focustap.minebound.server.ActivityXp;
import java.io.File;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Runs only with -PmineboundSmoke; creates its own throwaway single-player world. */
@EventBusSubscriber(modid=Minebound.MOD_ID,value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class SmokeScenario {
    private static int state,wait,age;
    private static volatile Protocol.Snapshot snapshot;
    private static volatile Throwable failure;
    private static volatile boolean serverDone;
    private static int carriedHp,carriedPp,beforeXp;
    @SubscribeEvent public static void setup(FMLClientSetupEvent e) {
        System.out.println("MINEBOUND_SMOKE setup enabled="+Boolean.getBoolean("minebound.smoke"));
        if (Boolean.getBoolean("minebound.smoke")) e.enqueueWork(() -> NeoForge.EVENT_BUS.addListener(SmokeScenario::tick));
    }
    private static void check(boolean condition,String message) { if (!condition) throw new IllegalStateException(message); }
    private static void advance() { state++; wait=20; age=0; System.out.println("MINEBOUND_SMOKE state="+state); }
    private static void press(String label) {
        Minecraft mc=Minecraft.getInstance();
        Button button=mc.screen.children().stream().filter(c -> c instanceof Button).map(c -> (Button)c)
                .filter(b -> b.active && b.getMessage().getString().startsWith(label)).findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing button "+label+" in "+mc.screen.getClass().getSimpleName()));
        check(mc.screen.mouseClicked(button.getX()+button.getWidth()/2.,button.getY()+button.getHeight()/2.,0),"Click was not handled: "+label);
        if (mc.screen!=null) mc.screen.mouseReleased(button.getX()+button.getWidth()/2.,button.getY()+button.getHeight()/2.,0);
    }
    private static void shot(String name) {
        Minecraft mc=Minecraft.getInstance();
        Screenshot.grab(mc.gameDirectory,"minebound-"+name+".png",mc.getMainRenderTarget(),c -> System.out.println("MINEBOUND_SMOKE screenshot "+name));
    }
    private static void server(Consumer<ServerPlayer> action) {
        Minecraft mc=Minecraft.getInstance(); var id=mc.player.getUUID(); serverDone=false;
        mc.getSingleplayerServer().execute(() -> {
            try { action.accept(mc.getSingleplayerServer().getPlayerList().getPlayer(id)); serverDone=true; }
            catch (Throwable e) { failure=e; }
        });
    }
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc=Minecraft.getInstance();
        if (failure!=null) {
            failure.printStackTrace(); System.out.println("MINEBOUND_SMOKE FAILED state="+state);
            result(mc,"FAIL state="+state+" "+failure); mc.stop(); return;
        }
        if (wait>0) { wait--; return; }
        if (++age>1200) { failure=new IllegalStateException("Timed out in state "+state); return; }
        try {
            if (age%100==0) System.out.println("MINEBOUND_SMOKE waiting state="+state+" screen="+(mc.screen==null ? "none" : mc.screen.getClass().getName()));
            switch(state) {
                case 0 -> {
                    if (!(mc.screen instanceof TitleScreen)) {
                        if (mc.screen!=null && mc.screen.getClass().getSimpleName().equals("AccessibilityOnboardingScreen")) press("Continue");
                        return;
                    }
                    var receiver=Protocol.clientReceiver; Protocol.clientReceiver=s -> { snapshot=s; receiver.accept(s); };
                    mc.options.guiScale().set(2); mc.resizeDisplay();
                    mc.options.tutorialStep=net.minecraft.client.tutorial.TutorialSteps.NONE; mc.getTutorial().stop();
                    mc.createWorldOpenFlows().createFreshLevel("minebound-smoke-"+System.currentTimeMillis(),
                            new LevelSettings("Minebound automated smoke",GameType.CREATIVE,false,Difficulty.NORMAL,true,new GameRules(),WorldDataConfiguration.DEFAULT),
                            new WorldOptions(428114L,false,false),WorldPresets::createNormalWorldDimensions,null);
                    advance();
                }
                case 1 -> {
                    if (mc.player==null || mc.getSingleplayerServer()==null) return;
                    server(p -> {
                        p.serverLevel().getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,p.server);
                        BlockPos pos=p.blockPosition().below(); p.serverLevel().setBlockAndUpdate(pos,Minebound.ALTAR.get().defaultBlockState());
                        Minebound.ALTAR.get().useWithoutItem(Minebound.ALTAR.get().defaultBlockState(),p.serverLevel(),pos,p,
                                new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));
                    }); advance();
                }
                case 2 -> { if (!(mc.screen instanceof StoryScreen)) return; shot("01-intro"); press("SKIP"); advance(); }
                case 3 -> { if (!(mc.screen instanceof CharacterCreationScreen)) return; press("Gnome"); press("NEXT"); press("Wizard"); press("NEXT"); advance(); }
                case 4 -> {
                    // Twelve points, six INT and six WIS, through actual allocation controls.
                    var screen=(RetroScreen)mc.screen;
                    for (int i=0;i<6;i++) mc.screen.mouseClicked(screen.x()+142,screen.y()+122,0);
                    for (int i=0;i<6;i++) mc.screen.mouseClicked(screen.x()+142,screen.y()+144,0);
                    advance();
                }
                case 5 -> { shot("02-creation"); press("NEXT"); mc.screen.children().stream().filter(c -> c instanceof EditBox).map(c -> (EditBox)c).findFirst().orElseThrow().setValue("Aster"); advance(); }
                case 6 -> { shot("03-review"); press("CONFIRM"); advance(); }
                case 7 -> {
                    if (!(mc.screen instanceof MineboundScreen) || snapshot==null || !snapshot.profile().created) return;
                    check(snapshot.profile().skillPoints==1,"Initial skill point"); check(snapshot.profile().startingClass==CharacterClass.WIZARD,"Class was not saved");
                    shot("04-map"); press("SHEET"); advance();
                }
                case 8 -> { shot("05-sheet"); press("SKILL WEB"); advance(); }
                case 9 -> {
                    check(mc.screen instanceof SkillWebScreen,"Web not open"); shot("06-web");
                    var screen=(SkillWebScreen)mc.screen;
                    var sx=SkillWebScreen.class.getDeclaredMethod("nx",SkillGraph.Node.class); sx.setAccessible(true);
                    var sy=SkillWebScreen.class.getDeclaredMethod("ny",SkillGraph.Node.class); sy.setAccessible(true);
                    var node=SkillGraph.NODES.get("arcane_1");
                    screen.mouseClicked((int)sx.invoke(screen,node),(int)sy.invoke(screen,node),0);
                    screen.mouseReleased((int)sx.invoke(screen,node),(int)sy.invoke(screen,node),0);
                    press("LEARN /"); advance();
                }
                case 10 -> {
                    check(snapshot.profile().nodes.contains("arcane_1"),"Server skill purchase failed");
                    shot("07-web-purchased"); press("BACK"); press("MAP"); press("ENTER"); advance();
                }
                case 11 -> {
                    if (snapshot.battle()==null) return;
                    check(snapshot.battle().state==Battle.State.COMMAND,"Battle command state"); shot("08-battle"); press("ABILITY"); advance();
                }
                case 12 -> { shot("09-abilities"); press("SPARK"); advance(); }
                case 13 -> {
                    if (snapshot.battle()==null || snapshot.battle().state!=Battle.State.DODGE) return;
                    shot("10-dodge"); check(snapshot.profile().currentPp<snapshot.profile().maxPp(),"PP not spent on server");
                    carriedHp=snapshot.profile().currentHp; carriedPp=snapshot.profile().currentPp;
                    mc.screen.onClose(); advance();
                }
                case 14 -> { server(p -> CharacterService.open(p)); advance(); }
                case 15 -> {
                    check(snapshot.profile().currentHp<=carriedHp,"Altar refilled HP"); check(snapshot.profile().currentPp==carriedPp,"Altar refilled PP");
                    server(p -> {
                        var s=CharacterService.session(p); s.profile.currentHp=3; s.profile.currentPp=1;
                        p.server.getWorldData().overworldData().setGameTime(CharacterService.now(p)+Recovery.DAY);
                        s.profile.lastRecoveryTick=CharacterService.now(p)-Recovery.DAY; CharacterService.open(p);
                    }); advance();
                }
                case 16 -> {
                    check(snapshot.profile().currentHp==snapshot.profile().maxHp(),"Daily recovery HP");
                    check(snapshot.profile().currentPp==snapshot.profile().maxPp(),"Daily recovery PP");
                    shot("11-recovery");
                    server(p -> {
                        p.setGameMode(GameType.SURVIVAL); p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_PICKAXE));
                        var s=CharacterService.session(p); beforeXp=s.profile.xp;
                        BlockPos ore=p.blockPosition().offset(2,0,0); p.serverLevel().setBlockAndUpdate(ore,Blocks.DIAMOND_ORE.defaultBlockState());
                        check(p.gameMode.destroyBlock(ore),"Mining fixture did not break ore");
                    }); advance();
                }
                case 17 -> {
                    server(p -> {
                        var s=CharacterService.session(p); check(s.profile.xp>beforeXp,"Ore XP missing"); beforeXp=s.profile.xp;
                        BlockPos crop=p.blockPosition().offset(2,0,2); p.serverLevel().setBlockAndUpdate(crop.below(),Blocks.FARMLAND.defaultBlockState());
                        p.serverLevel().setBlockAndUpdate(crop,Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE,7));
                        var hit=new BlockHitResult(Vec3.atCenterOf(crop),Direction.UP,crop,false);
                        // A common right-click harvest/replant mod: interaction then mature->age0.
                        NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickBlock(p,InteractionHand.MAIN_HAND,crop,hit));
                        NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickBlock(p,InteractionHand.MAIN_HAND,crop,hit));
                        p.serverLevel().setBlockAndUpdate(crop,Blocks.WHEAT.defaultBlockState());
                    }); advance();
                }
                case 18 -> {
                    server(p -> {
                        var s=CharacterService.session(p); check(s.profile.xp==beforeXp+2,"Right-click harvest was missing or double counted");
                        beforeXp=s.profile.xp;
                        var zombie=net.minecraft.world.entity.EntityType.ZOMBIE.create(p.serverLevel());
                        check(zombie!=null,"Zombie fixture"); zombie.setPos(p.getX()+2,p.getY(),p.getZ()); p.serverLevel().addFreshEntity(zombie);
                        zombie.hurt(p.damageSources().playerAttack(p),1000);
                        check(s.profile.xp==beforeXp+3,"Personal hostile kill XP missing");
                        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.FISHING_ROD));
                        p.teleportTo(p.getX()+1,p.getY(),p.getZ()); // recent real movement after the clock fixture
                        NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickItem(p,InteractionHand.MAIN_HAND));
                    }); advance(); wait=100;
                }
                case 19 -> {
                    server(p -> {
                        var s=CharacterService.session(p);
                        var hook=new net.minecraft.world.entity.projectile.FishingHook(p,p.serverLevel(),0,0);
                        NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.player.ItemFishedEvent(List.of(new ItemStack(Items.COD)),0,hook));
                        hook.discard();
                        check(s.profile.xp==beforeXp+6,"Manual fishing XP missing");
                        s.battle=new Battle(s.profile,0); s.settled=false; s.battle.hp=10; s.battle.pp=12; s.battle.enemyHp=1;
                        CharacterService.command(p,new Protocol.Command("BASH","",0,0,false));
                        check(s.profile.currentHp==10 && s.profile.currentPp==12,"Victory refilled resources");
                    }); advance();
                }
                case 20 -> {
                    server(p -> {
                        var s=CharacterService.session(p);
                        CharacterService.command(p,new Protocol.Command("GOOD","herb",0,0,false));
                        check(s.profile.currentHp==22,"Settled battle overwrote out-of-combat healing");
                        int hp=s.profile.currentHp,pp=s.profile.currentPp; CharacterService.flush(p,s);
                        CharacterProfile reloaded;
                        try { reloaded=s.store.load(); } catch (java.io.IOException error) { throw new RuntimeException(error); }
                        check(reloaded.currentHp==hp && reloaded.currentPp==pp,"Server profile roundtrip");
                        check(reloaded.nodes.contains("arcane_1"),"Purchased node was not saved");
                        p.setGameMode(GameType.CREATIVE); p.setShiftKeyDown(true);
                        p.getInventory().setItem(7,new ItemStack(Items.DIAMOND,4));
                        Minebound.RESET_RELIC.get().use(p.serverLevel(),p,InteractionHand.MAIN_HAND);
                    }); advance();
                }
                case 21 -> { if (!(mc.screen instanceof ResetConfirmationScreen)) return; shot("12-reset-confirm"); press("CANCEL"); advance(); }
                case 22 -> {
                    server(p -> {
                        check(CharacterService.session(p).profile.created,"Cancelling reset erased the character");
                        Minebound.RESET_RELIC.get().use(p.serverLevel(),p,InteractionHand.MAIN_HAND);
                    }); advance();
                }
                case 23 -> { if (!(mc.screen instanceof ResetConfirmationScreen)) return; press("ERASE MINEBOUND"); advance(); }
                case 24 -> {
                    if (!(mc.screen instanceof StoryScreen)) return;
                    check(!snapshot.profile().created && snapshot.profile().nodes.isEmpty() && snapshot.profile().xp==0,"Reset incomplete");
                    server(p -> {
                        check(p.getInventory().getItem(7).getCount()==4 && p.getInventory().getItem(7).is(Items.DIAMOND),"Reset changed vanilla inventory");
                        try { check(!CharacterService.session(p).store.load().created,"Reset was not reloadable"); }
                        catch (java.io.IOException error) { throw new RuntimeException(error); }
                    });
                    advance();
                }
                case 25 -> {
                    if (!serverDone) return;
                    System.out.println("MINEBOUND_SMOKE PASSED: altar, creation, graph purchase, battle authority, HP/PP persistence, recovery, mining, hostile kills, fishing, right-click harvest dedup, disk reload, reset cancellation/isolation.");
                    shot("13-reset-complete"); press("SKIP"); advance();
                }
                case 26 -> {
                    org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(),1280,800);
                    mc.options.guiScale().set(3); mc.resizeDisplay(); mc.getToasts().clear();
                    press("Elf"); press("NEXT"); press("Warlock"); advance();
                }
                case 27 -> {
                    shot("14-warlock-class"); press("NEXT"); var screen=(RetroScreen)mc.screen;
                    for (int row : new int[]{98,164}) for (int i=0;i<6;i++) {
                        screen.mouseClicked(screen.x()+142,screen.y()+row,0); screen.mouseReleased(screen.x()+142,screen.y()+row,0);
                    }
                    advance();
                }
                case 28 -> { shot("15-warlock-stats"); press("NEXT"); press("CONFIRM"); advance(); }
                case 29 -> {
                    check(snapshot.profile().startingClass==CharacterClass.WARLOCK,"Warlock creation");
                    check(snapshot.profile().abilities().contains(Ability.ELDRITCH_BLAST),"Eldritch starter missing");
                    check(!snapshot.profile().abilities().contains(Ability.PSYCHIC_BLADE),"Psychic Blade should require Bladeslinger");
                    shot("16-world-map"); press("SHEET"); advance();
                }
                case 30 -> { shot("17-warlock-sheet"); press("SKILL WEB"); advance(); }
                case 31 -> { shot("18-bladeslinger"); press("LEARN /"); advance(); }
                case 32 -> {
                    check(snapshot.profile().nodes.contains("psychic_blade_1"),"Bladeslinger node missing");
                    press("BACK"); press("MAP"); press("SHEET"); press("SKILL WEB"); press("BACK"); press("MAP"); press("LORE"); advance();
                }
                case 33 -> { shot("19-lore"); press("MAP"); press("GOODS"); advance(); }
                case 34 -> { shot("20-goods"); press("BACK"); press("ENTER"); advance(); }
                case 35 -> { check(snapshot.battle().state==Battle.State.COMMAND,"Battle did not open"); shot("21-warlock-battle"); press("ABILITY"); advance(); }
                case 36 -> { shot("22-eldritch-scaling"); press("ELDRITCH BLAST"); advance(); }
                case 37 -> {
                    check(snapshot.battle().state==Battle.State.DODGE,"Eldritch Blast did not work");
                    server(p -> { var s=CharacterService.session(p); s.battle.state=Battle.State.COMMAND; s.battle.enemyHp=1; CharacterService.sync(p,s,"UPDATE"); }); advance();
                }
                case 38 -> { press("BASH"); advance(); }
                case 39 -> { check(snapshot.battle().state==Battle.State.WON,"Victory fixture failed"); shot("23-results"); press("WORLD MAP"); press("ENTER"); advance(); }
                case 40 -> {
                    check(snapshot.battle().state==Battle.State.COMMAND,"Repeat battle did not open");
                    press("ABILITY"); press("BACK"); press("DEFEND"); advance();
                }
                case 41 -> {
                    check(snapshot.battle().state==Battle.State.DODGE,"Repeat battle buttons stuck"); shot("24-dodge"); mc.screen.onClose();
                    server(p -> {
                        p.setGameMode(GameType.CREATIVE);
                        var level=p.serverLevel(); BlockPos base=p.blockPosition().offset(0,0,5);
                        for (int dx=-4;dx<=4;dx++) for (int dz=-4;dz<=4;dz++) for (int dy=-1;dy<=4;dy++)
                            level.setBlockAndUpdate(base.offset(dx,dy,dz),dy==-1 ? Blocks.STONE_BRICKS.defaultBlockState() : Blocks.AIR.defaultBlockState());
                        level.setDayTime(6000);
                        BlockPos legacy=base.offset(-3,0,0); level.setBlockAndUpdate(legacy,Minebound.ALTAR.get().defaultBlockState());
                        level.setBlockAndUpdate(legacy.above(),Blocks.DIRT.defaultBlockState()); level.setBlockAndUpdate(legacy.above(),Blocks.AIR.defaultBlockState());
                        check(level.getBlockState(legacy).is(Minebound.ALTAR.get()),"Legacy single-block altar disappeared on neighbor update");
                        level.setBlockAndUpdate(legacy,Blocks.AIR.defaultBlockState());
                        BlockPos blocked=base.offset(3,0,0); level.setBlockAndUpdate(blocked.above(),Blocks.STONE.defaultBlockState());
                        var item=new ItemStack(Minebound.ALTAR_ITEM.get()); p.setItemInHand(InteractionHand.MAIN_HAND,item);
                        var context=new net.minecraft.world.item.context.BlockPlaceContext(p,InteractionHand.MAIN_HAND,item,new BlockHitResult(Vec3.atCenterOf(base.below()),Direction.UP,base.below(),false));
                        check(((net.minecraft.world.item.BlockItem)item.getItem()).place(context).consumesAction(),"Altar item placement");
                        check(level.getBlockState(base.above()).is(Minebound.ALTAR.get()),"Altar upper half missing");
                        check(level.getBlockState(base.above()).getValue(AltarBlock.HALF)==net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER,"Altar upper state");
                        var blockedContext=new net.minecraft.world.item.context.BlockPlaceContext(p,InteractionHand.MAIN_HAND,item,new BlockHitResult(Vec3.atCenterOf(blocked.below()),Direction.UP,blocked.below(),false));
                        check(Minebound.ALTAR.get().getStateForPlacement(blockedContext)==null,"Altar placed through a ceiling");
                        p.teleportTo(p.serverLevel(),base.getX()+3.5,base.getY()+.3,base.getZ()-4.5,java.util.Set.of(),32,0);
                        p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
                    }); mc.options.hideGui=true; advance(); wait=60;
                }
                case 42 -> { shot("25-altar-model"); mc.options.hideGui=false;
                    server(p -> {
                        var level=p.serverLevel(); BlockPos base=p.blockPosition().offset(0,0,3);
                        var lower=Minebound.ALTAR.get().defaultBlockState();
                        level.setBlock(base,lower,3); Minebound.ALTAR.get().setPlacedBy(level,base,lower,p,new ItemStack(Minebound.ALTAR_ITEM.get()));
                        p.setGameMode(GameType.SURVIVAL);
                        var upper=level.getBlockState(base.above()); Minebound.ALTAR.get().playerWillDestroy(level,base.above(),upper,p);
                        level.setBlockAndUpdate(base.above(),Blocks.AIR.defaultBlockState());
                        check(level.getBlockState(base).isAir(),"Altar left an orphan base");
                        long drops=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(base).inflate(1)).stream()
                                .filter(e -> e.getItem().is(Minebound.ALTAR_ITEM.get())).mapToInt(e -> e.getItem().getCount()).sum();
                        check(drops==1,"Breaking upper half must drop exactly one altar, got "+drops);
                    }); advance();
                }
                case 43 -> {
                    if (!serverDone) return;
                    result(mc,"PASS"); System.out.println("MINEBOUND_VISUAL PASSED: Warlock starter, Bladeslinger, real mouse navigation, repeat battle, tall altar placement/ceiling/break/drop."); advance();
                }
                case 44 -> mc.stop();
            }
        } catch (Throwable e) { failure=e; }
    }
    private static void result(Minecraft mc,String result) {
        try {
            var path=mc.gameDirectory.toPath().resolve("../build/smoke-result.txt").normalize();
            java.nio.file.Files.createDirectories(path.getParent()); java.nio.file.Files.writeString(path,result);
        } catch (java.io.IOException e) { throw new RuntimeException(e); }
    }
}
