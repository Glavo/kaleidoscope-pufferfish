// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.glavo.kaleidoscope.pufferfish.client.DeflatingPufferfishModel;
import org.glavo.kaleidoscope.pufferfish.client.PufferfishFlailModel;
import org.glavo.kaleidoscope.pufferfish.client.PufferfishTextures;
import org.joml.Matrix4f;

public final class PufferfishDeflationClientTest {
    public static void verify(Minecraft minecraft) {
        var geometry = new DeflatingPufferfishModel<Entity>(minecraft.getEntityModels());
        for (var original : PufferfishPlacementGameTests.variants()) {
            for (int stage = 0; stage < 4; stage++) {
                var stack = worn(original, stage);
                var model = minecraft.getItemRenderer().getModel(stack, null, null, 0);
                if (model.isCustomRenderer() != (stage > 0)) {
                    throw new IllegalStateException("Worn fish did not select puff-state item rendering: " + stack);
                }
                geometry.prepare(stack);
                var root = geometry.root();
                var body = root.getChild("body").getRandomCube(RandomSource.create(0));
                float expectedWidth = stage == 0 ? 8 : stage == 3 ? 3 : 5;
                float expectedHeight = stage == 3 ? 2 : expectedWidth;
                if (body.maxX - body.minX != expectedWidth || body.maxY - body.minY != expectedHeight
                        || body.maxZ - body.minZ != expectedWidth || root.xScale != root.yScale || root.yScale != root.zScale) {
                    throw new IllegalStateException("Puff-state geometry was flattened instead of using vanilla proportions");
                }
                if (stage == 3 && (!root.hasChild("left_eye") || !root.hasChild("right_eye") || !root.hasChild("back_fin"))) {
                    throw new IllegalStateException("Deflated fish lost its raised eyes or tail");
                }
            }
            geometry.prepare(original);
            if (geometry.root().getChild("body").yScale != 1 || geometry.root().getChild("body").y != 22) {
                throw new IllegalStateException("Shared fish model retained deformation from another stack");
            }
        }
        var pump = PufferfishItems.PUMP.toStack();
        if (!minecraft.getItemRenderer().getModel(pump, null, null, 0).isCustomRenderer()
                || pump.getHoverName().getString().startsWith("block.")) {
            throw new IllegalStateException("Pump model or translation is missing");
        }
        preview(minecraft, geometry);
    }

    private static ItemStack worn(ItemStack original, int stage) {
        var stack = original.copyWithCount(1);
        PufferfishDurability.activate(stack);
        stack.setDamageValue((stack.getMaxDamage() * stage + 3) / 4);
        return stack;
    }

    private static void preview(Minecraft minecraft, DeflatingPufferfishModel<Entity> fish) {
        var target = new TextureTarget(1000, 800, true, Minecraft.ON_OSX);
        target.setClearColor(0.12F, 0.15F, 0.18F, 1);
        target.clear(Minecraft.ON_OSX);
        target.bindWrite(true);
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, 1000, 800, 0, -1000, 1000), VertexSorting.ORTHOGRAPHIC_Z);
        var view = RenderSystem.getModelViewStack();
        view.pushMatrix().identity();
        RenderSystem.applyModelViewMatrix();
        Lighting.setupFor3DItems();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        try {
            var buffers = minecraft.renderBuffers().bufferSource();
            var flail = new PufferfishFlailModel();
            for (int stage = 0; stage < 4; stage++) {
                for (int row = 0; row < 2; row++) {
                    var stack = worn(row == 0 ? new ItemStack(Items.PUFFERFISH) : PufferfishItems.HEAVY.toStack(), stage);
                    var pose = new PoseStack();
                    pose.translate(125 + stage * 250, 120 + row * 200, 0);
                    pose.scale(240, -240, 240);
                    pose.mulPose(Axis.XP.rotationDegrees(15));
                    pose.mulPose(Axis.YP.rotationDegrees(25));
                    pose.scale(1, -1, -1);
                    pose.translate(0, -18.0F / 16, 0);
                    fish.prepare(stack);
                    fish.setupAnim(null, 0, 0, 0, 0, 0);
                    var texture = row == 0 ? ResourceLocation.withDefaultNamespace("textures/entity/fish/pufferfish.png") : PufferfishTextures.heavyEntity();
                    fish.renderToBuffer(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(texture)), 15728880, OverlayTexture.NO_OVERLAY);
                }
                var pose = new PoseStack();
                pose.translate(125 + stage * 250, 510, 0);
                pose.scale(250, -250, 250);
                pose.mulPose(Axis.YP.rotationDegrees(25));
                flail.renderHead(worn(PufferfishItems.FLAIL.toStack(), stage), pose, buffers, 15728880, OverlayTexture.NO_OVERLAY, 0);
                buffers.endBatch();
                for (int size : new int[] {16, 64}) {
                    pose = new PoseStack();
                    pose.translate(90 + stage * 250 + size, 630, 0);
                    pose.scale(size, -size, size);
                    minecraft.getItemRenderer().renderStatic(worn(new ItemStack(Items.PUFFERFISH), stage), ItemDisplayContext.GUI,
                            15728880, OverlayTexture.NO_OVERLAY, pose, buffers, null, 0);
                    buffers.endBatch();
                }
            }
            for (int size : new int[] {16, 32, 80}) {
                var pose = new PoseStack();
                pose.translate(100 + size * 5, 735, 0);
                pose.scale(size, -size, size);
                minecraft.getItemRenderer().renderStatic(PufferfishItems.PUMP.toStack(), ItemDisplayContext.GUI,
                        15728880, OverlayTexture.NO_OVERLAY, pose, buffers, null, 0);
                buffers.endBatch();
            }
            try (var image = Screenshot.takeScreenshot(target)) { image.writeToFile(java.nio.file.Path.of("pufferfish-deflation-preview.png")); }
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Could not save fish deflation preview", exception);
        } finally {
            view.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.restoreProjectionMatrix();
            target.destroyBuffers();
            minecraft.getMainRenderTarget().bindWrite(true);
        }
    }
}
