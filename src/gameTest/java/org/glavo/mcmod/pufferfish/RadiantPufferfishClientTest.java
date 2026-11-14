// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.glavo.mcmod.pufferfish.client.PufferfishTextures;
import org.glavo.mcmod.pufferfish.client.RadiantPufferfishVisuals;
import org.glavo.mcmod.pufferfish.test.RenderCapture;
import org.glavo.mcmod.pufferfish.test.RenderTestScene;

public final class RadiantPufferfishClientTest {
    public static void verify(Minecraft minecraft) {
        verifyRadiantSprite(minecraft.getItemRenderer().getModel(PufferfishItems.RADIANT.toStack(), null, null, 0));
        verifyGlow(minecraft);
        verifyWornInventoryBounds(minecraft);
        var stars = RenderType.entityTranslucentEmissive(net.minecraft.resources.ResourceLocation.withDefaultNamespace("textures/particle/flash.png"));
        var glow = RenderType.entityTranslucentEmissive(PufferfishTextures.radiantGlow());
        for (var item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
            var stack = new ItemStack(item);
            if (!PufferfishItems.isRadiant(stack) || PufferfishItems.isPufferfish(stack)) continue;
            for (var context : ItemDisplayContext.values()) {
                var pose = new PoseStack();
                var capture = new RenderCapture();
                minecraft.getItemRenderer().renderStatic(stack, context, 15728880, OverlayTexture.NO_OVERLAY, pose, capture, null, 0);
                if (capture.count(stars) == 0 || capture.count(glow) == 0 || !pose.clear()) {
                    throw new IllegalStateException("Radiant item lost its glow or leaked its transform: " + stack + ", " + context);
                }
            }
        }
    }

    /// Worn fish must keep their glow inside a single inventory slot at every puff stage.
    private static void verifyWornInventoryBounds(Minecraft minecraft) {
        var glow = RenderType.entityTranslucentEmissive(PufferfishTextures.radiantGlow());
        var stars = RenderType.entityTranslucentEmissive(net.minecraft.resources.ResourceLocation.withDefaultNamespace("textures/particle/flash.png"));
        for (int stage = 1; stage <= 3; stage++) {
            var stack = PufferfishItems.RADIANT.toStack();
            PufferfishDurability.activate(stack);
            stack.setDamageValue(stack.getMaxDamage() * stage / 4 + 1);
            var halo = new RenderCapture();
            var body = new RenderCapture();
            var pose = new PoseStack();
            minecraft.getItemRenderer().renderStatic(stack, ItemDisplayContext.GUI, 15728880, OverlayTexture.NO_OVERLAY,
                    pose, type -> (type == glow || type == stars ? halo : body).getBuffer(type), null, 0);
            var bounds = halo.bounds();
            if (bounds.minX < -0.5 || bounds.maxX > 0.5 || bounds.minY < -0.5 || bounds.maxY > 0.5 || !pose.clear()) {
                throw new IllegalStateException("Worn fish halo escaped its inventory slot at stage " + stage + ": " + bounds);
            }
        }
    }

    private static void verifyGlow(Minecraft minecraft) {
        var hashes = new java.util.HashSet<Long>();
        int faintTotal = 0, brightTotal = 0;
        try (var scene = new RenderTestScene(minecraft, 256, 256, 0.04F, 0.07F, 0.10F)) {
            for (double phase : new double[] {0, Math.PI / 2, Math.PI, Math.PI * 1.5}) {
                scene.target().clear(Minecraft.ON_OSX);
                scene.target().bindWrite(true);
                var pose = new PoseStack();
                pose.translate(128, 128, 0);
                pose.scale(155, 155, 155);
                pose.translate(0, -18.0 / 16, 0);
                var buffers = minecraft.renderBuffers().bufferSource();
                RadiantPufferfishVisuals.render(PufferfishItems.RADIANT.toStack(), pose, buffers, OverlayTexture.NO_OVERLAY, phase);
                buffers.endBatch();
                try (var image = net.minecraft.client.Screenshot.takeScreenshot(scene.target())) {
                    long hash = 1;
                    int gold = 0, faint = 0, bright = 0;
                    for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
                        int pixel = image.getPixelRGBA(x, y);
                        hash = hash * 31 + pixel;
                        int red = pixel & 255, green = pixel >>> 8 & 255, blue = pixel >>> 16 & 255;
                        if (red > green && green > blue && red > 40) {
                            gold++;
                            if (red < 90) faint++;
                            if (red > 130) bright++;
                        }
                    }
                    if (gold < 25) throw new IllegalStateException("Radiant wisps disappeared during their animation cycle: " + phase);
                    faintTotal += faint;
                    brightTotal += bright;
                    hashes.add(hash);
                }
            }
            if (hashes.size() != 4) throw new IllegalStateException("Radiant wisps did not drift throughout the animation cycle");
            if (faintTotal < 100 || brightTotal < 20) throw new IllegalStateException("Radiant wisps lost their soft brightness layers");
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
        if (hashes.size() < 2) throw new IllegalStateException("Radiant star trail animation is static");
    }
}
