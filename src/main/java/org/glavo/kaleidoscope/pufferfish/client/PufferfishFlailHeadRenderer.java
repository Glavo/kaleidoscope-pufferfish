// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.glavo.kaleidoscope.pufferfish.entity.PufferfishFlailHead;

/// Renders the same world-space head during charging, queued release, and flight.
public final class PufferfishFlailHeadRenderer extends EntityRenderer<PufferfishFlailHead> {
    /// Geometry shared by this renderer's heads.
    private final PufferfishFlailModel model = new PufferfishFlailModel();

    /// Creates a renderer for the supplied client rendering context.
    public PufferfishFlailHeadRenderer(EntityRendererProvider.Context context) { super(context); }

    @Override
    public boolean shouldRender(PufferfishFlailHead head, Frustum frustum, double x, double y, double z) {
        return super.shouldRender(head, frustum, x, y, z) || head.getOwner() instanceof Player player
                && frustum.isVisible(head.getBoundingBox().minmax(player.getBoundingBox()));
    }

    @Override
    public void render(PufferfishFlailHead head, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        var correction = head.renderPosition(partialTick).subtract(head.getPosition(partialTick));
        pose.pushPose();
        pose.translate(correction.x, correction.y, correction.z);
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(Mth.rotLerp(partialTick, head.yRotO, head.getYRot())));
        pose.mulPose(Axis.XP.rotationDegrees(-Mth.lerp(partialTick, head.xRotO, head.getXRot())));
        model.renderHead(head.getItem(), pose, buffers, light, OverlayTexture.NO_OVERLAY, head.tickCount + partialTick);
        pose.popPose();
        pose.popPose();
        super.render(head, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(PufferfishFlailHead head) {
        return ResourceLocation.withDefaultNamespace("textures/entity/fish/pufferfish.png");
    }
}
