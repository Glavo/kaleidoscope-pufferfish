// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.minecraft.tags.FluidTags;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

public final class PufferfishHeadgear {
    private PufferfishHeadgear() {}

    public static boolean isWearing(LivingEntity entity) {
        return ((entity instanceof Player player && !player.isSpectator())
                || entity.getType() == EntityType.ZOMBIE || entity instanceof IronGolem)
                && PufferfishItems.isPufferfish(entity.getItemBySlot(EquipmentSlot.HEAD));
    }

    public static void breathe(PlayerTickEvent.Post event) {
        breathe(event.getEntity());
    }

    public static void breatheMob(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof Zombie || event.getEntity() instanceof IronGolem) {
            breathe((LivingEntity) event.getEntity());
        }
    }

    public static void interact(PlayerInteractEvent.EntityInteract event) {
        var player = event.getEntity();
        if (!(event.getTarget() instanceof IronGolem golem) || !golem.isAlive()
                || player.isSpectator() || !player.mayBuild()) return;
        var held = event.getItemStack();
        var equipped = golem.getItemBySlot(EquipmentSlot.HEAD);
        boolean equip = player.isShiftKeyDown() && equipped.isEmpty() && PufferfishItems.isPufferfish(held);
        boolean remove = held.isEmpty() && player.isShiftKeyDown() && PufferfishItems.isPufferfish(equipped)
                && (player.hasInfiniteMaterials() || !EnchantmentHelper.has(equipped, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE));
        if (!equip && !remove) return;
        if (!player.level().isClientSide()) {
            if (equip) {
                golem.setItemSlot(EquipmentSlot.HEAD, held.copyWithCount(1));
                golem.setDropChance(EquipmentSlot.HEAD, 2.0F);
                held.consume(1, player);
            } else {
                golem.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                player.getInventory().add(equipped);
                if (!equipped.isEmpty()) player.drop(equipped, false);
            }
            golem.playSound(SoundEvents.ARMOR_EQUIP_GENERIC.value(), 1, 1);
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(player.level().isClientSide()));
    }

    private static void breathe(LivingEntity wearer) {
        if (!wearer.level().isClientSide() && isWearing(wearer) && wearer.getItemBySlot(EquipmentSlot.HEAD).is(PufferfishItems.RADIANT.get())) {
            wearer.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, false, false, true));
        }
        if (!wearer.level().isClientSide() && isWearing(wearer) && !wearer.isEyeInFluid(FluidTags.WATER)) {
            wearer.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 200, 0, false, false, true));
        }
    }

    public static void retaliate(LivingDamageEvent.Post event) {
        var wearer = event.getEntity();
        var source = event.getSource();
        if (!(wearer.level() instanceof ServerLevel level) || !isWearing(wearer)
                || !PufferfishRetaliation.canRetaliate(event)) {
            return;
        }
        int strength = PufferfishRetaliation.thornsLevel(wearer.getItemBySlot(EquipmentSlot.HEAD));
        if (!wearer.hasInfiniteMaterials()) PufferfishDurability.activate(wearer.getItemBySlot(EquipmentSlot.HEAD));
        PufferfishRetaliation.thorns(level, strength,
                new EnchantedItemInUse(wearer.getItemBySlot(EquipmentSlot.HEAD), EquipmentSlot.HEAD, wearer), wearer, source);
    }
}
