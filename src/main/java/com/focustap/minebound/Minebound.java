package com.focustap.minebound;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SoundType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import com.focustap.minebound.net.Protocol;
import com.focustap.minebound.server.CharacterService;
import net.neoforged.neoforge.common.NeoForge;
import com.focustap.minebound.server.ActivityXp;
import com.focustap.minebound.server.XpConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;

@Mod(Minebound.MOD_ID)
public final class Minebound {
    public static final String MOD_ID = "minebound";
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);

    public static final DeferredBlock<AltarBlock> ALTAR = BLOCKS.registerBlock(
            "altar", AltarBlock::new, BlockBehaviour.Properties.of().strength(2.5f, 6.0f).sound(SoundType.AMETHYST)
    );
    public static final DeferredItem<?> ALTAR_ITEM = ITEMS.registerSimpleBlockItem("altar", ALTAR);
    public static final DeferredItem<ResetRelic> RESET_RELIC = ITEMS.registerItem("reset_relic", ResetRelic::new,
            new net.minecraft.world.item.Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC));

    public Minebound(IEventBus modBus, ModContainer container) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        modBus.addListener(this::onCreativeTab);
        modBus.addListener(Protocol::register);
        NeoForge.EVENT_BUS.addListener(CharacterService::tick);
        NeoForge.EVENT_BUS.addListener(CharacterService::logout);
        NeoForge.EVENT_BUS.addListener(CharacterService::stopped);
        container.registerConfig(ModConfig.Type.SERVER,XpConfig.SPEC);
        NeoForge.EVENT_BUS.addListener(ActivityXp::tick);
        NeoForge.EVENT_BUS.addListener(ActivityXp::breakBlock);
        // Snapshot maturity before right-click harvesting mods handle/cancel the interaction.
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGHEST,true,ActivityXp::rightClick);
        NeoForge.EVENT_BUS.addListener(ActivityXp::placed);
        NeoForge.EVENT_BUS.addListener(ActivityXp::spawned);
        NeoForge.EVENT_BUS.addListener(ActivityXp::killed);
        NeoForge.EVENT_BUS.addListener(ActivityXp::useRod);
        NeoForge.EVENT_BUS.addListener(ActivityXp::fished);
    }

    private void onCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ALTAR_ITEM);
            event.accept(RESET_RELIC);
        }
    }
}
