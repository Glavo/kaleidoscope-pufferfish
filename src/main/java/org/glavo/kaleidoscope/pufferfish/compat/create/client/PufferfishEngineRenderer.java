// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.compat.create.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PufferfishBigModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.pipeline.VertexConsumerWrapper;
import org.glavo.kaleidoscope.pufferfish.PufferfishColors;
import org.glavo.kaleidoscope.pufferfish.PufferfishItems;
import org.glavo.kaleidoscope.pufferfish.client.PufferfishTextures;
import org.glavo.kaleidoscope.pufferfish.compat.create.PufferfishEngineBlock;
import org.glavo.kaleidoscope.pufferfish.compat.create.PufferfishEngineBlockEntity;

public final class PufferfishEngineRenderer implements BlockEntityRenderer<PufferfishEngineBlockEntity> {
    private static final ResourceLocation ORIGINAL = ResourceLocation.withDefaultNamespace("textures/entity/fish/pufferfish.png");
    private final PufferfishBigModel<LivingEntity> fishModel;

    public PufferfishEngineRenderer(BlockEntityRendererProvider.Context context) {
        fishModel = new PufferfishBigModel<>(context.bakeLayer(ModelLayers.PUFFERFISH_BIG));
    }

    @Override
    public void render(PufferfishEngineBlockEntity engine, float partialTick, PoseStack pose, MultiBufferSource buffers,
            int light, int overlay) {
        float angle = engine.getAnimationAngle(partialTick);
        var facing = engine.getBlockState().getValue(PufferfishEngineBlock.HORIZONTAL_FACING);
        float pulse = engine.isRunning() ? (1 - Mth.cos(angle * Mth.DEG_TO_RAD)) * 0.5F : 0;
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(180 - facing.toYRot()));
        pose.translate(-0.5, -0.5, -0.5);

        pose.pushPose();
        pose.translate(0.5, 0.5, 0.8125);
        pose.mulPose(Axis.ZP.rotationDegrees(angle * facing.getOpposite().getAxisDirection().getStep()));
        pose.translate(-0.5, -0.5, -0.8125);
        renderPart("shaft", pose, buffers, light, overlay);
        renderPart("flywheel", pose, buffers, light, overlay);
        pose.popPose();

        pose.pushPose();
        pose.translate(0, 0, pulse * 0.125);
        renderPart("piston", pose, buffers, light, overlay);
        pose.popPose();

        pose.pushPose();
        pose.translate(0.5, 0.25, 0.1875);
        pose.mulPose(Axis.XP.rotationDegrees(-75 * (1 - pulse)));
        pose.translate(-0.5, -0.25, -0.1875);
        renderPart("flap", pose, buffers, light, overlay);
        pose.popPose();

        var fish = engine.getFish();
        if (!fish.isEmpty()) {
            var color = PufferfishColors.get(fish);
            var texture = PufferfishItems.isHeavy(fish) ? PufferfishTextures.heavyEntity()
                    : PufferfishItems.isNetherite(fish) ? PufferfishTextures.netheriteEntity()
                    : color == null ? ORIGINAL : PufferfishTextures.entity(color);
            float scale = 0.65F + pulse * 0.4F;
            pose.pushPose();
            pose.translate(0.5, 0.5, 0.34375);
            pose.scale(-scale, -scale, scale);
            pose.translate(0, -18.0F / 16, 0);
            fishModel.setupAnim(null, 0, 0, angle / 6, 0, 0);
            fishModel.renderToBuffer(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(texture)), light, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
        if (engine.hasWater()) {
            renderPart("water", pose, buffers, light, overlay);
        }
        pose.popPose();
    }

    private static void renderPart(String part, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var minecraft = Minecraft.getInstance();
        var model = minecraft.getModelManager().getModel(CreateClientIntegration.model(part));
        boolean water = part.equals("water");
        VertexConsumer vertices = buffers.getBuffer(water ? RenderType.translucent() : RenderType.cutout());
        if (water) {
            vertices = new VertexConsumerWrapper(vertices) {
                @Override
                public VertexConsumer setColor(int red, int green, int blue, int alpha) {
                    return super.setColor(red, green, blue, Math.round(alpha * 0.22F));
                }
            };
        }
        minecraft.getBlockRenderer().getModelRenderer().renderModel(pose.last(), vertices, null, model,
                water ? 0.25F : 1, water ? 0.5F : 1, 1, light, overlay,
                ModelData.EMPTY,
                water ? RenderType.translucent() : RenderType.cutout());
    }
}
