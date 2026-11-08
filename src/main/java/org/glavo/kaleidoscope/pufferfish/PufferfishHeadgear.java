// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.EnchantmentTarget;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public final class PufferfishHeadgear {
    private PufferfishHeadgear() {}

    public static boolean isWearing(LivingEntity entity) {
        return entity instanceof Player player && !player.isSpectator()
                && PufferfishItems.isPufferfish(entity.getItemBySlot(EquipmentSlot.HEAD));
    }

    public static void breathe(LivingBreatheEvent event) {
        if (isWearing(event.getEntity())) {
            event.setCanBreathe(true);
            event.setRefillAirAmount(event.getEntity().getMaxAirSupply());
        }
    }

    public static void protect(LivingIncomingDamageEvent event) {
        if (isWearing(event.getEntity()) && event.getSource().getDirectEntity() instanceof Pufferfish) {
            event.setCanceled(true);
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
        boolean netherite = PufferfishItems.isNetherite(wearer.getItemBySlot(EquipmentSlot.HEAD));
        attacker.addEffect(new MobEffectInstance(MobEffects.POISON, netherite ? 120 : 60, netherite ? 1 : 0), wearer);
        var thorns = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.THORNS);
        thorns.value().doPostAttack(level, netherite ? 3 : 1,
                new EnchantedItemInUse(wearer.getItemBySlot(EquipmentSlot.HEAD), EquipmentSlot.HEAD, wearer),
                EnchantmentTarget.VICTIM, wearer, source);
    }
}
