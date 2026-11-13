// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import org.glavo.mcmod.pufferfish.PufferfishColors;
import org.glavo.mcmod.pufferfish.PufferfishData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractFish.class)
public abstract class AbstractFishMixin {
    @Inject(method = "saveToBucketTag", at = @At("TAIL"))
    private void pufferfishPlease$saveBucketColor(ItemStack stack, CallbackInfo ci) {
        if ((Object) this instanceof Pufferfish fish && !(fish instanceof org.glavo.mcmod.pufferfish.entity.MaterialPufferfish)) {
            if (!PufferfishData.stored(fish).isEmpty()) net.minecraft.world.item.component.CustomData.update(
                    net.minecraft.core.component.DataComponents.BUCKET_ENTITY_DATA, stack,
                    tag -> tag.put("Fish", PufferfishData.copy(fish).save(fish.registryAccess())));
        }
        if ((Object) this instanceof Pufferfish fish && PufferfishColors.get(fish) != null) {
            PufferfishColors.dye(stack, PufferfishColors.get(fish));
        }
    }

    @Inject(method = "loadFromBucketTag", at = @At("HEAD"))
    private void pufferfishPlease$loadBucketColor(CompoundTag tag, CallbackInfo ci) {
        if ((Object) this instanceof Pufferfish fish) {
            if (!(fish instanceof org.glavo.mcmod.pufferfish.entity.MaterialPufferfish) && tag.contains("Fish")) {
                ItemStack.parse(fish.registryAccess(), tag.getCompound("Fish"))
                        .filter(item -> item.is(net.minecraft.world.item.Items.PUFFERFISH))
                        .ifPresent(item -> PufferfishData.set(fish, item));
            }
            PufferfishColors.set(fish, DyeColor.byName(tag.getString(PufferfishColors.COLOR_TAG), null));
        }
    }
}
