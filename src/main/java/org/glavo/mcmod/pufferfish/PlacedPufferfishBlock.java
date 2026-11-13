// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PlacedPufferfishBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
    public static final MapCodec<PlacedPufferfishBlock> CODEC = simpleCodec(PlacedPufferfishBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public PlacedPufferfishBlock(Properties properties) {
        super(properties.dynamicShape());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, false).setValue(LIT, false));
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) { return true; }

    @Override
    protected void randomTick(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
        CopperPufferfish.randomTick(level, pos, random);
    }

    @Override
    protected MapCodec<? extends PlacedPufferfishBlock> codec() { return CODEC; }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        var entity = new PlacedPufferfishBlockEntity(pos, state);
        var fish = PufferfishLamps.fish(this);
        if (!fish.isEmpty()) entity.setFish(fish);
        return entity;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, PufferfishBlocks.PLACED_FISH_ENTITY.get(), PlacedPufferfishBlockEntity::serverTick);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        var cultivation = stack.get(PufferfishPlease.CULTIVATION.get());
        if (cultivation != null && PufferfishLamps.isLamp(this) && level.getBlockEntity(pos) instanceof PlacedPufferfishBlockEntity entity) {
            var fish = entity.fish();
            fish.set(PufferfishPlease.CULTIVATION.get(), cultivation);
            entity.setFish(fish);
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        var facing = state.getValue(FACING);
        return level.getBlockEntity(pos) instanceof PlacedPufferfishBlockEntity entity
                ? entity.shape(facing) : PufferfishSize.placedShape(ItemStack.EMPTY, facing);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, WATERLOGGED, LIT); }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) { return state.rotate(mirror.getRotation(state.getValue(FACING))); }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (PufferfishLamps.isLamp(this)) return List.of(lampStack(params.getOptionalParameter(LootContextParams.BLOCK_ENTITY)));
        return params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof PlacedPufferfishBlockEntity fish && !fish.fish().isEmpty()
                ? List.of(fish.fish()) : List.of();
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        if (PufferfishLamps.isLamp(this)) return lampStack(level.getBlockEntity(pos));
        return level.getBlockEntity(pos) instanceof PlacedPufferfishBlockEntity fish ? fish.fish() : ItemStack.EMPTY;
    }

    private ItemStack lampStack(BlockEntity entity) {
        var stack = new ItemStack(this);
        if (entity instanceof PlacedPufferfishBlockEntity fish) {
            var cultivation = fish.fish().get(PufferfishPlease.CULTIVATION.get());
            if (cultivation != null) stack.set(PufferfishPlease.CULTIVATION.get(), cultivation);
        }
        return stack;
    }
}
