// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.glavo.kaleidoscope.pufferfish.PufferfishBlocks;
import org.glavo.kaleidoscope.pufferfish.PufferfishColors;
import org.glavo.kaleidoscope.pufferfish.PufferfishItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/// Adds the matching skin when Cookery's single-output board releases pufferfish meat.
@Pseudo
@Mixin(targets = "com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.ChoppingBoardBlockEntity", remap = false)
public abstract class ChoppingBoardMixin {
    /// The board's persisted input, including its dye component.
    @Shadow private ItemStack currentCutStack;
    /// The primary output stored by the board when the input is inserted.
    @Shadow private ItemStack result;

    /// Drops one skin on the server immediately before the completed meat is released.
    @Inject(method = "onCutItem", at = @At(value = "INVOKE", target =
            "Lcom/github/ysbbbbbb/kaleidoscopecookery/blockentity/kitchen/ChoppingBoardBlockEntity;popResource(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)V"))
    private void kaleidoscope$releaseSkin(Level level, LivingEntity user, ItemStack cutter,
            CallbackInfoReturnable<Boolean> callback) {
        if (!level.isClientSide && currentCutStack.is(Items.PUFFERFISH) && result.is(PufferfishItems.MEAT.get())) {
            var pos = ((BlockEntity) (Object) this).getBlockPos();
            var skin = new ItemStack(PufferfishBlocks.skin(PufferfishColors.get(currentCutStack)));
            var entity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.25, pos.getZ() + 0.5, skin, 0, 0, 0);
            entity.setDefaultPickUpDelay();
            level.addFreshEntity(entity);
        }
    }
}
