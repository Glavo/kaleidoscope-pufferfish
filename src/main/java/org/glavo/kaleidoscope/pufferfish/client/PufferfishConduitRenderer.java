// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PufferfishBigModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.ConduitRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.glavo.kaleidoscope.pufferfish.PufferfishBlocks;
import org.glavo.kaleidoscope.pufferfish.PufferfishConduitBlockEntity;

public final class PufferfishConduitRenderer implements BlockEntityRenderer<PufferfishConduitBlockEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/fish/pufferfish.png");
    private final PufferfishBigModel<Entity> fish;
    private final ModelPart wind;

    public PufferfishConduitRenderer(BlockEntityRendererProvider.Context context) {
        fish = new PufferfishBigModel<>(context.bakeLayer(ModelLayers.PUFFERFISH_BIG));
        wind = context.bakeLayer(ModelLayers.CONDUIT_WIND);
    }

    @Override
    public void render(PufferfishConduitBlockEntity entity, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        float age = entity.tickCount + partialTick;
        boolean active = entity.isActive();
        pose.pushPose();
        pose.translate(0.5, active ? 0.5 + Mth.sin(age * 0.1F) * 0.1F : 0.5, 0.5);
        if (active) pose.mulPose(Axis.YP.rotation(entity.getActiveRotation(partialTick)));
        float size = active ? 1 : 0.75F;
        pose.scale(size, -size, -size);
        pose.translate(0, -18.0F / 16, 0);
        fish.setupAnim(null, 0, 0, age, 0, 0);
        fish.renderToBuffer(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, overlay);
        pose.popPose();
        if (active) {
            int axis = entity.tickCount / 66 % 3;
            pose.pushPose();
            pose.translate(0.5, 0.5, 0.5);
            if (axis == 1) pose.mulPose(Axis.XP.rotationDegrees(90));
            else if (axis == 2) pose.mulPose(Axis.ZP.rotationDegrees(90));
            var material = axis == 1 ? ConduitRenderer.VERTICAL_WIND_TEXTURE : ConduitRenderer.WIND_TEXTURE;
            var buffer = material.buffer(buffers, RenderType::entityCutoutNoCull);
            wind.render(pose, buffer, light, overlay);
            pose.scale(0.875F, 0.875F, 0.875F);
            pose.mulPose(Axis.XP.rotationDegrees(180));
            pose.mulPose(Axis.ZP.rotationDegrees(180));
            wind.render(pose, buffer, light, overlay);
            pose.popPose();
        }
    }

    @Override
    public AABB getRenderBoundingBox(PufferfishConduitBlockEntity entity) {
        return new AABB(entity.getBlockPos()).inflate(0, 0.25, 0);
    }

    public static IClientItemExtensions itemExtensions() {
        return new IClientItemExtensions() {
            private final BlockEntityWithoutLevelRenderer renderer = new ItemRenderer();
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() { return renderer; }
        };
    }

    private static final class ItemRenderer extends BlockEntityWithoutLevelRenderer {
        private final PufferfishConduitBlockEntity entity = new PufferfishConduitBlockEntity(BlockPos.ZERO, PufferfishBlocks.CONDUIT.get().defaultBlockState());

        private ItemRenderer() {
            super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        }

        @Override
        public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            var renderer = Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(entity);
            if (renderer != null) renderer.render(entity, 0, pose, buffers, light, overlay);
        }
    }
}
