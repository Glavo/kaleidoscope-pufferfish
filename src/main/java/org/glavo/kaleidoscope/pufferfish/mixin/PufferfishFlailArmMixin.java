// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.glavo.kaleidoscope.pufferfish.client.PufferfishFlailArmAnimation;
import org.glavo.kaleidoscope.pufferfish.client.PufferfishFlailTether;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class PufferfishFlailArmMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void kaleidoscope$flailArm(LivingEntity entity, float limbSwing, float limbAmount, float age,
            float headYaw, float headPitch, CallbackInfo callback) {
        if (entity instanceof Player player) {
            float partial = PufferfishFlailTether.partialTick(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
            PufferfishFlailArmAnimation.poseArm((HumanoidModel<?>) (Object) this, player, partial,
                    Mth.rotLerp(partial, player.yBodyRotO, player.yBodyRot));
        }
    }
}
