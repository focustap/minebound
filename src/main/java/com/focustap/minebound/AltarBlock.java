package com.focustap.minebound;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Opens a completely menu-based RPG. Never teleports the Minecraft player. */
public final class AltarBlock extends Block {
    public AltarBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            ClientEntry.openAltar();
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
