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

@Mod(Minebound.MOD_ID)
public final class Minebound {
    public static final String MOD_ID = "minebound";
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);

    public static final DeferredBlock<AltarBlock> ALTAR = BLOCKS.registerBlock(
            "altar", AltarBlock::new, BlockBehaviour.Properties.of().strength(2.5f, 6.0f).sound(SoundType.AMETHYST)
    );
    public static final DeferredItem<?> ALTAR_ITEM = ITEMS.registerSimpleBlockItem("altar", ALTAR);

    public Minebound(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        modBus.addListener(this::onCreativeTab);
    }

    private void onCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ALTAR_ITEM);
        }
    }
}
