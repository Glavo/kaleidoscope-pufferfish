// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.compat.cooking;

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
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.glavo.kaleidoscope.pufferfish.compat.cookery.CookeryFoodIntegration;
import org.joml.Matrix4f;

/// Checks every portion model and renders the item beside its placed meal states.
public final class CookeryPieClientTest {
    private CookeryPieClientTest() {}

    public static void verify(Minecraft minecraft) {
        var item = new ItemStack(CookeryFoodIntegration.STARGAZY_PIE_ITEM.get());
        var itemModel = minecraft.getItemRenderer().getModel(item, null, null, 0);
        if (itemModel == minecraft.getModelManager().getMissingModel() || item.getHoverName().getString().startsWith("block.")) {
            throw new IllegalStateException("Missing pufferfish pie item model or translation");
        }
        var target = new TextureTarget(1000, 340, true, Minecraft.ON_OSX);
        target.setClearColor(0.16F, 0.18F, 0.21F, 1);
        target.clear(Minecraft.ON_OSX);
        target.bindWrite(true);
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, 1000, 340, 0, -1000, 1000), VertexSorting.ORTHOGRAPHIC_Z);
        var view = RenderSystem.getModelViewStack();
        view.pushMatrix();
        view.identity();
        RenderSystem.applyModelViewMatrix();
        Lighting.setupFor3DItems();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        try {
            int previous = Integer.MAX_VALUE;
            var block = CookeryFoodIntegration.STARGAZY_PIE.get();
            for (int bites = 0; bites <= 4; bites++) {
                var state = block.defaultBlockState().setValue(block.getBites(), bites);
                var model = minecraft.getBlockRenderer().getBlockModel(state);
                int quads = model.getQuads(state, null, RandomSource.create()).size();
                if (model == minecraft.getModelManager().getMissingModel() || quads <= 0 || quads >= previous) {
                    throw new IllegalStateException("Missing or non-decreasing pufferfish pie portion model: " + bites);
                }
                previous = quads;
                var buffers = minecraft.renderBuffers().bufferSource();
                var pose = new PoseStack();
                pose.translate(100 + bites * 200, 100, 0);
                pose.scale(48, -48, 48);
                minecraft.getItemRenderer().renderStatic(item, ItemDisplayContext.GUI, 15728880,
                        OverlayTexture.NO_OVERLAY, pose, buffers, null, 0);
                pose = new PoseStack();
                pose.translate(100 + bites * 200, 250, 0);
                pose.scale(140, -140, 140);
                pose.mulPose(Axis.XP.rotationDegrees(55));
                pose.mulPose(Axis.YP.rotationDegrees(35));
                pose.translate(-0.5, 0, -0.5);
                minecraft.getBlockRenderer().renderSingleBlock(state, pose, buffers, 15728880, OverlayTexture.NO_OVERLAY);
                buffers.endBatch();
            }
            try (var image = Screenshot.takeScreenshot(target)) {
                image.writeToFile(Path.of("pufferfish-stargazy-pie-preview.png"));
            }
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Could not save pufferfish pie preview", exception);
        } finally {
            view.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.restoreProjectionMatrix();
            target.destroyBuffers();
            minecraft.getMainRenderTarget().bindWrite(true);
        }
    }
}
