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
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.model.PufferfishBigModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.glavo.kaleidoscope.pufferfish.client.PufferfishTextures;
import org.joml.Matrix4f;

/// Checks material fish resources and renders their icons and wearable models after each resource reload.
public final class MaterialPufferfishClientTest {
    private MaterialPufferfishClientTest() {}

    public static void verify(Minecraft minecraft) {
        var target = new TextureTarget(1024, 280, true, Minecraft.ON_OSX);
        target.setClearColor(0.16F, 0.18F, 0.21F, 1);
        target.clear(Minecraft.ON_OSX);
        target.bindWrite(true);
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, 1024, 280, 0, -1000, 1000), VertexSorting.ORTHOGRAPHIC_Z);
        var view = RenderSystem.getModelViewStack();
        view.pushMatrix();
        view.identity();
        RenderSystem.applyModelViewMatrix();
        Lighting.setupFor3DItems();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        try {
            var items = List.of(PufferfishItems.COPPER.get(), PufferfishItems.IRON.get(), PufferfishItems.DIAMOND.get(), PufferfishItems.GOLDEN.get());
            var fish = new PufferfishBigModel<Entity>(minecraft.getEntityModels().bakeLayer(ModelLayers.PUFFERFISH_BIG));
            fish.setupAnim(null, 0, 0, 0, 0, 0);
            for (int i = 0; i < items.size(); i++) {
                var stack = new ItemStack(items.get(i));
                var material = PufferfishItems.materialName(stack);
                var model = minecraft.getItemRenderer().getModel(stack, null, null, 0);
                var texture = PufferfishTextures.materialEntity(material);
                if (model == minecraft.getModelManager().getMissingModel() || model.getQuads(null, null, RandomSource.create()).isEmpty()
                        || !model.getParticleIcon().contents().name().equals(KaleidoscopePufferfish.id("item/" + material + "_pufferfish"))
                        || !texture.getNamespace().equals(KaleidoscopePufferfish.MODID) || stack.getHoverName().getString().startsWith("item.")) {
                    throw new IllegalStateException("Missing material pufferfish resources: " + material);
                }
                var buffers = minecraft.renderBuffers().bufferSource();
                Lighting.setupForFlatItems();
                var pose = new PoseStack();
                pose.translate(128 + i * 256, 65, 0);
                pose.scale(72, -72, 72);
                minecraft.getItemRenderer().renderStatic(stack, ItemDisplayContext.GUI, 15728880, OverlayTexture.NO_OVERLAY, pose, buffers, null, 0);
                buffers.endBatch();
                Lighting.setupFor3DItems();
                pose = new PoseStack();
                pose.translate(128 + i * 256, 195, 0);
                pose.scale(150, 150, 150);
                pose.mulPose(Axis.XP.rotationDegrees(-10));
                pose.mulPose(Axis.YP.rotationDegrees(155));
                pose.translate(0, -18.0 / 16, 0);
                fish.renderToBuffer(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(texture)), 15728880, OverlayTexture.NO_OVERLAY, -1);
                buffers.endBatch();
            }
            try (var image = Screenshot.takeScreenshot(target)) {
                image.writeToFile(Path.of("material-pufferfish-preview.png"));
            }
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Could not save material pufferfish preview", exception);
        } finally {
            view.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.restoreProjectionMatrix();
            target.destroyBuffers();
            minecraft.getMainRenderTarget().bindWrite(true);
        }
    }
}
