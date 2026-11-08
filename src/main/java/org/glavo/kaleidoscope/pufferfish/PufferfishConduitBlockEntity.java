// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ConduitBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class PufferfishConduitBlockEntity extends ConduitBlockEntity {
    public PufferfishConduitBlockEntity(BlockPos pos, BlockState state) { super(pos, state); }

    @Override
    public BlockEntityType<?> getType() { return PufferfishBlocks.CONDUIT_ENTITY.get(); }
}
