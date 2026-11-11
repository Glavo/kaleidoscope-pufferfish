// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;
import org.glavo.mcmod.pufferfish.client.PufferfishHeadRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CustomHeadLayer.class)
public abstract class PufferfishHeadLayerMixin<T extends LivingEntity, M extends EntityModel<T> & HeadedModel> extends RenderLayer<T, M> {
    @Unique
    private PufferfishHeadRenderer pufferfishPlease$headRenderer;

    protected PufferfishHeadLayerMixin(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At("HEAD"), cancellable = true)
    private void pufferfishPlease$renderPufferfish(PoseStack pose, MultiBufferSource buffers, int light, T entity,
            float limbSwing, float limbSwingAmount, float partialTick, float age, float yaw, float pitch, CallbackInfo callback) {
        if (PufferfishHeadRenderer.shouldRender(entity)) {
            if (pufferfishPlease$headRenderer == null) {
                pufferfishPlease$headRenderer = new PufferfishHeadRenderer();
            }
            pufferfishPlease$headRenderer.render(getParentModel().getHead(), pose, buffers, light, entity, age);
            callback.cancel();
        }
    }
}
