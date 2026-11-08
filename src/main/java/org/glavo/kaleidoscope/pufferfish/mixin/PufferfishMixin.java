// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.mixin;

import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.item.DyeColor;
import org.glavo.kaleidoscope.pufferfish.ColoredPufferfish;
import org.glavo.kaleidoscope.pufferfish.PufferfishColors;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Pufferfish.class)
public abstract class PufferfishMixin implements ColoredPufferfish {
    @Unique
    private static final EntityDataAccessor<Integer> kaleidoscope$COLOR =
            SynchedEntityData.defineId(Pufferfish.class, EntityDataSerializers.INT);

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void kaleidoscope$defineColor(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(kaleidoscope$COLOR, -1);
    }

    @Override
    @Nullable
    public DyeColor kaleidoscope$getColor() {
        int color = ((Pufferfish) (Object) this).getEntityData().get(kaleidoscope$COLOR);
        return color < 0 ? null : DyeColor.byId(color);
    }

    @Override
    public void kaleidoscope$setColor(@Nullable DyeColor color) {
        ((Pufferfish) (Object) this).getEntityData().set(kaleidoscope$COLOR, color == null ? -1 : color.getId());
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void kaleidoscope$saveColor(CompoundTag tag, CallbackInfo ci) {
        DyeColor color = kaleidoscope$getColor();
        if (color != null) {
            tag.putString(PufferfishColors.COLOR_TAG, color.getName());
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void kaleidoscope$loadColor(CompoundTag tag, CallbackInfo ci) {
        kaleidoscope$setColor(DyeColor.byName(tag.getString(PufferfishColors.COLOR_TAG), null));
    }
}
