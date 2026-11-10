// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/// Pristine fish stack normally; only a single active fish receives native durability components.
public final class PufferfishDurability {
    private PufferfishDurability() {}

    /// Advances at each quarter of spent durability, with the final stage lasting until breakage.
    public static int deflationStage(ItemStack stack) {
        int maximum = stack.getMaxDamage();
        return maximum <= 0 ? 0 : (int) Math.min(3, (long) stack.getDamageValue() * 4 / maximum);
    }

    public static boolean canWearAsGlove(ItemStack stack) {
        return PufferfishItems.isPufferfish(stack)
                && (stack.getMaxDamage() <= 0 || (long) stack.getDamageValue() * 2 <= stack.getMaxDamage());
    }

    public static int maximum(ItemStack stack) {
        var fish = PufferfishItems.flailFish(stack);
        if (PufferfishItems.isHeavy(fish)) return 3047;
        if (fish.is(PufferfishItems.NETHERITE.get()) || fish.is(PufferfishItems.RADIANT.get())) return 2031;
        if (fish.is(PufferfishItems.DIAMOND.get())) return 1561;
        if (fish.is(PufferfishItems.IRON.get())) return 250;
        if (fish.is(PufferfishItems.GOLDEN.get())) return 32;
        if (CopperPufferfish.index(fish) >= 0) return 128;
        return 64;
    }

    public static void activate(ItemStack stack) {
        if (!PufferfishItems.isWeapon(stack) || stack.getCount() != 1) return;
        stack.set(DataComponents.MAX_STACK_SIZE, 1);
        stack.set(DataComponents.MAX_DAMAGE, maximum(stack));
        if (!stack.has(DataComponents.DAMAGE)) stack.set(DataComponents.DAMAGE, 0);
    }

    public static void normalize(ItemStack stack) {
        if (!PufferfishItems.isWeapon(stack) || stack.getOrDefault(DataComponents.DAMAGE, 0) > 0) return;
        stack.remove(DataComponents.MAX_DAMAGE);
        stack.remove(DataComponents.DAMAGE);
        stack.set(DataComponents.MAX_STACK_SIZE, stack.getItem().getDefaultInstance().getMaxStackSize());
    }

    /// Preserves the fraction of durability spent when changing fish material or crafting a flail.
    public static ItemStack transfer(ItemStack source, ItemStack result) {
        if (result.isEmpty()) return result;
        int damage = source.getOrDefault(DataComponents.DAMAGE, 0);
        if (damage > 0) {
            int oldMaximum = source.getOrDefault(DataComponents.MAX_DAMAGE, maximum(source));
            activate(result);
            result.setDamageValue(Math.min(maximum(result) - 1, (int) Math.ceil((double) damage * maximum(result) / oldMaximum)));
        } else normalize(result);
        return result;
    }

    public static void damage(ItemStack stack, ServerLevel level, LivingEntity owner, EquipmentSlot slot) {
        if (!PufferfishItems.isWeapon(stack) || stack.getCount() != 1 || owner != null && owner.hasInfiniteMaterials()) return;
        activate(stack);
        stack.hurtAndBreak(1, level, owner, item -> {
            if (owner != null && slot != null) owner.onEquippedItemBroken(item, slot);
        });
        normalize(stack);
    }

    public static void beforeAttack(AttackEntityEvent event) {
        var player = event.getEntity();
        var stack = player.getMainHandItem();
        if (player.level().isClientSide || player.hasInfiniteMaterials() || !PufferfishItems.isPufferfish(stack)) return;
        if (stack.getCount() > 1) {
            int capacity = 0;
            for (var slot : player.getInventory().items) {
                if (slot == stack) continue;
                if (slot.isEmpty()) capacity += stack.getMaxStackSize();
                else if (ItemStack.isSameItemSameComponents(slot, stack)) capacity += Math.max(0, slot.getMaxStackSize() - slot.getCount());
            }
            if (capacity < stack.getCount() - 1) {
                event.setCanceled(true);
                player.displayClientMessage(Component.translatable("message.kaleidoscope_pufferfish.split_fish_before_attack"), true);
                return;
            }
            var remainder = stack.copyWithCount(stack.getCount() - 1);
            stack.setCount(1);
            activate(stack);
            player.getInventory().add(remainder);
        } else activate(stack);
    }

    public static void afterDamage(LivingDamageEvent.Post event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)
                || event.getNewDamage() + event.getReduction(DamageContainer.Reduction.ABSORPTION) <= 0) return;
        var source = event.getSource();
        if (source.is(DamageTypes.PLAYER_ATTACK) && source.getDirectEntity() instanceof Player player) {
            damage(player.getMainHandItem(), level, player, EquipmentSlot.MAINHAND);
        }
        if (event.getEntity() instanceof Player player && source.getEntity() instanceof LivingEntity
                && !source.is(DamageTypes.THORNS) && PufferfishHeadgear.isWearing(player)) {
            damage(player.getItemBySlot(EquipmentSlot.HEAD), level, player, EquipmentSlot.HEAD);
        }
    }

    public static void beforeDamage(LivingIncomingDamageEvent event) {
        var wearer = event.getEntity();
        if (wearer.level().isClientSide || wearer.hasInfiniteMaterials()) return;
        for (var slot : EquipmentSlot.values()) {
            var stack = wearer.getItemBySlot(slot);
            if (PufferfishItems.isWeapon(stack) && (slot == EquipmentSlot.HEAD || stack.getEnchantments().keySet().stream()
                    .anyMatch(enchantment -> enchantment.is(net.minecraft.world.item.enchantment.Enchantments.THORNS)))) activate(stack);
        }
    }

    public static void repair(net.neoforged.neoforge.event.AnvilUpdateEvent event) {
        var left = event.getLeft();
        var right = event.getRight();
        if (!event.getOutput().isEmpty() || !PufferfishItems.isWeapon(left) || !left.isDamaged() || left.getCount() != 1
                || !right.is(left.getItem()) || right.getOrDefault(DataComponents.DAMAGE, 0) != 0 || !right.getEnchantments().isEmpty()) return;
        long cost = 2L + left.getOrDefault(DataComponents.REPAIR_COST, 0) + right.getOrDefault(DataComponents.REPAIR_COST, 0);
        var result = left.copy();
        result.setDamageValue(0);
        normalize(result);
        String name = event.getName();
        if (name != null && !name.equals(left.getHoverName().getString())) {
            if (name.isBlank()) result.remove(DataComponents.CUSTOM_NAME);
            else result.set(DataComponents.CUSTOM_NAME, Component.literal(name));
            cost++;
        }
        if (cost >= 40 && !event.getPlayer().hasInfiniteMaterials()) { event.setCanceled(true); return; }
        result.set(DataComponents.REPAIR_COST, net.minecraft.world.inventory.AnvilMenu.calculateIncreasedRepairCost(
                Math.max(left.getOrDefault(DataComponents.REPAIR_COST, 0), right.getOrDefault(DataComponents.REPAIR_COST, 0))));
        event.setOutput(result);
        event.setCost(cost);
        event.setMaterialCost(1);
    }

    public static boolean sameWeapon(ItemStack first, ItemStack second) {
        var a = first.copy();
        var b = second.copy();
        for (var component : java.util.List.of(DataComponents.DAMAGE, DataComponents.MAX_DAMAGE, DataComponents.MAX_STACK_SIZE)) {
            a.remove(component);
            b.remove(component);
        }
        return ItemStack.isSameItemSameComponents(a, b);
    }

    /// Restores stacking after native repairs, including experience orbs and anvil repairs.
    public static void tick(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide) return;
        var inventory = event.getEntity().getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) normalize(inventory.getItem(slot));
        normalize(event.getEntity().containerMenu.getCarried());
    }
}
