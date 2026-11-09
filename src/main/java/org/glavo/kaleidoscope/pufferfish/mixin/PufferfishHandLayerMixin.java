// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.glavo.kaleidoscope.pufferfish.PufferfishItems;
import org.glavo.kaleidoscope.pufferfish.client.PufferfishGloveRenderer;
import org.glavo.kaleidoscope.pufferfish.client.PufferfishFlailItemRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandLayer.class)
public abstract class PufferfishHandLayerMixin<T extends LivingEntity, M extends EntityModel<T> & ArmedModel> extends RenderLayer<T, M> {
    @Unique
    private PufferfishGloveRenderer kaleidoscope$gloveRenderer;

    protected PufferfishHandLayerMixin(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @WrapMethod(method = "renderArmWithItem")
    private void kaleidoscope$flailBodyPose(LivingEntity entity, ItemStack stack, ItemDisplayContext context, HumanoidArm arm,
            PoseStack pose, MultiBufferSource buffers, int light, Operation<Void> original) {
        if (!(entity instanceof Player) || !PufferfishItems.isFlail(stack) || !entity.isUsingItem() || entity.getUseItem() != stack) {
            original.call(entity, stack, context, arm, pose, buffers, light);
            return;
        }
        var previous = PufferfishFlailItemRenderer.setBodyPose(new Matrix4f(pose.last().pose()));
        try {
            original.call(entity, stack, context, arm, pose, buffers, light);
        } finally {
            PufferfishFlailItemRenderer.setBodyPose(previous);
        }
    }

    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void kaleidoscope$renderGlove(LivingEntity entity, ItemStack stack, ItemDisplayContext context, HumanoidArm arm,
            PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo callback) {
        if (entity instanceof Player && PufferfishItems.isPufferfish(stack)) {
            if (kaleidoscope$gloveRenderer == null) kaleidoscope$gloveRenderer = new PufferfishGloveRenderer();
            kaleidoscope$gloveRenderer.renderAttached(getParentModel(), entity, stack, arm, pose, buffers, light, entity.tickCount);
            callback.cancel();
        }
    }
}
