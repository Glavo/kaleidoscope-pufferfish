// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import org.glavo.kaleidoscope.pufferfish.ColoredPufferfish;
import org.glavo.kaleidoscope.pufferfish.PufferfishColors;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractFish.class)
public abstract class AbstractFishMixin {
    @Inject(method = "saveToBucketTag", at = @At("TAIL"))
    private void kaleidoscope$saveBucketColor(ItemStack stack, CallbackInfo ci) {
        if (this instanceof ColoredPufferfish fish && fish.kaleidoscope$getColor() != null) {
            PufferfishColors.dye(stack, fish.kaleidoscope$getColor());
        }
    }

    @Inject(method = "loadFromBucketTag", at = @At("TAIL"))
    private void kaleidoscope$loadBucketColor(CompoundTag tag, CallbackInfo ci) {
        if (this instanceof ColoredPufferfish fish) {
            fish.kaleidoscope$setColor(DyeColor.byName(tag.getString(PufferfishColors.COLOR_TAG), null));
        }
    }
}
