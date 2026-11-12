// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
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

/// Fresh fish stack normally; activating one permanently gives it native durability and a stack limit of one.
public final class PufferfishDurability {
    public record State(int maxDamage) {
        public static final Codec<State> CODEC = Codec.intRange(1, Integer.MAX_VALUE)
                .fieldOf("max_damage").xmap(State::new, State::maxDamage).codec();
        public static final StreamCodec<ByteBuf, State> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(State::new, State::maxDamage);

        public boolean matches(ItemStack stack) {
            return stack.getOrDefault(DataComponents.MAX_DAMAGE, 0) == maxDamage
                    && stack.getOrDefault(DataComponents.MAX_STACK_SIZE, 1) == 1;
        }
    }

    private PufferfishDurability() {}

    /// Advances at each quarter of spent durability, with the final stage lasting until breakage.
    public static int deflationStage(ItemStack stack) {
        if (!PufferfishItems.isPufferfish(stack)) return 0;
        int maximum = stack.getMaxDamage();
        return maximum <= 0 ? 0 : (int) Math.min(3, (long) stack.getDamageValue() * 4 / maximum);
    }

    public static boolean canWearAsGlove(ItemStack stack) {
        return PufferfishItems.isPufferfish(stack)
                && (stack.getMaxDamage() <= 0 || (long) stack.getDamageValue() * 2 <= stack.getMaxDamage());
    }

    public static int maximum(ItemStack stack) {
        var variant = PufferfishVariant.of(stack);
        return variant == null ? 64 : variant.material().fishDurability();
    }

    public static void activate(ItemStack stack) {
        if (!PufferfishItems.isWeapon(stack) || stack.getCount() != 1) return;
        var state = stack.get(PufferfishPlease.MANAGED_DURABILITY.get());
        if (state != null && !state.matches(stack)) stack.remove(PufferfishPlease.MANAGED_DURABILITY.get());
        if (stack.has(DataComponents.MAX_DAMAGE)) return;
        stack.set(PufferfishPlease.MANAGED_DURABILITY.get(), new State(maximum(stack)));
        stack.set(DataComponents.MAX_STACK_SIZE, 1);
        stack.set(DataComponents.MAX_DAMAGE, maximum(stack));
        if (!stack.has(DataComponents.DAMAGE)) stack.set(DataComponents.DAMAGE, 0);
    }

    /// Preserves activation and the fraction of durability spent when changing fish material or crafting a flail.
    public static ItemStack transfer(ItemStack source, ItemStack result) {
        if (result.isEmpty()) return result;
        var state = source.get(PufferfishPlease.MANAGED_DURABILITY.get());
        result.remove(PufferfishPlease.MANAGED_DURABILITY.get());
        if (state == null || !state.matches(source)) return result;
        int damage = source.getOrDefault(DataComponents.DAMAGE, 0);
        result.remove(DataComponents.MAX_DAMAGE);
        result.remove(DataComponents.DAMAGE);
        activate(result);
        result.setDamageValue(Math.min(maximum(result) - 1, (int) Math.ceil((double) damage * maximum(result) / state.maxDamage())));
        return result;
    }

    public static void damage(ItemStack stack, ServerLevel level, LivingEntity owner, EquipmentSlot slot) {
        if (!PufferfishItems.isWeapon(stack) || stack.getCount() != 1 || owner != null && owner.hasInfiniteMaterials()) return;
        activate(stack);
        stack.hurtAndBreak(1, level, owner, item -> {
            if (owner != null && slot != null) owner.onEquippedItemBroken(item, slot);
        });
    }

    public static void beforeAttack(AttackEntityEvent event) {
        var player = event.getEntity();
        var stack = player.getMainHandItem();
        if (player.level().isClientSide || player.hasInfiniteMaterials() || !PufferfishItems.isPufferfish(stack)) return;
        if (stack.getCount() > 1) {
            var remainder = stack.copyWithCount(stack.getCount() - 1);
            stack.setCount(1);
            activate(stack);
            player.getInventory().add(remainder);
            if (!remainder.isEmpty()) {
                var drop = player.drop(remainder, false);
                if (drop != null) {
                    drop.setPos(player.getX(), player.getY() + 0.1, player.getZ());
                    drop.setDeltaMovement(0, 0.1, 0);
                }
            }
        } else activate(stack);
    }

    public static void afterDamage(LivingDamageEvent.Post event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)
                || event.getNewDamage() + event.getReduction(DamageContainer.Reduction.ABSORPTION) <= 0) return;
        var source = event.getSource();
        if (source.is(DamageTypes.PLAYER_ATTACK) && source.getDirectEntity() instanceof Player player) {
            damage(player.getMainHandItem(), level, player, EquipmentSlot.MAINHAND);
        }
        var wearer = event.getEntity();
        if (source.getEntity() instanceof LivingEntity
                && !source.is(DamageTypes.THORNS) && PufferfishHeadgear.isWearing(wearer)) {
            damage(wearer.getItemBySlot(EquipmentSlot.HEAD), level, wearer, EquipmentSlot.HEAD);
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
        if (first.getItem() != second.getItem()) return false;
        if (ItemStack.isSameItemSameComponents(first, second)) return true;
        var a = first.copy();
        var b = second.copy();
        var firstCultivation = a.get(PufferfishPlease.CULTIVATION.get());
        var secondCultivation = b.get(PufferfishPlease.CULTIVATION.get());
        if (firstCultivation != null && secondCultivation != null && firstCultivation.talent() == secondCultivation.talent()) {
            a.remove(PufferfishPlease.CULTIVATION.get());
            b.remove(PufferfishPlease.CULTIVATION.get());
        }
        for (var component : java.util.List.of(DataComponents.DAMAGE, DataComponents.MAX_DAMAGE, DataComponents.MAX_STACK_SIZE,
                PufferfishPlease.MANAGED_DURABILITY.get())) {
            a.remove(component);
            b.remove(component);
        }
        return ItemStack.isSameItemSameComponents(a, b);
    }
}
