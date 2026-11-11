// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.glavo.mcmod.pufferfish.PufferfishCatArmorItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class PufferfishEquipmentMixin {
    @Shadow protected abstract void doHurtEquipment(DamageSource source, float amount, EquipmentSlot... slots);

    @ModifyReturnValue(method = "getEquipmentSlotForItem", at = @At("RETURN"))
    private EquipmentSlot pufferfishPlease$pufferfishHeadSlot(EquipmentSlot original, ItemStack stack) {
        return original == EquipmentSlot.MAINHAND && stack.is(Items.PUFFERFISH) ? EquipmentSlot.HEAD : original;
    }

    @Inject(method = "hurtArmor", at = @At("TAIL"))
    private void pufferfishPlease$damageCatArmor(DamageSource source, float amount, CallbackInfo callback) {
        if ((Object) this instanceof Cat cat && cat.getBodyArmorItem().getItem() instanceof PufferfishCatArmorItem) {
            doHurtEquipment(source, amount, EquipmentSlot.BODY);
        }
    }
}
