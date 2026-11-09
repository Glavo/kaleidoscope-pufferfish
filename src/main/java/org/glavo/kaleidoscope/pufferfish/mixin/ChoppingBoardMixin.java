// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.glavo.kaleidoscope.pufferfish.PufferfishBlocks;
import org.glavo.kaleidoscope.pufferfish.PufferfishItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/// Adds the matching skin when Cookery's board releases pufferfish sashimi.
@Pseudo
@Mixin(targets = "com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.ChoppingBoardBlockEntity", remap = false)
public abstract class ChoppingBoardMixin {
    /// The board's persisted input, including its dye component.
    @Shadow private ItemStack currentCutStack;

    /// Drops one skin on the server immediately before the completed sashimi is released.
    @ModifyArg(method = "onCutItem", index = 2, at = @At(value = "INVOKE", target =
            "Lcom/github/ysbbbbbb/kaleidoscopecookery/blockentity/kitchen/ChoppingBoardBlockEntity;popResource(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)V"))
    private ItemStack kaleidoscope$releaseSkin(Level level, BlockPos pos, ItemStack result) {
        if (!level.isClientSide && result.is(PufferfishItems.SASHIMI.get())) {
            var block = PufferfishBlocks.skinForFish(currentCutStack);
            if (block == null) return result;
            var skin = new ItemStack(block);
            var entity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.25, pos.getZ() + 0.5, skin, 0, 0, 0);
            entity.setDefaultPickUpDelay();
            level.addFreshEntity(entity);
        }
        return result;
    }
}
