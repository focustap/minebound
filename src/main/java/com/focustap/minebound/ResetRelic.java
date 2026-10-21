package com.focustap.minebound;

import com.focustap.minebound.server.CharacterService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Creative-only, sneak-use and server confirmation; never touches vanilla progression. */
public final class ResetRelic extends Item {
    public ResetRelic(Properties properties) { super(properties); }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!player.isCreative() || !player.isShiftKeyDown()) return InteractionResultHolder.pass(player.getItemInHand(hand));
        if (player instanceof ServerPlayer serverPlayer) CharacterService.armReset(serverPlayer);
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);
    }
}
