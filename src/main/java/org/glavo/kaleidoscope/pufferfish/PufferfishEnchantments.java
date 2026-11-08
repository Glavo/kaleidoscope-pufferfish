// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.neoforge.event.AnvilUpdateEvent;

public final class PufferfishEnchantments {
    private PufferfishEnchantments() {}

    public static void applyLoyalty(AnvilUpdateEvent event) {
        var fish = event.getLeft();
        var book = event.getRight();
        if (!event.getOutput().isEmpty() || !PufferfishItems.isPufferfish(fish) || !book.is(Items.ENCHANTED_BOOK)) {
            return;
        }
        var loyalty = event.getPlayer().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LOYALTY);
        int incoming = book.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY).getLevel(loyalty);
        if (incoming <= 0) {
            return;
        }
        var existing = EnchantmentHelper.getEnchantmentsForCrafting(fish);
        if (fish.getCount() != 1 || !fish.isBookEnchantable(book)
                || existing.keySet().stream().anyMatch(other -> !other.equals(loyalty) && !Enchantment.areCompatible(loyalty, other))) {
            event.setCanceled(true);
            return;
        }
        int current = existing.getLevel(loyalty);
        int level = Math.min(loyalty.value().getMaxLevel(), current == incoming ? current + 1 : Math.max(current, incoming));
        var result = fish.copy();
        EnchantmentHelper.updateEnchantments(result, enchantments -> enchantments.set(loyalty, level));
        int previousCost = fish.getOrDefault(DataComponents.REPAIR_COST, 0);
        int bookCost = book.getOrDefault(DataComponents.REPAIR_COST, 0);
        long cost = (long) previousCost + bookCost + (long) Math.max(1, loyalty.value().getAnvilCost() / 2) * level;
        String name = event.getName();
        if (name != null) {
            if (name.isBlank() && result.has(DataComponents.CUSTOM_NAME)) {
                result.remove(DataComponents.CUSTOM_NAME);
                cost++;
            } else if (!name.isBlank() && !name.equals(fish.getHoverName().getString())) {
                result.set(DataComponents.CUSTOM_NAME, Component.literal(name));
                cost++;
            }
        }
        if (cost >= 40 && !event.getPlayer().getAbilities().instabuild) {
            event.setCanceled(true);
            return;
        }
        result.set(DataComponents.REPAIR_COST, AnvilMenu.calculateIncreasedRepairCost(Math.max(previousCost, bookCost)));
        event.setOutput(result);
        event.setCost(cost);
        event.setMaterialCost(1);
    }
}
