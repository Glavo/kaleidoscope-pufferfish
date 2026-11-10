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
        verify(minecraft, CookeryFoodIntegration.STARGAZY_PIE.get(), CookeryFoodIntegration.STARGAZY_PIE_ITEM.get(), "stargazy-pie");
        verify(minecraft, CookeryFoodIntegration.ASPIC.get(), CookeryFoodIntegration.ASPIC_ITEM.get(), "aspic");
    }

    private static void verify(Minecraft minecraft, com.github.ysbbbbbb.kaleidoscopecookery.block.food.FoodBiteBlock block,
            net.minecraft.world.item.Item food, String name) {
        var item = new ItemStack(food);
        var itemModel = minecraft.getItemRenderer().getModel(item, null, null, 0);
        if (itemModel == minecraft.getModelManager().getMissingModel() || item.getHoverName().getString().startsWith("block.")) {
            throw new IllegalStateException("Missing pufferfish meal model or translation: " + name);
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
            for (int bites = 0; bites <= 4; bites++) {
                var state = block.defaultBlockState().setValue(block.getBites(), bites);
                var model = minecraft.getBlockRenderer().getBlockModel(state);
                int quads = model.getQuads(state, null, RandomSource.create()).size();
                if (model == minecraft.getModelManager().getMissingModel() || quads <= 0 || quads >= previous) {
                    throw new IllegalStateException("Missing or non-decreasing pufferfish meal portion model: " + name + "/" + bites);
                }
                previous = quads;
                var buffers = minecraft.renderBuffers().bufferSource();
                var pose = new PoseStack();
                pose.translate(100 + bites * 200, 100, 0);
                pose.scale(48, -48, 48);
                var display = name.equals("aspic") && bites == 0
                        ? new ItemStack(org.glavo.kaleidoscope.pufferfish.PufferfishItems.SKIN_BROTH.get()) : item;
                if (minecraft.getItemRenderer().getModel(display, null, null, 0) == minecraft.getModelManager().getMissingModel()) {
                    throw new IllegalStateException("Missing broth model");
                }
                minecraft.getItemRenderer().renderStatic(display, ItemDisplayContext.GUI, 15728880,
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
                image.writeToFile(Path.of("pufferfish-" + name + "-preview.png"));
            }
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Could not save pufferfish meal preview: " + name, exception);
        } finally {
            view.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.restoreProjectionMatrix();
            target.destroyBuffers();
            minecraft.getMainRenderTarget().bindWrite(true);
        }
    }
}
