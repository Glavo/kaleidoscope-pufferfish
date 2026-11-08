// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.compat.create;

import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class PufferfishEngineBlock extends HorizontalKineticBlock implements IBE<PufferfishEngineBlockEntity> {
    public PufferfishEngineBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(HORIZONTAL_FACING, Direction.NORTH));
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        return onBlockEntityUseItemOn(level, pos, engine -> engine.useItem(player, hand));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.isShiftKeyDown() || !player.getMainHandItem().isEmpty()) {
            return InteractionResult.PASS;
        }
        return onBlockEntityUse(level, pos, engine -> engine.takeFish(player));
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos from, boolean moving) {
        if (!level.isClientSide()) {
            withBlockEntityDo(level, pos, PufferfishEngineBlockEntity::refreshPower);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!state.is(replacement.getBlock()) && !level.isClientSide()) {
            withBlockEntityDo(level, pos, PufferfishEngineBlockEntity::dropFish);
        }
        super.onRemove(state, level, pos, replacement, moving);
    }

    @Override
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(HORIZONTAL_FACING).getOpposite();
    }

    @Override
    protected boolean areStatesKineticallyEquivalent(BlockState previous, BlockState next) {
        return previous.is(next.getBlock()) && previous.getValue(HORIZONTAL_FACING) == next.getValue(HORIZONTAL_FACING);
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(HORIZONTAL_FACING).getAxis();
    }

    @Override
    public boolean hideStressImpact() {
        return true;
    }

    @Override
    public Class<PufferfishEngineBlockEntity> getBlockEntityClass() {
        return PufferfishEngineBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends PufferfishEngineBlockEntity> getBlockEntityType() {
        return CreateIntegration.ENGINE_ENTITY.get();
    }
}
