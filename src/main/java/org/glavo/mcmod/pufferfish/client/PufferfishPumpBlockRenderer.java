// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import org.glavo.mcmod.pufferfish.PufferfishPumpBlock;
import org.glavo.mcmod.pufferfish.PufferfishPumpBlockEntity;

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
        float age = entity.getLevel() == null ? 0 : entity.getLevel().getGameTime() + partialTick;
        float pressure = entity.pressure();
        if (pressure >= 3) pressure += (float) Math.sin(age * 2.5F) * 0.08F;
        model.render(entity.pump(), stack, entity.compression(partialTick), pressure, pose, buffers, light, overlay);
        if (!stack.isEmpty()) {
            var center = PufferfishPumpModel.fishCenter(stack, pressure);
            pose.translate(center.x, center.y, center.z);
            pose.scale(0.5F, 0.5F, 0.5F);
            fish.renderHead(stack, pose, buffers, light, overlay, age, pressure);
        }
        pose.popPose();
    }
}
