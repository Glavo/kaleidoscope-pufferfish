// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import org.glavo.mcmod.pufferfish.FishProcessingTableBlock;
import org.glavo.mcmod.pufferfish.FishProcessingTableBlockEntity;
import org.glavo.mcmod.pufferfish.PufferfishItems;

public final class FishProcessingTableRenderer implements BlockEntityRenderer<FishProcessingTableBlockEntity> {
    private final net.minecraft.client.renderer.entity.ItemRenderer items;
    private final PufferfishFlailModel fish = new PufferfishFlailModel();

    public FishProcessingTableRenderer(BlockEntityRendererProvider.Context context) { items = context.getItemRenderer(); }

    @Override
    public void render(FishProcessingTableBlockEntity entity, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var stack = entity.item();
        if (stack.isEmpty()) return;
        pose.pushPose();
        boolean wholeFish = PufferfishItems.isPufferfish(stack);
        float height = wholeFish ? 0.76F + 0.14F * DeflatingPufferfishModel.height(org.glavo.mcmod.pufferfish.PufferfishDurability.deflationStage(stack)) : 0.9F;
        pose.translate(0.5, height, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-entity.getBlockState().getValue(FishProcessingTableBlock.FACING).toYRot()));
        float motion = entity.motion(partialTick);
        pose.translate(entity.isBrushingAnimation() ? Math.sin(motion * Math.PI * 2) * 0.025 : 0, -motion * 0.025, 0);
        if (wholeFish) {
            pose.scale(0.7F, 0.7F, 0.7F);
            fish.renderHead(stack, pose, buffers, light, overlay, entity.getLevel().getGameTime() + partialTick, 0);
        } else {
            boolean skin = org.glavo.mcmod.pufferfish.PufferfishSkinBlock.isSkin(stack);
            pose.translate(0, skin ? 0.16 : -0.12, 0);
            if (!skin) pose.mulPose(Axis.XP.rotationDegrees(90));
            pose.scale(0.6F, 0.6F, 0.6F);
            items.renderStatic(stack, ItemDisplayContext.FIXED, light, overlay, pose, buffers, entity.getLevel(), 0);
        }
        pose.popPose();
    }
}
