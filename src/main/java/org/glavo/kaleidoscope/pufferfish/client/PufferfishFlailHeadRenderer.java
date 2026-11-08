// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
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
    public void render(PufferfishFlailHead head, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        model.renderHead(head.getItem(), pose, buffers, light, OverlayTexture.NO_OVERLAY, head.tickCount + partialTick);
        if (head.getOwner() instanceof Player player) {
            model.renderChain(handPosition(player, head.getHand(), partialTick).subtract(head.getPosition(partialTick)), pose, buffers, light);
        }
        super.render(head, yaw, partialTick, pose, buffers, light);
    }

    /// Returns the world-space chain attachment, accounting for handedness and camera mode.
    private Vec3 handPosition(Player player, InteractionHand hand, float partialTick) {
        var arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        if (player == Minecraft.getInstance().player && entityRenderDispatcher.options.getCameraType().isFirstPerson()) {
            var offset = entityRenderDispatcher.camera.getNearPlane().getPointOnPlane(side * 0.525F, -0.1F)
                    .scale(960.0 / entityRenderDispatcher.options.fov().get());
            return player.getEyePosition(partialTick).add(offset);
        }
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
