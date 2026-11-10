// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

/// A placeable foot pump whose durability and enchantments survive placement.
public final class PufferfishPumpItem extends net.minecraft.world.item.BlockItem {

    public PufferfishPumpItem(Properties properties) { super(PufferfishBlocks.PUMP.get(), properties); }

    public static boolean canInflate(ItemStack fish) {
        return PufferfishItems.isWeapon(fish) && fish.getCount() == 1 && fish.isDamaged();
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) { return 10; }

    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        return enchantment.is(Enchantments.UNBREAKING);
    }

    @Override
    public boolean isPrimaryItemFor(ItemStack stack, Holder<Enchantment> enchantment) {
        return supportsEnchantment(stack, enchantment);
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack ingredient) { return ingredient.is(Items.COPPER_INGOT); }
}
