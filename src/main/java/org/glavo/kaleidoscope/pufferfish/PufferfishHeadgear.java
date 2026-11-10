// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.EnchantmentTarget;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.minecraft.tags.FluidTags;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

public final class PufferfishHeadgear {
    private PufferfishHeadgear() {}

    public static boolean isWearing(LivingEntity entity) {
        return ((entity instanceof Player player && !player.isSpectator())
                || entity.getType() == EntityType.ZOMBIE)
                && PufferfishItems.isPufferfish(entity.getItemBySlot(EquipmentSlot.HEAD));
    }

    public static void breathe(PlayerTickEvent.Post event) {
        breathe(event.getEntity());
    }

    public static void breatheZombie(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof Zombie zombie) breathe(zombie);
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
                || source.is(DamageTypes.THORNS) || !(source.getEntity() instanceof LivingEntity attacker)
                || attacker == wearer || event.getNewDamage() + event.getReduction(DamageContainer.Reduction.ABSORPTION) <= 0) {
            return;
        }
        int strength = PufferfishRetaliation.thornsLevel(wearer.getItemBySlot(EquipmentSlot.HEAD));
        if (!wearer.hasInfiniteMaterials()) PufferfishDurability.activate(wearer.getItemBySlot(EquipmentSlot.HEAD));
        var thorns = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.THORNS);
        thorns.value().doPostAttack(level, strength,
                new EnchantedItemInUse(wearer.getItemBySlot(EquipmentSlot.HEAD), EquipmentSlot.HEAD, wearer),
                EnchantmentTarget.VICTIM, wearer, source);
    }
}
