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
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.model.PufferfishBigModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.glavo.kaleidoscope.pufferfish.client.PufferfishTextures;
import org.joml.Matrix4f;

/// Renders normal, netherite, and heavy fish side by side for eye-color inspection.
public final class PufferfishEyesClientPreview {
    private PufferfishEyesClientPreview() {}

    public static void save(Minecraft minecraft) {
        var target = new TextureTarget(960, 360, true, Minecraft.ON_OSX);
        target.setClearColor(0.12F, 0.15F, 0.18F, 1);
        target.clear(Minecraft.ON_OSX);
        target.bindWrite(true);
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, 960, 360, 0, -1000, 1000), VertexSorting.ORTHOGRAPHIC_Z);
        var view = RenderSystem.getModelViewStack();
        view.pushMatrix().identity();
        RenderSystem.applyModelViewMatrix();
        Lighting.setupFor3DItems();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        try {
            var fish = new PufferfishBigModel<Entity>(minecraft.getEntityModels().bakeLayer(ModelLayers.PUFFERFISH_BIG));
            var textures = List.of(ResourceLocation.withDefaultNamespace("textures/entity/fish/pufferfish.png"),
                    PufferfishTextures.netheriteEntity(), PufferfishTextures.heavyEntity());
            var buffers = minecraft.renderBuffers().bufferSource();
            for (int i = 0; i < textures.size(); i++) {
                var pose = new PoseStack();
                pose.translate(160 + i * 320, 180, 0);
                pose.scale(300, -300, 300);
                pose.mulPose(Axis.XP.rotationDegrees(15));
                pose.mulPose(Axis.YP.rotationDegrees(25));
                pose.scale(1, -1, -1);
                pose.translate(0, -18.0F / 16, 0);
                fish.setupAnim(null, 0, 0, 0, 0, 0);
                fish.renderToBuffer(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(textures.get(i))), 15728880, OverlayTexture.NO_OVERLAY);
                buffers.endBatch();
            }
            try (var image = Screenshot.takeScreenshot(target)) {
                image.writeToFile(Path.of("pufferfish-white-eyes-preview.png"));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save pufferfish eye preview", exception);
        } finally {
            view.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.restoreProjectionMatrix();
            target.destroyBuffers();
            minecraft.getMainRenderTarget().bindWrite(true);
        }
    }
}
