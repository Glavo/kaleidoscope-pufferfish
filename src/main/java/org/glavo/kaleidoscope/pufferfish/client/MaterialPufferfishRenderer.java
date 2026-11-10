// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.PufferfishRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Pufferfish;
import org.glavo.kaleidoscope.pufferfish.PufferfishItems;
import org.glavo.kaleidoscope.pufferfish.entity.MaterialPufferfish;

public final class MaterialPufferfishRenderer extends PufferfishRenderer {
    public MaterialPufferfishRenderer(EntityRendererProvider.Context context) {
        super(context);
        addLayer(new RenderLayer<Pufferfish, EntityModel<Pufferfish>>(this) {
            @Override
            public void render(PoseStack pose, MultiBufferSource buffers, int light, Pufferfish entity, float limbSwing,
                    float limbSwingAmount, float partialTick, float age, float yaw, float pitch) {
                if (!PufferfishItems.isRadiant(((MaterialPufferfish) entity).fish()) || entity.isInvisible()) return;
                int puff = entity.getPuffState();
                pose.pushPose();
                pose.translate(0, (22.0 - puff * 2) / 16, 0);
                float scale = puff == 0 ? 0.5F : puff == 1 ? 0.7F : 1;
                pose.scale(scale, scale, scale);
                RadiantPufferfishVisuals.renderCentered(pose, buffers, OverlayTexture.NO_OVERLAY);
                if (((MaterialPufferfish) entity).attackPhase() == MaterialPufferfish.PULSE) {
                    pose.scale(1.25F, 1.25F, 1.25F);
                    pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(45));
                    RadiantPufferfishVisuals.renderCentered(pose, buffers, OverlayTexture.NO_OVERLAY);
                }
                pose.popPose();
            }
        });
    }

    @Override
    public ResourceLocation getTextureLocation(Pufferfish entity) {
        var stack = ((MaterialPufferfish) entity).fish();
        if (PufferfishItems.isNetherite(stack)) return PufferfishTextures.netheriteEntity();
        if (PufferfishItems.isHeavy(stack)) return PufferfishTextures.heavyEntity();
        return PufferfishTextures.materialEntity(PufferfishItems.materialName(stack));
    }

    @Override
    protected int getBlockLightLevel(Pufferfish entity, BlockPos pos) {
        return PufferfishItems.isRadiant(((MaterialPufferfish) entity).fish()) ? 15 : super.getBlockLightLevel(entity, pos);
    }
}
