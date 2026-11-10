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
        var state = PufferfishBlocks.PUMP.get().defaultBlockState().setValue(PufferfishPumpBlock.FACING, Direction.SOUTH);
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
        var target = new TextureTarget(1600, 700, true, Minecraft.ON_OSX);
        target.setClearColor(0.12F, 0.15F, 0.18F, 1);
        target.clear(Minecraft.ON_OSX);
        target.bindWrite(true);
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, 1600, 700, 0, -1000, 1000), VertexSorting.ORTHOGRAPHIC_Z);
        var view = RenderSystem.getModelViewStack();
        view.pushMatrix().identity();
        RenderSystem.applyModelViewMatrix();
        Lighting.setupFor3DItems();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        try {
            var buffers = minecraft.renderBuffers().bufferSource();
            for (int i = 0; i < 4; i++) {
                var displayed = new PufferfishPumpBlockEntity(BlockPos.ZERO, state);
                var fish = new ItemStack(Items.PUFFERFISH);
                PufferfishDurability.activate(fish);
                fish.setDamageValue(fish.getMaxDamage() * i / 4);
                displayed.setFish(fish);
                var mouth = org.glavo.kaleidoscope.pufferfish.client.PufferfishPumpModel.mouth(fish);
                double[] heights = {0.175, 0.14375, 0.125, 0.1};
                double[] fronts = {0.1, 0.08125, 0.0625, 0.0375};
                if (Math.abs(mouth.x - 5.0 / 16) > 0.000001 || Math.abs(mouth.y - heights[i]) > 0.000001
                        || Math.abs(mouth.z - fronts[i]) > 0.000001) {
                    throw new IllegalStateException("Pump needle missed the vanilla mouth position at stage " + i);
                }
                displayed.setLevel(level);
                displayed.triggerEvent(1, 0);
                var pose = new PoseStack();
                pose.translate(200 + i * 400, 330, 0);
                pose.scale(360, -360, 360);
                pose.mulPose(Axis.XP.rotationDegrees(20));
                pose.mulPose(Axis.YP.rotationDegrees(25));
                pose.translate(-0.5, 0, -0.5);
                renderer.render(displayed, i == 2 ? 0 : 8, pose, buffers, 15728880, OverlayTexture.NO_OVERLAY);
                buffers.endBatch();
            }
            for (int i = 0; i < 3; i++) {
                int size = new int[] {16, 32, 96}[i];
                var pose = new PoseStack();
                pose.translate(400 + i * 400, 550, 0);
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
