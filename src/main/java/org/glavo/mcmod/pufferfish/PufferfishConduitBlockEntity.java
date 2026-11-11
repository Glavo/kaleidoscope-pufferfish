// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ConduitBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class PufferfishConduitBlockEntity extends ConduitBlockEntity {
    public PufferfishConduitBlockEntity(BlockPos pos, BlockState state) { super(pos, state); }

    @Override
    public BlockEntityType<?> getType() { return PufferfishBlocks.CONDUIT_ENTITY.get(); }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PufferfishConduitBlockEntity entity) {
        ConduitBlockEntity.serverTick(level, pos, state, entity);
        if (level.getGameTime() % 40 != 0) return;
        int frameSize = 0;
        if (entity.isActive()) {
            for (var offset : BlockPos.betweenClosed(-2, -2, -2, 2, 2, 2)) {
                int x = Math.abs(offset.getX());
                int y = Math.abs(offset.getY());
                int z = Math.abs(offset.getZ());
                if ((x == 0 && (y == 2 || z == 2)) || (y == 0 && (x == 2 || z == 2))
                        || (z == 0 && (x == 2 || y == 2))) {
                    var framePos = pos.offset(offset);
                    if (level.getBlockState(framePos).isConduitFrame(level, framePos, pos)) frameSize++;
                }
            }
        }
        PufferfishConduitSpawning.update(level, pos, frameSize / 7 * 16);
    }

    @Override
    public void setRemoved() {
        PufferfishConduitSpawning.update(level, worldPosition, 0);
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        PufferfishConduitSpawning.update(level, worldPosition, 0);
        super.onChunkUnloaded();
    }
}
