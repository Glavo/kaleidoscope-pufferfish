// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BrushableBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class SuspiciousAncientPrismarineBlock extends BrushableBlock {
    public static final MapCodec<BrushableBlock> CODEC = simpleCodec(SuspiciousAncientPrismarineBlock::new);

    public SuspiciousAncientPrismarineBlock(Properties properties) {
        super(PufferfishBlocks.ANCIENT_PRISMARINE.get(), SoundEvents.BRUSH_GRAVEL,
                SoundEvents.BRUSH_GRAVEL_COMPLETED, properties);
    }

    @Override
    public MapCodec<BrushableBlock> codec() { return CODEC; }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AncientPrismarineBlockEntity(pos, state);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof BrushableBlockEntity entity) entity.checkReset();
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {}
}
