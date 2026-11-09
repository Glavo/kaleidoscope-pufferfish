// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.FallingBlockRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.ItemStack;
import org.glavo.kaleidoscope.pufferfish.HeavyPufferfishLampBlock;
import org.glavo.kaleidoscope.pufferfish.PufferfishItems;
import org.glavo.kaleidoscope.pufferfish.client.PlacedPufferfishRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FallingBlockRenderer.class)
public abstract class FallingPufferfishLampRendererMixin {
    @Unique private PlacedPufferfishRenderer kaleidoscope$lampRenderer;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void kaleidoscope$init(EntityRendererProvider.Context context, CallbackInfo ci) {
        kaleidoscope$lampRenderer = new PlacedPufferfishRenderer(context.getModelSet());
    }

    @Inject(method = "render(Lnet/minecraft/world/entity/item/FallingBlockEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"))
    private void kaleidoscope$render(FallingBlockEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo ci) {
        if (!(entity.getBlockState().getBlock() instanceof HeavyPufferfishLampBlock)) return;
        pose.pushPose();
        pose.translate(-0.5, 0, -0.5);
        kaleidoscope$lampRenderer.render(new ItemStack(PufferfishItems.HEAVY.get()), entity.getBlockState(), pose, buffers,
                LightTexture.pack(15, LightTexture.sky(light)), OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}
