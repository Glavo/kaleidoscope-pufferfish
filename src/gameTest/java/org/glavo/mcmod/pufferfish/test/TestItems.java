// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.test;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.glavo.mcmod.pufferfish.PufferfishDurability;
import org.glavo.mcmod.pufferfish.PufferfishVariant;

public final class TestItems {
    private TestItems() {}

    public static ItemStack worn(ItemStack original) {
        var result = original.copyWithCount(1);
        result.set(DataComponents.MAX_STACK_SIZE, 1);
        result.set(DataComponents.MAX_DAMAGE, PufferfishDurability.maximum(original));
        result.set(DataComponents.DAMAGE, original.getOrDefault(DataComponents.DAMAGE, 0) + 1);
        return result;
    }

    public static ItemStack enchant(GameTestHelper helper, ItemStack stack, ResourceKey<Enchantment> key, int level) {
        var holder = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
        EnchantmentHelper.updateEnchantments(stack, enchantments -> enchantments.set(holder, level));
        return stack;
    }

    public static List<ItemStack> fishVariants() {
        return PufferfishVariant.ALL.stream().map(PufferfishVariant::fish).toList();
    }
}
