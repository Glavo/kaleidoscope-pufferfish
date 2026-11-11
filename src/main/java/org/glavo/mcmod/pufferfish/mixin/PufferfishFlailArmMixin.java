// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.glavo.mcmod.pufferfish.client.PufferfishFlailArmAnimation;
import org.glavo.mcmod.pufferfish.client.PufferfishFlailTether;
import org.glavo.mcmod.pufferfish.client.PufferfishRowingAnimation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class PufferfishFlailArmMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void pufferfishPlease$flailArm(LivingEntity entity, float limbSwing, float limbAmount, float age,
            float headYaw, float headPitch, CallbackInfo callback) {
        if (entity instanceof Player player) {
            PufferfishRowingAnimation.poseLegs((HumanoidModel<?>) (Object) this, player);
            float partial = PufferfishFlailTether.partialTick(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
            if (PufferfishRowingAnimation.poseArms((HumanoidModel<?>) (Object) this, player, partial,
                    Mth.rotLerp(partial, player.yBodyRotO, player.yBodyRot))) return;
            PufferfishFlailArmAnimation.poseArm((HumanoidModel<?>) (Object) this, player, partial,
                    Mth.rotLerp(partial, player.yBodyRotO, player.yBodyRot));
        }
    }
}
