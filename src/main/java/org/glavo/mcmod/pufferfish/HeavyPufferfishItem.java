// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/// A wearable pufferfish with falling smash attacks.
public final class HeavyPufferfishItem extends Item {
    /// Creates a heavy pufferfish with the supplied equipment and item properties.
    public HeavyPufferfishItem(Properties properties) {
        super(properties);
    }

    /// Returns the head slot for inventory equipment interactions.
    @Override
    public EquipmentSlot getEquipmentSlot(ItemStack stack) {
        return EquipmentSlot.HEAD;
    }

    /// Retains the heavy fish's enchantability independently of its combat implementation.
    @Override
    public int getEnchantmentValue() {
        return 15;
    }

    @Override
    public float getAttackDamageBonus(Entity target, float damage, DamageSource source) {
        if (source.getDirectEntity() instanceof LivingEntity attacker && attacker.getWeaponItem().getItem() == this
                && PufferfishCombat.canSmash(attacker, attacker.getWeaponItem())) {
            return PufferfishCombat.smashBonus(attacker.getWeaponItem(), attacker.fallDistance);
        }
        return 0;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!PufferfishCombat.canSmash(attacker, stack)) return false;
        PufferfishCombat.smashHit(attacker, target);
        return true;
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (PufferfishCombat.canSmash(attacker, stack)) attacker.resetFallDistance();
    }
}
