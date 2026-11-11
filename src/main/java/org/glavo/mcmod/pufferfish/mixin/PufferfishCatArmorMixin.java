// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.glavo.mcmod.pufferfish.PufferfishCatArmorItem;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Cat.class)
public abstract class PufferfishCatArmorMixin extends TamableAnimal {
    protected PufferfishCatArmorMixin(EntityType<? extends TamableAnimal> type, Level level) { super(type, level); }

    @Override
    public boolean canUseSlot(EquipmentSlot slot) { return slot == EquipmentSlot.BODY || super.canUseSlot(slot); }

    @Override
    public boolean isBodyArmorItem(ItemStack stack) { return stack.getItem() instanceof PufferfishCatArmorItem; }

    @Override
    protected void hurtArmor(DamageSource source, float amount) { doHurtEquipment(source, amount, EquipmentSlot.BODY); }
}
