// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.PufferfishBigModel;
import net.minecraft.client.model.PufferfishSmallModel;
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
import org.glavo.mcmod.pufferfish.PufferfishBlocks;
import org.glavo.mcmod.pufferfish.PufferfishConduitBlockEntity;

public final class PufferfishConduitRenderer implements BlockEntityRenderer<PufferfishConduitBlockEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/fish/pufferfish.png");
    /// The inflated model rendered while the conduit is active.
    private final PufferfishBigModel<Entity> activeFish;
    /// The uninflated model rendered while the conduit is inactive, including item rendering.
    private final PufferfishSmallModel<Entity> inactiveFish;
    private final ModelPart wind;

    public PufferfishConduitRenderer(BlockEntityRendererProvider.Context context) {
        activeFish = new PufferfishBigModel<>(context.bakeLayer(ModelLayers.PUFFERFISH_BIG));
        inactiveFish = new PufferfishSmallModel<>(context.bakeLayer(ModelLayers.PUFFERFISH_SMALL));
        wind = context.bakeLayer(ModelLayers.CONDUIT_WIND);
    }

    @Override
    public void render(PufferfishConduitBlockEntity entity, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        float age = entity.tickCount + partialTick;
        boolean active = entity.isActive();
        pose.pushPose();
        pose.translate(0.5, active ? 0.5 + Mth.sin(age * 0.1F) * 0.1F : 0.5, 0.5);
        if (active) pose.mulPose(Axis.YP.rotation(entity.getActiveRotation(partialTick)));
        pose.scale(1, -1, -1);
        pose.translate(0, -(active ? 18.0F : 22.0F) / 16, 0);
        EntityModel<Entity> fish = active ? activeFish : inactiveFish;
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
