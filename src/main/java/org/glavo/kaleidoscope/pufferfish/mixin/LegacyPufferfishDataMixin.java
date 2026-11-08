// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.item.DyeColor;
import org.glavo.kaleidoscope.pufferfish.PufferfishColors;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Pufferfish.class)
public abstract class LegacyPufferfishDataMixin {
    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void kaleidoscope$migrateColor(CompoundTag tag, CallbackInfo ci) {
        Pufferfish fish = (Pufferfish) (Object) this;
        if (PufferfishColors.get(fish) == null) {
            PufferfishColors.set(fish, DyeColor.byName(tag.getString(PufferfishColors.COLOR_TAG), null));
        }
    }
}
