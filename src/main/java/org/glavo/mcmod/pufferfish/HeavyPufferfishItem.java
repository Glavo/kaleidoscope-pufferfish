// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;

/// A wearable pufferfish using shared combat and durability hooks.
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
}
