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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.glavo.kaleidoscope.pufferfish.entity.PufferfishFlailHead;

/// Renders a flying flail head with a chain extending to its owner's casting hand.
public final class PufferfishFlailHeadRenderer extends EntityRenderer<PufferfishFlailHead> {
    /// Geometry shared by this renderer's heads and chains.
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
        var position = head.getPosition(partialTick);
        Vec3 hand = null;
        Vec3 correction = Vec3.ZERO;
        if (head.getOwner() instanceof Player player && !PufferfishFlailTether.usesHandPass(head)) {
            hand = handPosition(player, head.getHand(), partialTick);
            var origin = player.getEyePosition(partialTick).add(0, -0.3, 0);
            double blend = Math.max(0, 1 - position.distanceTo(origin) / 1.25);
            correction = hand.subtract(origin).scale(blend * blend);
        }
        // Blend the eye-based projectile path into the visible grip near launch and recovery.
        pose.pushPose();
        pose.translate(correction.x, correction.y, correction.z);
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(Mth.rotLerp(partialTick, head.yRotO, head.getYRot())));
        pose.mulPose(Axis.XP.rotationDegrees(-Mth.lerp(partialTick, head.xRotO, head.getXRot())));
        model.renderHead(head.getItem(), pose, buffers, light, OverlayTexture.NO_OVERLAY, head.tickCount + partialTick);
        pose.popPose();
        if (hand != null) {
            var offset = hand.subtract(position.add(correction));
            double distance = offset.length();
            var attachment = offset.normalize().scale(Math.min(0.20, distance * 0.5));
            // The tether is taut during launch and retrieval, with slack only as the head reaches the grip.
            double sag = head.isReturning() && distance < 1
                    ? 0.08 * distance * (1 - distance) : 0;
            pose.pushPose();
            pose.translate(attachment.x, attachment.y, attachment.z);
            model.renderChain(head.getItem(), offset.subtract(attachment), sag, pose, buffers, light);
            pose.popPose();
        }
        pose.popPose();
        super.render(head, yaw, partialTick, pose, buffers, light);
    }

    /// Returns the third-person chain attachment, accounting for handedness and posture.
    private Vec3 handPosition(Player player, InteractionHand hand, float partialTick) {
        var arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        float yaw = Mth.lerp(partialTick, player.yBodyRotO, player.yBodyRot) * Mth.DEG_TO_RAD;
        double scale = player.getScale();
        return player.getEyePosition(partialTick).add(
                (-Mth.cos(yaw) * side * 0.35 - Mth.sin(yaw) * 0.8) * scale,
                (-0.45 - (player.isCrouching() ? 0.1875 : 0)) * scale,
                (-Mth.sin(yaw) * side * 0.35 + Mth.cos(yaw) * 0.8) * scale);
    }

    @Override
    public ResourceLocation getTextureLocation(PufferfishFlailHead head) {
        return ResourceLocation.withDefaultNamespace("textures/entity/fish/pufferfish.png");
    }
}
