// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import com.mojang.math.Axis;

public final class PufferfishBoatClientTest {
    public static void verify(Minecraft minecraft) {
        for (var entry : PufferfishBoats.VARIANTS.entrySet()) {
            var stack = new ItemStack(entry.getValue().item().get());
            var model = minecraft.getItemRenderer().getModel(stack, null, null, 0);
            if (model == minecraft.getModelManager().getMissingModel()
                    || !model.getParticleIcon().contents().name().equals(KaleidoscopePufferfish.id("item/" + entry.getKey()))
                    || stack.getHoverName().getString().startsWith("item.")) {
                throw new IllegalStateException("Missing pufferfish boat model or translation");
            }
            var boat = entry.getValue().entity().get().create(PufferfishHeadgearClientTest.level(minecraft));
            var renderer = (org.glavo.kaleidoscope.pufferfish.client.PufferfishBoatRenderer) minecraft.getEntityRenderDispatcher().getRenderer(boat);
            var texture = renderer.getModelWithLocation(boat).getFirst();
            if (!(minecraft.getTextureManager().getTexture(texture) instanceof DynamicTexture dynamic)
                    || dynamic.getPixels() == null || dynamic.getPixels().getWidth() != 128) {
                throw new IllegalStateException("Missing generated pufferfish boat texture");
            }
        }
        preview(minecraft);
    }

    private static void preview(Minecraft minecraft) {
        var target = new TextureTarget(900, 540, true, Minecraft.ON_OSX);
        target.setClearColor(0.16F, 0.18F, 0.21F, 1);
        target.clear(Minecraft.ON_OSX);
        target.bindWrite(true);
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, 900, 540, 0, -1000, 1000), VertexSorting.ORTHOGRAPHIC_Z);
        var view = RenderSystem.getModelViewStack();
        view.pushMatrix();
        view.identity();
        RenderSystem.applyModelViewMatrix();
        Lighting.setupForFlatItems();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        var level = PufferfishHeadgearClientTest.level(minecraft);
        try {
            int index = 0;
            for (var item : PufferfishBoats.VARIANTS.values()) {
                var buffers = minecraft.renderBuffers().bufferSource();
                var pose = new PoseStack();
                pose.translate(75 + index % 6 * 150, 28 + index / 6 * 180, 0);
                pose.scale(30, -30, 30);
                minecraft.getItemRenderer().renderStatic(new ItemStack(item.item().get()), ItemDisplayContext.GUI, 15728880,
                        OverlayTexture.NO_OVERLAY, pose, buffers, null, 0);
                pose = new PoseStack();
                pose.translate(75 + index % 6 * 150, 72 + index / 6 * 180, 0);
                pose.scale(48, -48, 48);
                pose.mulPose(Axis.XP.rotationDegrees(25));
                var boat = item.entity().get().create(level);
                var renderer = (org.glavo.kaleidoscope.pufferfish.client.PufferfishBoatRenderer) minecraft.getEntityRenderDispatcher().getRenderer(boat);
                renderer.render(boat, 35, 0, pose, buffers, 15728880);
                buffers.endBatch();
                index++;
            }
            try (var image = Screenshot.takeScreenshot(target)) {
                image.writeToFile(Path.of("pufferfish-boats-preview.png"));
            }
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Could not save boat preview", exception);
        } finally {
            view.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.restoreProjectionMatrix();
            target.destroyBuffers();
            minecraft.getMainRenderTarget().bindWrite(true);
        }
    }
}
