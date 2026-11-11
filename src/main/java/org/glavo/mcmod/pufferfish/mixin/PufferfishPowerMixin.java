// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.mixin;

import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.LivingEntity;
import org.glavo.mcmod.pufferfish.PufferfishPowerEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobEffectUtil.class)
public abstract class PufferfishPowerMixin {
    @Inject(method = {"hasDigSpeed", "hasWaterBreathing"}, at = @At("RETURN"), cancellable = true)
    private static void pufferfishPlease$power(LivingEntity entity, CallbackInfoReturnable<Boolean> callback) {
        if (!callback.getReturnValue() && PufferfishPowerEffect.empowersPlayer(entity)) callback.setReturnValue(true);
    }
}
