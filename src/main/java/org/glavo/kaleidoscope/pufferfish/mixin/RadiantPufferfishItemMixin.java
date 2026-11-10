// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.glavo.kaleidoscope.pufferfish.PufferfishItems;
import org.glavo.kaleidoscope.pufferfish.client.RadiantPufferfishVisuals;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public abstract class RadiantPufferfishItemMixin {
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V"))
    private void kaleidoscope$renderRadiantTrails(ItemStack stack, ItemDisplayContext context, boolean leftHand,
            PoseStack pose, MultiBufferSource buffers, int light, int overlay, BakedModel model, CallbackInfo callback) {
        if (!PufferfishItems.isRadiant(stack) || PufferfishItems.isPufferfish(stack) || model.isCustomRenderer()) return;
        pose.pushPose();
        pose.translate(0.5, stack.getItem() instanceof BlockItem ? 0.05 : 0.5, 0.5);
        RadiantPufferfishVisuals.renderCentered(pose, buffers, overlay);
        pose.popPose();
    }
}
