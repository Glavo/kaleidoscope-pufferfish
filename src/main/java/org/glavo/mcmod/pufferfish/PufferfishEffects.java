// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class PufferfishEffects {
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, PufferfishPlease.MODID);
    public static final DeferredHolder<MobEffect, MobEffect> LETHAL_TOXIN = EFFECTS.register("lethal_toxin", LethalToxinEffect::new);

    private PufferfishEffects() {}

    public static void register(IEventBus bus) {
        EFFECTS.register(bus);
    }

    private static final class LethalToxinEffect extends MobEffect {
        private LethalToxinEffect() {
            super(MobEffectCategory.HARMFUL, 0x356B28);
        }

        @Override
        public boolean applyEffectTick(LivingEntity target, int amplifier) {
            var type = target.damageSources().damageTypes.getHolderOrThrow(NeoForgeMod.POISON_DAMAGE);
            target.hurt(new DamageSource(type), 1.0F);
            return true;
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            int interval = Math.max(1, 20 >> Math.clamp(amplifier, 0, 5));
            return duration % interval == 0;
        }
    }
}
