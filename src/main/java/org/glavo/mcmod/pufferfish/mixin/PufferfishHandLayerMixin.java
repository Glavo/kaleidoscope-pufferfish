// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.mixin;

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
import org.glavo.mcmod.pufferfish.PufferfishDurability;
import org.glavo.mcmod.pufferfish.client.PufferfishGloveRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandLayer.class)
public abstract class PufferfishHandLayerMixin<T extends LivingEntity, M extends EntityModel<T> & ArmedModel> extends RenderLayer<T, M> {
    @Unique
    private PufferfishGloveRenderer pufferfishPlease$gloveRenderer;

    protected PufferfishHandLayerMixin(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void pufferfishPlease$renderGlove(LivingEntity entity, ItemStack stack, ItemDisplayContext context, HumanoidArm arm,
            PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo callback) {
        if (entity instanceof Player player && org.glavo.mcmod.pufferfish.client.PufferfishRowingAnimation.isRowing(player)) {
            callback.cancel();
            return;
        }
        if (entity instanceof Player && PufferfishDurability.canWearAsGlove(stack)) {
            if (pufferfishPlease$gloveRenderer == null) pufferfishPlease$gloveRenderer = new PufferfishGloveRenderer();
            pufferfishPlease$gloveRenderer.renderAttached(getParentModel(), entity, stack, arm, pose, buffers, light, entity.tickCount);
            callback.cancel();
        }
    }
}
