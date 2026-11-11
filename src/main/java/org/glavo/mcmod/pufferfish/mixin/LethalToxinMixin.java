// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.mixin;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.glavo.mcmod.pufferfish.PufferfishEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LethalToxinMixin {
    /// Converts accepted poison before it enters the active effect map; toxin still passes its own immunity checks.
    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/LivingEntity;activeEffects:Ljava/util/Map;", ordinal = 0),
            cancellable = true)
    private void pufferfishPlease$mergePoison(MobEffectInstance effect, Entity source, CallbackInfoReturnable<Boolean> callback) {
        var target = (LivingEntity) (Object) this;
        if (!target.level().isClientSide && effect.is(MobEffects.POISON) && target.hasEffect(PufferfishEffects.LETHAL_TOXIN)) {
            callback.setReturnValue(target.addEffect(PufferfishEffects.mergeToxin(target, effect), source));
        }
    }
}
