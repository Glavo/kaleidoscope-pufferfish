// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.FastColor;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import org.glavo.mcmod.pufferfish.test.TestImages;
import org.joml.Matrix4f;

final class PufferfishSpawnEggClientTest {
    static void verify(Minecraft minecraft) {
        var eggs = new ArrayList<ItemStack>();
        eggs.add(new ItemStack(Items.PUFFERFISH_SPAWN_EGG));
        for (var dye : DyeColor.values()) eggs.add(PufferfishMobs.dyedEgg(dye));
        PufferfishMobs.VARIANTS.values().forEach(variant -> eggs.add(variant.egg().toStack()));
        eggs.add(new ItemStack(Items.CHICKEN_SPAWN_EGG));
        for (var egg : eggs) {
            var model = minecraft.getItemRenderer().getModel(egg, null, null, 0);
            var quads = model.getQuads(null, null, RandomSource.create());
            if (model == minecraft.getModelManager().getMissingModel() || quads.isEmpty()) {
                throw new IllegalStateException("Missing spawn egg model: " + egg);
            }
            for (int layer = 0; layer < 2; layer++) {
                int color = minecraft.getItemColors().getColor(egg, layer);
                int expected = ((SpawnEggItem) egg.getItem()).getColor(layer);
                int tint = layer;
                if (FastColor.ARGB32.alpha(color) != 255 || (color & 0xFFFFFF) != (expected & 0xFFFFFF)
                        || quads.stream().noneMatch(quad -> quad.isTinted() && quad.getTintIndex() == tint)) {
                    throw new IllegalStateException("Invisible or incorrectly tinted spawn egg layer " + layer + ": " + egg
                            + ", actual=" + Integer.toHexString(color) + ", expected=" + Integer.toHexString(expected)
                            + ", tints=" + quads.stream().map(quad -> quad.getTintIndex()).distinct().toList());
                }
            }
        }
        var target = new TextureTarget(800, 400, true, Minecraft.ON_OSX);
        target.setClearColor(0, 0, 0, 0);
        target.clear(Minecraft.ON_OSX);
        target.bindWrite(true);
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, 800, 400, 0, -1000, 1000), VertexSorting.ORTHOGRAPHIC_Z);
        var view = RenderSystem.getModelViewStack();
        view.pushMatrix().identity();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        Lighting.setupForFlatItems();
        try {
            var buffers = minecraft.renderBuffers().bufferSource();
            for (int i = 0; i < eggs.size(); i++) {
                var pose = new PoseStack();
                pose.translate(50 + i % 8 * 100, 50 + i / 8 * 100, 0);
                pose.scale(64, -64, 64);
                minecraft.getItemRenderer().renderStatic(eggs.get(i), ItemDisplayContext.GUI,
                        15728880, OverlayTexture.NO_OVERLAY, pose, buffers, null, 0);
            }
            buffers.endBatch();
            try (var image = Screenshot.takeScreenshot(target)) {
                for (int i = 0; i < eggs.size(); i++) {
                    int visible = 0;
                    for (int y = 0; y < 100; y++) for (int x = 0; x < 100; x++) {
                        if ((image.getPixelRGBA(i % 8 * 100 + x, i / 8 * 100 + y) >>> 24) != 0) visible++;
                    }
                    if (visible < 100) throw new IllegalStateException("Spawn egg rendered transparent: " + eggs.get(i));
                }
                TestImages.write(image, java.nio.file.Path.of("pufferfish-spawn-eggs-preview.png"));
            }
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Could not save spawn egg preview", exception);
        } finally {
            view.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.restoreProjectionMatrix();
            target.destroyBuffers();
            minecraft.getMainRenderTarget().bindWrite(true);
        }
    }
}
