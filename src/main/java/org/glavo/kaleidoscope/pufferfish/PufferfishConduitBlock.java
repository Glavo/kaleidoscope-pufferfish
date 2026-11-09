// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ConduitBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ConduitBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class PufferfishConduitBlock extends ConduitBlock {
    public static final MapCodec<ConduitBlock> CODEC = simpleCodec(PufferfishConduitBlock::new);

    public PufferfishConduitBlock(Properties properties) { super(properties); }

    @Override
    public MapCodec<ConduitBlock> codec() { return CODEC; }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PufferfishConduitBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, PufferfishBlocks.CONDUIT_ENTITY.get(),
                level.isClientSide ? ConduitBlockEntity::clientTick : PufferfishConduitBlockEntity::serverTick);
    }
}
