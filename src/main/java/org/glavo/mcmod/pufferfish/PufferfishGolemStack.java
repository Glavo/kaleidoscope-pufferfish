// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.GameRules;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/// The head slot holds the active fish; the attachment holds decorations from bottom to top.
public final class PufferfishGolemStack {
    public static final int MAX_SUPPORTED_COUNT = 1024;

    private PufferfishGolemStack() {}

    public static List<ItemStack> decorations(IronGolem golem) {
        return golem.hasData(PufferfishPlease.GOLEM_FISH) ? golem.getData(PufferfishPlease.GOLEM_FISH) : List.of();
    }

    private static void store(IronGolem golem, List<ItemStack> fish) {
        if (fish.isEmpty()) golem.removeData(PufferfishPlease.GOLEM_FISH);
        else golem.setData(PufferfishPlease.GOLEM_FISH, List.copyOf(fish));
    }

    public static void spawn(FinalizeSpawnEvent event) {
        if (!(event.getEntity() instanceof IronGolem golem) || event.isSpawnCancelled()
                || event.getSpawnType() != MobSpawnType.NATURAL && event.getSpawnType() != MobSpawnType.CHUNK_GENERATION
                && event.getSpawnType() != MobSpawnType.STRUCTURE && event.getSpawnType() != MobSpawnType.MOB_SUMMONED
                || golem.isPlayerCreated()
                || !golem.getItemBySlot(EquipmentSlot.HEAD).isEmpty() || !decorations(golem).isEmpty()) return;
        var random = golem.getRandom();
        if (random.nextDouble() >= PufferfishMobConfig.value(PufferfishMobConfig.GOLEM_HEADGEAR_CHANCE)) return;
        int single = PufferfishMobConfig.value(PufferfishMobConfig.GOLEM_SINGLE_WEIGHT);
        int small = PufferfishMobConfig.value(PufferfishMobConfig.GOLEM_SMALL_WEIGHT);
        int total = single + small + PufferfishMobConfig.value(PufferfishMobConfig.GOLEM_LARGE_WEIGHT);
        if (total == 0) return;
        int roll = random.nextInt(total);
        int count = 1;
        if (roll >= single) {
            int first = PufferfishMobConfig.value(roll < single + small
                    ? PufferfishMobConfig.GOLEM_SMALL_MIN_COUNT : PufferfishMobConfig.GOLEM_LARGE_MIN_COUNT);
            int last = PufferfishMobConfig.value(roll < single + small
                    ? PufferfishMobConfig.GOLEM_SMALL_MAX_COUNT : PufferfishMobConfig.GOLEM_LARGE_MAX_COUNT);
            count = Math.min(first, last) + random.nextInt(Math.abs(last - first) + 1);
        }
        count = Math.min(count, PufferfishMobConfig.value(PufferfishMobConfig.GOLEM_MAX_STACK_SIZE));
        var fish = new ArrayList<ItemStack>(count);
        for (int i = 0; i < count; i++) {
            var stack = PufferfishMobs.naturalFish(golem.registryAccess(), random);
            if (!stack.isEmpty()) fish.add(stack);
        }
        if (fish.isEmpty()) return;
        golem.setItemSlot(EquipmentSlot.HEAD, fish.removeFirst());
        golem.setDropChance(EquipmentSlot.HEAD, 2.0F);
        store(golem, fish);
    }

    public static void interact(PlayerInteractEvent.EntityInteract event) {
        var player = event.getEntity();
        if (!(event.getTarget() instanceof IronGolem golem) || !golem.isAlive()
                || !player.isShiftKeyDown() || player.isSpectator() || !player.mayBuild()) return;
        var held = event.getItemStack();
        var equipped = golem.getItemBySlot(EquipmentSlot.HEAD);
        var fish = decorations(golem);
        var top = fish.isEmpty() ? equipped : fish.getLast();
        boolean equip = (equipped.isEmpty() || PufferfishItems.isPufferfish(equipped))
                && fish.size() + (equipped.isEmpty() ? 0 : 1) < PufferfishMobConfig.value(PufferfishMobConfig.GOLEM_MAX_STACK_SIZE)
                && PufferfishItems.isPufferfish(held);
        boolean remove = held.isEmpty() && PufferfishItems.isPufferfish(top)
                && (player.hasInfiniteMaterials() || !EnchantmentHelper.has(top, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE));
        if (!equip && !remove) return;
        if (!player.level().isClientSide()) {
            if (equip) {
                if (equipped.isEmpty() && fish.isEmpty()) {
                    golem.setItemSlot(EquipmentSlot.HEAD, held.copyWithCount(1));
                    golem.setDropChance(EquipmentSlot.HEAD, 2.0F);
                } else {
                    var updated = new ArrayList<>(fish);
                    updated.add(held.copyWithCount(1));
                    store(golem, updated);
                }
                held.consume(1, player);
            } else {
                if (fish.isEmpty()) golem.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                else store(golem, fish.subList(0, fish.size() - 1));
                player.getInventory().add(top);
                if (!top.isEmpty()) player.drop(top, false);
            }
            golem.playSound(SoundEvents.ARMOR_EQUIP_GENERIC.value(), 1, 1);
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(player.level().isClientSide()));
    }

    public static void tick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof IronGolem golem) || golem.level().isClientSide() || !golem.isAlive()) return;
        var fish = decorations(golem);
        if (fish.isEmpty()) return;
        var equipped = golem.getItemBySlot(EquipmentSlot.HEAD);
        if (equipped.isEmpty()) {
            golem.setItemSlot(EquipmentSlot.HEAD, fish.getFirst());
            golem.setDropChance(EquipmentSlot.HEAD, 2.0F);
            store(golem, fish.subList(1, fish.size()));
        } else if (!PufferfishItems.isPufferfish(equipped)) {
            store(golem, List.of());
            fish.forEach(golem::spawnAtLocation);
        }
    }

    public static void drops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof IronGolem golem)) return;
        var fish = decorations(golem);
        if (fish.isEmpty()) return;
        store(golem, List.of());
        if (!golem.level().getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) return;
        for (var stack : fish) {
            if (!EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP)) {
                event.getDrops().add(new ItemEntity(golem.level(), golem.getX(), golem.getY(), golem.getZ(), stack));
            }
        }
    }
}
