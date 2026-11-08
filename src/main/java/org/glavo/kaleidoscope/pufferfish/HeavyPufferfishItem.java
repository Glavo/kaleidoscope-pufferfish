// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;

/// A wearable pufferfish with native mace attacks and no durability consumption.
public final class HeavyPufferfishItem extends MaceItem {
    /// Creates a heavy pufferfish with the supplied equipment and item properties.
    public HeavyPufferfishItem(Properties properties) {
        super(properties);
    }

    /// Returns the head slot for inventory equipment interactions.
    @Override
    public EquipmentSlot getEquipmentSlot(ItemStack stack) {
        return EquipmentSlot.HEAD;
    }

    /// Resets fall distance after smash-hit enchantments without consuming or damaging the fish.
    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (canSmashAttack(attacker)) attacker.resetFallDistance();
    }
}
