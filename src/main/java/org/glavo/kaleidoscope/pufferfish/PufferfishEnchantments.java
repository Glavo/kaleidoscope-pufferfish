// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import net.minecraft.core.component.DataComponents;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.Holder;
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

    public static boolean supports(Holder<Enchantment> enchantment) {
        return enchantment.is(Enchantments.AQUA_AFFINITY) || enchantment.is(Enchantments.LOYALTY)
                || enchantment.is(Enchantments.DENSITY) || enchantment.is(Enchantments.BREACH)
                || enchantment.is(Enchantments.PUNCH) || enchantment.is(Enchantments.WIND_BURST)
                || enchantment.is(Enchantments.SMITE) || enchantment.is(Enchantments.BANE_OF_ARTHROPODS)
                || enchantment.is(Enchantments.VANISHING_CURSE) || enchantment.is(Enchantments.BINDING_CURSE)
                || enchantment.is(Enchantments.THORNS);
    }

    public static void applyBook(AnvilUpdateEvent event) {
        var fish = event.getLeft();
        var book = event.getRight();
        if (!event.getOutput().isEmpty() || !PufferfishItems.isWeapon(fish) || !book.is(Items.ENCHANTED_BOOK)) {
            return;
        }
        var stored = book.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        // Sort registry IDs so conflicting mixed books have a stable result.
        List<Holder<Enchantment>> supported = stored.keySet().stream().filter(PufferfishEnchantments::supports)
                .filter(enchantment -> !PufferfishItems.isFlail(fish)
                        || !enchantment.is(Enchantments.AQUA_AFFINITY) && !enchantment.is(Enchantments.LOYALTY)
                            && !enchantment.is(Enchantments.WIND_BURST) && !enchantment.is(Enchantments.BINDING_CURSE))
                .sorted(Comparator.comparing(holder -> holder.getRegisteredName())).toList();
        if (supported.isEmpty()) {
            return;
        }
        var existing = EnchantmentHelper.getEnchantmentsForCrafting(fish);
        if (fish.getCount() != 1 || !fish.isBookEnchantable(book)) {
            event.setCanceled(true);
            return;
        }
        var enchantments = new ItemEnchantments.Mutable(existing);
        long enchantmentCost = 0;
        boolean applied = false;
        for (var enchantment : supported) {
            boolean compatible = true;
            for (var other : enchantments.keySet()) {
                if (!other.equals(enchantment) && !Enchantment.areCompatible(enchantment, other)) {
                    compatible = false;
                    enchantmentCost++;
                }
            }
            if (!compatible) continue;
            int current = enchantments.getLevel(enchantment);
            int incoming = stored.getLevel(enchantment);
            int level = Math.min(enchantment.value().getMaxLevel(), current == incoming ? current + 1 : Math.max(current, incoming));
            enchantments.set(enchantment, level);
            enchantmentCost += (long) Math.max(1, enchantment.value().getAnvilCost() / 2) * level;
            applied = true;
        }
        if (!applied) {
            event.setCanceled(true);
            return;
        }
        var result = fish.copy();
        EnchantmentHelper.setEnchantments(result, enchantments.toImmutable());
        int previousCost = fish.getOrDefault(DataComponents.REPAIR_COST, 0);
        int bookCost = book.getOrDefault(DataComponents.REPAIR_COST, 0);
        long cost = (long) previousCost + bookCost + enchantmentCost;
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
