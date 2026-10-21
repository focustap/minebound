package com.focustap.minebound;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Two-block stone pedestal and suspended pact crystal. Either half opens the same altar. */
public final class AltarBlock extends Block {
    public static final EnumProperty<DoubleBlockHalf> HALF=BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    // Missing properties in an old world's single-block altar resolve to this safe legacy state.
    public static final BooleanProperty ASSEMBLED=BooleanProperty.create("assembled");
    private static final VoxelShape BASE=Shapes.or(box(1,0,1,15,3,15),box(4,3,4,12,11,12),box(0,11,0,16,16,16));
    private static final VoxelShape CROWN_NS=box(2,0,5,14,14,11),CROWN_EW=box(5,0,2,11,14,14);
    public AltarBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HALF,DoubleBlockHalf.LOWER).setValue(FACING,Direction.NORTH).setValue(ASSEMBLED,false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(HALF,FACING,ASSEMBLED); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos=context.getClickedPos();
        if (pos.getY()>=context.getLevel().getMaxBuildHeight()-1 || !context.getLevel().getBlockState(pos.above()).canBeReplaced(context)) return null;
        return defaultBlockState().setValue(FACING,context.getHorizontalDirection().getOpposite()).setValue(ASSEMBLED,true);
    }
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,LivingEntity placer,ItemStack stack) {
        state=state.setValue(ASSEMBLED,true); level.setBlock(pos,state,2);
        level.setBlock(pos.above(),state.setValue(HALF,DoubleBlockHalf.UPPER),3);
    }
    @Override public BlockState updateShape(BlockState state,Direction side,BlockState neighbor,LevelAccessor level,BlockPos pos,BlockPos neighborPos) {
        boolean lower=state.getValue(HALF)==DoubleBlockHalf.LOWER;
        if ((!lower || state.getValue(ASSEMBLED)) && side==(lower ? Direction.UP : Direction.DOWN)
                && (!neighbor.is(this) || neighbor.getValue(HALF)==state.getValue(HALF))) return Blocks.AIR.defaultBlockState();
        return super.updateShape(state,side,neighbor,level,pos,neighborPos);
    }
    @Override public BlockState playerWillDestroy(Level level,BlockPos pos,BlockState state,Player player) {
        if (!level.isClientSide && state.getValue(HALF)==DoubleBlockHalf.UPPER) {
            BlockPos base=pos.below(); BlockState lower=level.getBlockState(base);
            if (lower.is(this) && lower.getValue(HALF)==DoubleBlockHalf.LOWER) {
                if (!player.isCreative()) Block.dropResources(lower,level,base,level.getBlockEntity(base),player,player.getMainHandItem());
                level.setBlock(base,Blocks.AIR.defaultBlockState(),35);
            }
        }
        return super.playerWillDestroy(level,pos,state,player);
    }
    @Override public VoxelShape getShape(BlockState state,net.minecraft.world.level.BlockGetter level,BlockPos pos,CollisionContext context) {
        return state.getValue(HALF)==DoubleBlockHalf.LOWER ? BASE : state.getValue(FACING).getAxis()==Direction.Axis.Z ? CROWN_NS : CROWN_EW;
    }
    @Override public InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit) {
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)
            com.focustap.minebound.server.CharacterService.open(serverPlayer);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
