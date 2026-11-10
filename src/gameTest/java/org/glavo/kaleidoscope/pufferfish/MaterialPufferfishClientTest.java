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
        verifyRadiantItems(minecraft);
        var target = new TextureTarget(1280, 320, true, Minecraft.ON_OSX);
        target.setClearColor(0.16F, 0.18F, 0.21F, 1);
        target.clear(Minecraft.ON_OSX);
        target.bindWrite(true);
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, 1280, 320, 0, -1000, 1000), VertexSorting.ORTHOGRAPHIC_Z);
        var view = RenderSystem.getModelViewStack();
        view.pushMatrix();
        view.identity();
        RenderSystem.applyModelViewMatrix();
        Lighting.setupFor3DItems();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        try {
            var items = List.of(PufferfishItems.COPPER.get(), PufferfishItems.IRON.get(), PufferfishItems.DIAMOND.get(), PufferfishItems.GOLDEN.get(), PufferfishItems.RADIANT.get());
            var fish = new PufferfishBigModel<Entity>(minecraft.getEntityModels().bakeLayer(ModelLayers.PUFFERFISH_BIG));
            fish.setupAnim(null, 0, 0, 0, 0, 0);
            for (int i = 0; i < items.size(); i++) {
                var stack = new ItemStack(items.get(i));
                var material = PufferfishItems.materialName(stack);
                var model = minecraft.getItemRenderer().getModel(stack, null, null, 0);
                var texture = PufferfishTextures.materialEntity(material);
                if (material.equals("radiant")) verifyRadiantSprite(model);
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
                org.glavo.kaleidoscope.pufferfish.client.RadiantPufferfishVisuals.render(stack, pose, buffers, OverlayTexture.NO_OVERLAY);
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

    private static void verifyRadiantItems(Minecraft minecraft) {
        var items = net.minecraft.core.registries.BuiltInRegistries.ITEM.stream()
                .map(ItemStack::new).filter(PufferfishItems::isRadiant).toList();
        int height = (items.size() + 4) / 5 * 200;
        var target = new TextureTarget(1000, height, true, Minecraft.ON_OSX);
        target.setClearColor(0.16F, 0.18F, 0.21F, 1);
        target.clear(Minecraft.ON_OSX);
        target.bindWrite(true);
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, 1000, height, 0, -1000, 1000), VertexSorting.ORTHOGRAPHIC_Z);
        var view = RenderSystem.getModelViewStack();
        view.pushMatrix().identity();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        try {
            var buffers = minecraft.renderBuffers().bufferSource();
            var stars = RenderType.entityTranslucentEmissive(net.minecraft.resources.ResourceLocation.withDefaultNamespace("textures/particle/flash.png"));
            for (int i = 0; i < items.size(); i++) {
                var stack = items.get(i);
                var model = minecraft.getItemRenderer().getModel(stack, null, null, 0);
                if (model.usesBlockLight()) Lighting.setupFor3DItems();
                else Lighting.setupForFlatItems();
                for (var context : ItemDisplayContext.values()) {
                    boolean[] hasTrails = {false};
                    var pose = new PoseStack();
                    pose.translate(context == ItemDisplayContext.GUI ? 100 + i % 5 * 200 : -10000, 100 + i / 5 * 200, 0);
                    pose.scale(112, -112, 112);
                    minecraft.getItemRenderer().renderStatic(stack, context, 15728880, OverlayTexture.NO_OVERLAY, pose, type -> {
                        if (type == stars) hasTrails[0] = true;
                        return buffers.getBuffer(type);
                    }, null, 0);
                    if (!PufferfishItems.isPufferfish(stack) && !hasTrails[0]) {
                        throw new IllegalStateException("Missing radiant item trails: " + stack + ", " + context);
                    }
                    buffers.endBatch();
                }
            }
            try (var image = Screenshot.takeScreenshot(target)) {
                image.writeToFile(Path.of("radiant-items-preview.png"));
            }
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Could not save radiant item preview", exception);
        } finally {
            view.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.restoreProjectionMatrix();
            target.destroyBuffers();
            minecraft.getMainRenderTarget().bindWrite(true);
        }
    }

    private static void verifyRadiantSprite(net.minecraft.client.resources.model.BakedModel model) {
        var sprite = model.getParticleIcon().contents();
        if (model.isGui3d() || model.isCustomRenderer() || sprite.getUniqueFrames().count() < 2) {
            throw new IllegalStateException("Radiant item must remain flat and animate its star trails");
        }
        var source = sprite.getOriginalImage();
        int size = sprite.width();
        var hashes = new java.util.HashSet<Long>();
        for (int frame : sprite.getUniqueFrames().toArray()) {
            long hash = 1;
            for (int y = 0; y < size; y++) for (int x = 0; x < size; x++) {
                hash = hash * 31 + source.getPixelRGBA(x, frame * size + y);
            }
            hashes.add(hash);
        }
        if (hashes.size() < 16) throw new IllegalStateException("Radiant star trail animation is static");
        try (var preview = new com.mojang.blaze3d.platform.NativeImage(size * 6 * 4, size * 4, false)) {
            for (int sample = 0; sample < 6; sample++) for (int y = 0; y < size * 4; y++) for (int x = 0; x < size * 4; x++) {
                int pixel = source.getPixelRGBA(x / 4, sample * 8 * size + y / 4);
                int alpha = pixel >>> 24;
                int red = ((pixel & 255) * alpha + 41 * (255 - alpha)) / 255;
                int green = ((pixel >>> 8 & 255) * alpha + 46 * (255 - alpha)) / 255;
                int blue = ((pixel >>> 16 & 255) * alpha + 54 * (255 - alpha)) / 255;
                preview.setPixelRGBA(sample * size * 4 + x, y, 0xFF000000 | blue << 16 | green << 8 | red);
            }
            preview.writeToFile(Path.of("radiant-item-animation-preview.png"));
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Could not save radiant animation preview", exception);
        }
    }
}
