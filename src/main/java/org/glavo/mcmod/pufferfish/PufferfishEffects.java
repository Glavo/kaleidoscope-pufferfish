// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class PufferfishEffects {
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, PufferfishPlease.MODID);
    public static final DeferredHolder<MobEffect, MobEffect> LETHAL_TOXIN = EFFECTS.register("lethal_toxin", LethalToxinEffect::new);

    private PufferfishEffects() {}

    public static void register(IEventBus bus) {
        EFFECTS.register(bus);
        NeoForge.EVENT_BUS.addListener(PufferfishEffects::immunity);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, PufferfishEffects::mergeAddedToxin);
    }

    public static boolean isPoison(MobEffectInstance effect) {
        return effect.is(MobEffects.POISON) || effect.is(LETHAL_TOXIN);
    }

    /// Strength and duration are merged independently; infinite duration wins over every finite duration.
    public static MobEffectInstance mergeToxin(LivingEntity target, MobEffectInstance incoming) {
        int amplifier = incoming.getAmplifier();
        int duration = incoming.getDuration();
        for (var type : java.util.List.of(MobEffects.POISON, LETHAL_TOXIN)) {
            var current = target.getEffect(type);
            if (current == null) continue;
            amplifier = Math.max(amplifier, current.getAmplifier());
            duration = duration == MobEffectInstance.INFINITE_DURATION || current.isInfiniteDuration()
                    ? MobEffectInstance.INFINITE_DURATION : Math.max(duration, current.getDuration());
        }
        return new MobEffectInstance(LETHAL_TOXIN, duration, amplifier, incoming.isAmbient(), incoming.isVisible(), incoming.showIcon());
    }

    private static void immunity(MobEffectEvent.Applicable event) {
        if (event.getResult() == MobEffectEvent.Applicable.Result.DEFAULT && event.getEffectInstance().is(LETHAL_TOXIN)
                && event.getEntity().getType().is(EntityTypeTags.IGNORES_POISON_AND_REGEN)) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    private static void mergeAddedToxin(MobEffectEvent.Added event) {
        var target = event.getEntity();
        var incoming = event.getEffectInstance();
        if (target.level().isClientSide || !incoming.is(LETHAL_TOXIN)) return;
        incoming.update(mergeToxin(target, incoming));
        target.removeEffect(MobEffects.POISON);
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
            // Offset toxin pulses from the once-per-second contact damage cycle.
            return duration % interval == interval / 2;
        }
    }
}
