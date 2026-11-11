// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.glavo.mcmod.pufferfish.PlacedPufferfishBlock;
import org.glavo.mcmod.pufferfish.PlacedPufferfishBlockEntity;
import org.glavo.mcmod.pufferfish.PufferfishColors;
import org.glavo.mcmod.pufferfish.PufferfishItems;

public final class PlacedPufferfishRenderer implements BlockEntityRenderer<PlacedPufferfishBlockEntity> {
    private final DeflatingPufferfishModel<Entity> model;

    public PlacedPufferfishRenderer(BlockEntityRendererProvider.Context context) {
        this(context.getModelSet());
    }

    public PlacedPufferfishRenderer(EntityModelSet models) {
        model = new DeflatingPufferfishModel<>(models);
        model.setupAnim(null, 0, 0, 0, 0, 0);
    }

    @Override
    public void render(PlacedPufferfishBlockEntity blockEntity, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var state = blockEntity.getBlockState();
        light = LightTexture.pack(Math.max(LightTexture.block(light), state.getLightEmission()),
                LightTexture.sky(light));
        render(blockEntity.fish(), state, pose, buffers, light, overlay);
    }

    public void render(ItemStack stack, BlockState state, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (stack.isEmpty()) return;
        var color = PufferfishColors.get(stack);
        var material = PufferfishItems.materialName(stack);
        var texture = PufferfishItems.isHeavy(stack) ? PufferfishTextures.heavyEntity()
                : PufferfishItems.isNetherite(stack) ? PufferfishTextures.netheriteEntity()
                : material != null ? PufferfishTextures.materialEntity(material)
                : color != null ? PufferfishTextures.entity(color)
                : ResourceLocation.withDefaultNamespace("textures/entity/fish/pufferfish.png");
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-state.getValue(PlacedPufferfishBlock.FACING).toYRot()));
        pose.scale(1, -1, -1);
        pose.translate(0, -23.0 / 16, 0);
        pose.translate(0, (1 - DeflatingPufferfishModel.height(org.glavo.mcmod.pufferfish.PufferfishDurability.deflationStage(stack))) / 4, 0);
        model.prepare(stack);
        model.setupAnim(null, 0, 0, 0, 0, 0);
        model.renderToBuffer(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(texture)), light, overlay);
        RadiantPufferfishVisuals.render(stack, pose, buffers, overlay);
        pose.popPose();
    }
}
