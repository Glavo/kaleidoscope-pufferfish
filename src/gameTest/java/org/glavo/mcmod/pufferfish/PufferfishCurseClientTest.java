// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import java.io.IOException;
import java.nio.file.Path;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.glavo.mcmod.pufferfish.client.PufferfishCurseParticle;
import org.glavo.mcmod.pufferfish.test.ClientTestLevel;
import org.glavo.mcmod.pufferfish.test.TestImages;
import org.joml.Matrix4f;

public final class PufferfishCurseClientTest {
    public static void verify(Minecraft minecraft) {
        var sprite = minecraft.getMobEffectTextures().get(PufferfishEffects.PUFFERFISH_CURSE);
        if (!sprite.contents().name().equals(PufferfishEffects.PUFFERFISH_CURSE.getId())
                || PufferfishEffects.PUFFERFISH_CURSE.get().getDisplayName().getString().startsWith("effect.")) {
            throw new IllegalStateException("Missing pufferfish curse sprite or translation");
        }
        try (var iconPreview = new NativeImage(216, 216, true)) {
            var icon = sprite.contents().getOriginalImage();
            if (icon.getWidth() != 18 || icon.getHeight() != 18) throw new IllegalStateException("Incorrect curse icon size");
            for (int y = 0; y < 216; y++) for (int x = 0; x < 216; x++) {
                iconPreview.setPixelRGBA(x, y, icon.getPixelRGBA(x / 12, y / 12));
            }
            TestImages.write(iconPreview, Path.of("pufferfish-curse-icon-preview.png"));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save curse icon preview", exception);
        }
        var target = new TextureTarget(320, 240, true, Minecraft.ON_OSX);
        target.setClearColor(0.08F, 0.14F, 0.2F, 1);
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f().perspective((float) Math.toRadians(70), 320F / 240, 0.05F, 100),
                VertexSorting.DISTANCE_TO_ORIGIN);
        var view = RenderSystem.getModelViewStack();
        view.pushMatrix().identity();
        RenderSystem.applyModelViewMatrix();
        Lighting.setupFor3DItems();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        minecraft.particleEngine.setLevel(ClientTestLevel.level(minecraft));
        try (var preview = new NativeImage(1920, 240, true)) {
            var particle = minecraft.particleEngine.createParticle(
                    new ItemParticleOption(PufferfishEffects.CURSE_APPEARANCE.get(), new ItemStack(Items.PUFFERFISH)), 0, 0, 0, 0, 0, 0);
            if (!(particle instanceof PufferfishCurseParticle) || particle.getRenderType() != ParticleRenderType.CUSTOM) {
                throw new IllegalStateException("Missing pufferfish curse particle provider");
            }
            var camera = new Camera();
            for (int frame = 0; frame < 6; frame++) {
                target.clear(Minecraft.ON_OSX);
                target.bindWrite(true);
                particle.render(null, camera, 0);
                try (var image = Screenshot.takeScreenshot(target)) {
                    int changed = 0;
                    int background = image.getPixelRGBA(0, 0);
                    for (int y = 0; y < 240; y++) for (int x = 0; x < 320; x++) {
                        int pixel = image.getPixelRGBA(x, y);
                        preview.setPixelRGBA(frame * 320 + x, y, pixel);
                        if (pixel != background) changed++;
                    }
                    if (frame == 3 && changed < 2000) throw new IllegalStateException("Curse apparition did not fill the center of the view");
                }
                for (int tick = 0; tick < 5; tick++) particle.tick();
            }
            particle.tick();
            if (particle.isAlive()) throw new IllegalStateException("Curse apparition did not expire");
            TestImages.write(preview, Path.of("pufferfish-curse-preview.png"));
            previewVariants(minecraft, target, camera);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save curse preview", exception);
        } finally {
            minecraft.particleEngine.setLevel(minecraft.level);
            view.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.restoreProjectionMatrix();
            target.destroyBuffers();
            minecraft.getMainRenderTarget().bindWrite(true);
        }
    }

    private static void previewVariants(Minecraft minecraft, TextureTarget target, Camera camera) throws IOException {
        int columns = 7;
        int rows = (PufferfishMobs.VARIANTS.size() + columns - 1) / columns;
        var hashes = new java.util.HashSet<Integer>();
        try (var preview = new NativeImage(columns * 320, rows * 240, true)) {
            int index = 0;
            for (var variant : PufferfishMobs.VARIANTS.values()) {
                var particle = minecraft.particleEngine.createParticle(
                        new ItemParticleOption(PufferfishEffects.CURSE_APPEARANCE.get(), variant.fish().toStack()), 0, 0, 0, 0, 0, 0);
                if (!(particle instanceof PufferfishCurseParticle)) throw new IllegalStateException("Missing variant apparition");
                for (int tick = 0; tick < 15; tick++) particle.tick();
                target.clear(Minecraft.ON_OSX);
                target.bindWrite(true);
                particle.render(null, camera, 0);
                try (var image = Screenshot.takeScreenshot(target)) {
                    int hash = 1;
                    for (int y = 0; y < 240; y++) for (int x = 0; x < 320; x++) {
                        int pixel = image.getPixelRGBA(x, y);
                        hash = 31 * hash + pixel;
                        preview.setPixelRGBA(index % columns * 320 + x, index / columns * 240 + y, pixel);
                    }
                    hashes.add(hash);
                }
                particle.remove();
                index++;
            }
            if (hashes.size() < 10) throw new IllegalStateException("Distinct material apparitions share the wrong texture");
            TestImages.write(preview, Path.of("pufferfish-curse-variants-preview.png"));
        }
    }
}
