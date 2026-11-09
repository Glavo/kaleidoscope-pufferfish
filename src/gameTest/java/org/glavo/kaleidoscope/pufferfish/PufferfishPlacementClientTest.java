// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.math.Axis;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.glavo.kaleidoscope.pufferfish.client.PlacedPufferfishRenderer;
import org.joml.Matrix4f;

public final class PufferfishPlacementClientTest {
    public static void verify(Minecraft minecraft) {
        var target = new TextureTarget(900, 600, true, Minecraft.ON_OSX);
        target.setClearColor(0.08F, 0.14F, 0.2F, 1);
        target.clear(Minecraft.ON_OSX);
        target.bindWrite(true);
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, 900, 600, 0, -1000, 1000), VertexSorting.ORTHOGRAPHIC_Z);
        var view = RenderSystem.getModelViewStack();
        view.pushMatrix().identity();
        RenderSystem.applyModelViewMatrix();
        Lighting.setupFor3DItems();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        try {
            var state = PufferfishBlocks.PLACED_FISH.get().defaultBlockState().setValue(PlacedPufferfishBlock.FACING, Direction.SOUTH);
            var entity = new PlacedPufferfishBlockEntity(BlockPos.ZERO, state);
            var renderer = minecraft.getBlockEntityRenderDispatcher().getRenderer(entity);
            if (!(renderer instanceof PlacedPufferfishRenderer)
                    || minecraft.getBlockRenderer().getBlockModel(state) == minecraft.getModelManager().getMissingModel()) {
                throw new IllegalStateException("Placed fish renderer or block model is missing");
            }
            var variants = PufferfishPlacementGameTests.variants();
            var buffers = minecraft.renderBuffers().bufferSource();
            for (int i = 0; i < variants.size(); i++) {
                entity.setFish(variants.get(i));
                var pose = new PoseStack();
                pose.translate(75 + i % 6 * 150, 100 + i / 6 * 150, 0);
                pose.scale(130, -130, 130);
                pose.mulPose(Axis.XP.rotationDegrees(20));
                pose.mulPose(Axis.YP.rotationDegrees(30));
                pose.translate(-0.5, 0, -0.5);
                renderer.render(entity, 0, pose, buffers, 15728880, OverlayTexture.NO_OVERLAY);
                buffers.endBatch();
            }
            try (var image = Screenshot.takeScreenshot(target)) { image.writeToFile(Path.of("placed-pufferfish-preview.png")); }
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Could not save placed fish preview", exception);
        } finally {
            view.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.restoreProjectionMatrix();
            target.destroyBuffers();
            minecraft.getMainRenderTarget().bindWrite(true);
        }
    }
}
