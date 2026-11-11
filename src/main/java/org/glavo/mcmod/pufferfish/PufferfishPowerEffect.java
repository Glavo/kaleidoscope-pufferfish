// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.glavo.mcmod.pufferfish.entity.MaterialPufferfish;

public final class PufferfishPowerEffect extends MobEffect {
    public PufferfishPowerEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xE8C65A);
    }

    public static boolean empowersPlayer(LivingEntity entity) {
        return entity instanceof Player && entity.hasEffect(PufferfishEffects.PUFFERFISH_POWER);
    }

    private static boolean empowersFish(LivingEntity entity) {
        return entity instanceof Pufferfish && entity.hasEffect(PufferfishEffects.PUFFERFISH_POWER);
    }

    public static void breathe(LivingBreatheEvent event) {
        if (empowersPlayer(event.getEntity()) && !event.canBreathe()) {
            event.setCanBreathe(true);
            event.setRefillAirAmount(0);
        }
    }

    public static void digSpeed(PlayerEvent.BreakSpeed event) {
        if (empowersPlayer(event.getEntity()) && !MobEffectUtil.hasDigSpeed(event.getEntity())) {
            event.setNewSpeed(event.getNewSpeed() * 1.2F);
        }
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) { return true; }

    @Override
    public boolean applyEffectTick(LivingEntity target, int amplifier) {
        // Refreshing the aura must not reset or accelerate regeneration pulses.
        if (target instanceof Pufferfish && target.tickCount % 50 == 0 && !target.hasEffect(MobEffects.REGENERATION)
                && target.getHealth() < target.getMaxHealth()) {
            target.heal(1);
        }
        return true;
    }

    public static void strength(LivingIncomingDamageEvent event) {
        if (event.getSource().is(DamageTypes.MOB_ATTACK)
                && event.getSource().getDirectEntity() instanceof Pufferfish fish && empowersFish(fish)) {
            // Material fish already include vanilla Strength in their attack attribute; vanilla stings do not.
            if (!(fish instanceof MaterialPufferfish) || !fish.hasEffect(MobEffects.DAMAGE_BOOST)) {
                event.setAmount(event.getAmount() + 3);
            }
        }
    }

    public static void resistance(LivingDamageEvent.Pre event) {
        if (empowersFish(event.getEntity()) && !event.getEntity().hasEffect(MobEffects.DAMAGE_RESISTANCE)
                && !event.getSource().is(DamageTypeTags.BYPASSES_EFFECTS)
                && !event.getSource().is(DamageTypeTags.BYPASSES_RESISTANCE)) {
            event.setNewDamage(event.getNewDamage() * 0.8F);
        }
    }
}
