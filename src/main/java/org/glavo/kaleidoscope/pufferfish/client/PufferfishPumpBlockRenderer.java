// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import org.glavo.kaleidoscope.pufferfish.PufferfishPumpBlock;
import org.glavo.kaleidoscope.pufferfish.PufferfishPumpBlockEntity;

public final class PufferfishPumpBlockRenderer implements BlockEntityRenderer<PufferfishPumpBlockEntity> {
    private final PufferfishPumpModel model = new PufferfishPumpModel();
    private final PufferfishFlailModel fish = new PufferfishFlailModel();

    public PufferfishPumpBlockRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(PufferfishPumpBlockEntity entity, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-entity.getBlockState().getValue(PufferfishPumpBlock.FACING).toYRot()));
        var stack = entity.fish();
        model.render(entity.pump(), stack, entity.compression(partialTick), pose, buffers, light, overlay);
        if (!stack.isEmpty()) {
            var center = PufferfishPumpModel.fishCenter(stack);
            pose.translate(center.x, center.y, center.z);
            pose.scale(0.5F, 0.5F, 0.5F);
            fish.renderHead(stack, pose, buffers, light, overlay, 0);
        }
        pose.popPose();
    }
}
