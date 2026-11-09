// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.compat.cooking;

import com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.SteamerBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.client.resources.ItemRenderReplacer;
import com.github.ysbbbbbb.kaleidoscopecookery.client.resources.ItemRenderReplacerReloadListener;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModBlocks;
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
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.glavo.kaleidoscope.pufferfish.PufferfishItems;
import org.joml.Matrix4f;

/// Renders both bun variants at the positions and scale used by Cookery's steamer renderer.
public final class CookeryBaoziClientTest {
    private CookeryBaoziClientTest() {}

    public static void preview(Minecraft minecraft) {
        var target = new TextureTarget(800, 400, true, Minecraft.ON_OSX);
        target.setClearColor(0.16F, 0.18F, 0.21F, 1);
        target.clear(Minecraft.ON_OSX);
        target.bindWrite(true);
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, 800, 400, 0, -1000, 1000), VertexSorting.ORTHOGRAPHIC_Z);
        var view = RenderSystem.getModelViewStack();
        view.pushMatrix();
        view.identity();
        RenderSystem.applyModelViewMatrix();
        Lighting.setupFor3DItems();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        try {
            for (int variant = 0; variant < 2; variant++) {
                var pose = new PoseStack();
                pose.translate(200 + variant * 400, 270, 0);
                pose.scale(240, -240, 240);
                pose.mulPose(Axis.XP.rotationDegrees(50));
                pose.mulPose(Axis.YP.rotationDegrees(35));
                pose.translate(-0.5, 0, -0.5);
                var buffers = minecraft.renderBuffers().bufferSource();
                var state = ModBlocks.STEAMER.get().defaultBlockState().setValue(SteamerBlock.HALF, true)
                        .setValue(SteamerBlock.HAS_LID, false);
                minecraft.getBlockRenderer().renderSingleBlock(state, pose, buffers, 15728880, OverlayTexture.NO_OVERLAY);
                var stack = new ItemStack(variant == 0 ? PufferfishItems.RAW_BAOZI.get() : PufferfishItems.BAOZI.get());
                var model = ItemRenderReplacer.getModel(null, stack, ItemRenderReplacerReloadListener.INSTANCE.steamer());
                for (int i = 0; i < 4; i++) {
                    pose.pushPose();
                    pose.translate((i % 2) * 0.3 + 0.35, 0.25 + (i % 4) * 0.01, ((i / 2) % 2) * 0.3 + 0.35);
                    pose.translate(0, 0.4375, 0.4375);
                    pose.scale(0.5F, 0.5F, 0.5F);
                    minecraft.getItemRenderer().render(stack, ItemDisplayContext.FIXED, false, pose, buffers,
                            15728880, OverlayTexture.NO_OVERLAY, model);
                    pose.popPose();
                }
                buffers.endBatch();
            }
            try (var image = Screenshot.takeScreenshot(target)) {
                image.writeToFile(Path.of("pufferfish-baozi-steamer-preview.png"));
            }
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Could not save baozi steamer preview", exception);
        } finally {
            view.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.restoreProjectionMatrix();
            target.destroyBuffers();
            minecraft.getMainRenderTarget().bindWrite(true);
        }
    }
}
