// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.item.ItemStack;
import org.glavo.mcmod.pufferfish.PufferfishCatArmorItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Mob.class)
public abstract class PufferfishCatArmorMixin {
    @ModifyReturnValue(method = "canUseSlot", at = @At("RETURN"))
    private boolean pufferfishPlease$bodySlot(boolean original, EquipmentSlot slot) {
        return original || (Object) this instanceof Cat && slot == EquipmentSlot.BODY;
    }

    @ModifyReturnValue(method = "isBodyArmorItem", at = @At("RETURN"))
    private boolean pufferfishPlease$catArmor(boolean original, ItemStack stack) {
        return original || (Object) this instanceof Cat && stack.getItem() instanceof PufferfishCatArmorItem;
    }
}
