// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.mixin;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import org.glavo.mcmod.pufferfish.PufferfishPowerEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LightTexture.class)
public abstract class PufferfishPowerLightMixin {
    @Redirect(method = "updateLightTexture", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;hasEffect(Lnet/minecraft/core/Holder;)Z"))
    private boolean pufferfishPlease$underwaterVision(LocalPlayer player, Holder<MobEffect> effect) {
        return player.hasEffect(effect) || effect.equals(MobEffects.CONDUIT_POWER) && PufferfishPowerEffect.empowersPlayer(player);
    }
}
