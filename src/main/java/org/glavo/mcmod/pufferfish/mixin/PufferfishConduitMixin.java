// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.mixin;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ConduitBlockEntity;
import org.glavo.mcmod.pufferfish.PufferfishConduitBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ConduitBlockEntity.class)
public abstract class PufferfishConduitMixin {
    @Inject(method = "applyEffects", at = @At("HEAD"), cancellable = true)
    private static void pufferfishPlease$power(Level level, BlockPos pos, List<BlockPos> frame, CallbackInfo callback) {
        if (level.getBlockEntity(pos) instanceof PufferfishConduitBlockEntity) {
            PufferfishConduitBlockEntity.applyPower(level, pos, frame.size());
            callback.cancel();
        }
    }
}
