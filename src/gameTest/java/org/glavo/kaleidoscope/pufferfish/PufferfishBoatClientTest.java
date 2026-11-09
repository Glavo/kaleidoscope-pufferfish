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
                    || !model.isCustomRenderer()
                    || !model.getParticleIcon().contents().name().equals(KaleidoscopePufferfish.id("block/" + entry.getKey().replace("_boat", "")))
                    || stack.getHoverName().getString().startsWith("item.")) {
                throw new IllegalStateException("Missing pufferfish boat model or translation");
            }
            var boat = entry.getValue().entity().get().create(PufferfishHeadgearClientTest.level(minecraft));
            var renderer = (org.glavo.kaleidoscope.pufferfish.client.PufferfishBoatRenderer) minecraft.getEntityRenderDispatcher().getRenderer(boat);
            var texture = renderer.getModelWithLocation(boat).getFirst();
            var kayak = renderer.getModelWithLocation(boat).getSecond();
            var player = new net.minecraft.client.player.RemotePlayer((net.minecraft.client.multiplayer.ClientLevel) boat.level(),
                    new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "kayak-render-test"));
            player.startRiding(boat, true);
            for (int input = 1; input <= 3; input++) {
                boat.setPaddleState((input & 1) != 0, (input & 2) != 0);
                kayak.setupAnim(boat, 0.25F, 0, 0, 0, 0);
                float angle = kayak.parts().iterator().next().xRot;
                kayak.setupAnim(boat, 0.75F, 0, 0, 0, 0);
                if (angle == kayak.parts().iterator().next().xRot) throw new IllegalStateException("Kayak paddle did not animate for steering input");
            }
            boat.setPaddleState(false, false);
            kayak.setupAnim(boat, 0, 0, 0, 0, 0);
            if (kayak.parts().iterator().next().xRot != 0) throw new IllegalStateException("Idle paddle did not return to the deck");
            player.stopRiding();
            if (!(minecraft.getTextureManager().getTexture(texture) instanceof DynamicTexture dynamic)
                    || dynamic.getPixels() == null || dynamic.getPixels().getWidth() != 160) {
                throw new IllegalStateException("Missing generated pufferfish boat texture");
            }
        }
        preview(minecraft);
        detail(minecraft);
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
                pose.scale(48, -48, 48);
                minecraft.getItemRenderer().renderStatic(new ItemStack(item.item().get()), ItemDisplayContext.GUI, 15728880,
                        OverlayTexture.NO_OVERLAY, pose, buffers, null, 0);
                pose = new PoseStack();
                pose.translate(75 + index % 6 * 150, 130 + index / 6 * 180, 0);
                pose.scale(42, -42, 42);
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
    private static void detail(Minecraft minecraft) {
        var target = new TextureTarget(960, 480, true, Minecraft.ON_OSX);
        target.setClearColor(0.16F, 0.18F, 0.21F, 1);
        target.clear(Minecraft.ON_OSX);
        target.bindWrite(true);
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, 960, 480, 0, -1000, 1000), VertexSorting.ORTHOGRAPHIC_Z);
        var view = RenderSystem.getModelViewStack();
        view.pushMatrix();
        view.identity();
        RenderSystem.applyModelViewMatrix();
        Lighting.setupForFlatItems();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        var level = PufferfishHeadgearClientTest.level(minecraft);
        try {
            int index = 0;
            for (var name : java.util.List.of("pufferfish_skin_boat", "red_pufferfish_skin_boat", "black_pufferfish_skin_boat", "golden_pufferfish_skin_boat")) {
                var item = PufferfishBoats.VARIANTS.get(name);
                var buffers = minecraft.renderBuffers().bufferSource();
                for (int size : new int[] {16, 32, 64}) {
                    var iconPose = new PoseStack();
                    iconPose.translate(index * 240 + (size == 16 ? 28 : size == 32 ? 82 : 168), 56, 0);
                    iconPose.scale(size, -size, size);
                    minecraft.getItemRenderer().renderStatic(new ItemStack(item.item().get()), ItemDisplayContext.GUI, 15728880,
                            OverlayTexture.NO_OVERLAY, iconPose, buffers, null, 0);
                }
                var pose = new PoseStack();
                pose.translate(120 + index * 240, 230, 0);
                pose.scale(65, -65, 65);
                pose.mulPose(Axis.XP.rotationDegrees(25));
                var boat = item.entity().get().create(level);
                var renderer = (org.glavo.kaleidoscope.pufferfish.client.PufferfishBoatRenderer) minecraft.getEntityRenderDispatcher().getRenderer(boat);
                renderer.render(boat, 65, 0, pose, buffers, 15728880);
                pose = new PoseStack();
                pose.translate(120 + index * 240, 365, 0);
                pose.scale(65, -65, 65);
                renderer.render(boat, 90, 0, pose, buffers, 15728880);
                buffers.endBatch();
                index++;
            }
            try (var image = Screenshot.takeScreenshot(target)) {
                image.writeToFile(Path.of("pufferfish-kayak-detail.png"));
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
