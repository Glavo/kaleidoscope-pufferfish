// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.glavo.mcmod.pufferfish.client.PufferfishHeadRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntityRenderer.class)
public abstract class PufferfishHeadVisibilityMixin {
    @WrapOperation(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"))
    private void pufferfishPlease$hideHead(EntityModel<?> model, PoseStack pose, VertexConsumer buffer,
            int light, int overlay, int color, Operation<Void> original, LivingEntity entity) {
        if (!(model instanceof HumanoidModel<?> humanoid) || !PufferfishHeadRenderer.shouldRender(entity)) {
            original.call(model, pose, buffer, light, overlay, color);
            return;
        }
        boolean headVisible = humanoid.head.visible;
        boolean hatVisible = humanoid.hat.visible;
        humanoid.head.visible = humanoid.hat.visible = false;
        try {
            original.call(model, pose, buffer, light, overlay, color);
        } finally {
            humanoid.head.visible = headVisible;
            humanoid.hat.visible = hatVisible;
        }
    }
}
