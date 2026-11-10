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
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.glavo.kaleidoscope.pufferfish.client.PufferfishPumpBlockRenderer;
import org.joml.Matrix4f;

public final class PufferfishPumpClientTest {
    public static void verify(Minecraft minecraft) {
        var level = PufferfishHeadgearClientTest.level(minecraft);
        var state = PufferfishBlocks.PUMP.get().defaultBlockState().setValue(PufferfishPumpBlock.FACING, Direction.NORTH);
        var entity = new PufferfishPumpBlockEntity(BlockPos.ZERO, state);
        entity.setLevel(level);
        var renderer = minecraft.getBlockEntityRenderDispatcher().getRenderer(entity);
        if (!(renderer instanceof PufferfishPumpBlockRenderer)
                || minecraft.getBlockRenderer().getBlockModel(state) == minecraft.getModelManager().getMissingModel()) {
            throw new IllegalStateException("Foot pump renderer or block model is missing");
        }
        if (entity.compression(0) != 0 || !entity.triggerEvent(1, 0) || entity.compression(0) != 1
                || entity.compression(4) != 0.5F || entity.compression(8) != 0) {
            throw new IllegalStateException("Pedal did not compress and rebound after a synchronized block event");
        }
        var target = new TextureTarget(1000, 550, true, Minecraft.ON_OSX);
        target.setClearColor(0.12F, 0.15F, 0.18F, 1);
        target.clear(Minecraft.ON_OSX);
        target.bindWrite(true);
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, 1000, 550, 0, -1000, 1000), VertexSorting.ORTHOGRAPHIC_Z);
        var view = RenderSystem.getModelViewStack();
        view.pushMatrix().identity();
        RenderSystem.applyModelViewMatrix();
        Lighting.setupFor3DItems();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        try {
            var buffers = minecraft.renderBuffers().bufferSource();
            for (int i = 0; i < 4; i++) {
                var displayed = new PufferfishPumpBlockEntity(BlockPos.ZERO, state);
                if (i > 0) {
                    var fish = new ItemStack(i == 3 ? PufferfishItems.HEAVY_FLAIL.get() : Items.PUFFERFISH);
                    PufferfishDurability.activate(fish);
                    fish.setDamageValue(i == 1 ? fish.getMaxDamage() * 3 / 4 : fish.getMaxDamage() / 4);
                    displayed.setFish(fish);
                }
                displayed.setLevel(level);
                displayed.triggerEvent(1, 0);
                var pose = new PoseStack();
                pose.translate(125 + i * 250, 250, 0);
                pose.scale(225, -225, 225);
                pose.mulPose(Axis.XP.rotationDegrees(30));
                pose.mulPose(Axis.YP.rotationDegrees(-30));
                pose.translate(-0.5, 0, -0.5);
                renderer.render(displayed, i == 2 ? 0 : 8, pose, buffers, 15728880, OverlayTexture.NO_OVERLAY);
                buffers.endBatch();
            }
            for (int i = 0; i < 3; i++) {
                int size = new int[] {16, 32, 96}[i];
                var pose = new PoseStack();
                pose.translate(250 + i * 250, 410, 0);
                pose.scale(size, -size, size);
                minecraft.getItemRenderer().renderStatic(PufferfishItems.PUMP.toStack(), ItemDisplayContext.GUI,
                        15728880, OverlayTexture.NO_OVERLAY, pose, buffers, null, 0);
                buffers.endBatch();
            }
            try (var image = Screenshot.takeScreenshot(target)) { image.writeToFile(java.nio.file.Path.of("pufferfish-foot-pump-preview.png")); }
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Could not save foot pump preview", exception);
        } finally {
            view.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.restoreProjectionMatrix();
            target.destroyBuffers();
            minecraft.getMainRenderTarget().bindWrite(true);
        }
    }
}
