// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.math.Axis;
import java.io.IOException;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix4f;

/// Saves an in-engine preview of tiled ordinary, netherite, and heavy skins.
public final class PufferfishSkinClientPreview {
    /// Prevents construction of this preview utility.
    private PufferfishSkinClientPreview() {}

    /// Renders three 3-by-3 skin patches into a PNG in the client's working directory.
    public static void save(Minecraft minecraft) {
        var target = new TextureTarget(900, 360, true, Minecraft.ON_OSX);
        target.setClearColor(0.08F, 0.14F, 0.2F, 1);
        target.clear(Minecraft.ON_OSX);
        target.bindWrite(true);
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, 900, 360, 0, -1000, 1000), VertexSorting.ORTHOGRAPHIC_Z);
        var view = RenderSystem.getModelViewStack();
        view.pushMatrix().identity();
        RenderSystem.applyModelViewMatrix();
        Lighting.setupFor3DItems();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        try {
            var skins = new PufferfishSkinBlock[] {PufferfishBlocks.skin(null), PufferfishBlocks.NETHERITE_SKIN.get(), PufferfishBlocks.HEAVY_SKIN.get()};
            for (int i = 0; i < skins.length; i++) {
                var pose = new PoseStack();
                pose.translate(150 + i * 300, 180, 0);
                pose.scale(72, -72, 72);
                pose.mulPose(Axis.XP.rotationDegrees(65));
                pose.mulPose(Axis.YP.rotationDegrees(15));
                pose.translate(-1.5, 0, -1.5);
                var buffers = minecraft.renderBuffers().bufferSource();
                for (int z = 0; z < 3; z++) {
                    for (int x = 0; x < 3; x++) {
                        pose.pushPose();
                        pose.translate(x, 0, z);
                        minecraft.getBlockRenderer().renderSingleBlock(skins[i].defaultBlockState(),
                                pose, buffers, 15728880, OverlayTexture.NO_OVERLAY);
                        pose.popPose();
                    }
                }
                buffers.endBatch();
            }
            try (var image = Screenshot.takeScreenshot(target)) {
                image.writeToFile(Path.of("pufferfish-skin-preview.png"));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save skin preview", exception);
        } finally {
            view.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.restoreProjectionMatrix();
            target.destroyBuffers();
            minecraft.getMainRenderTarget().bindWrite(true);
        }
    }
}
