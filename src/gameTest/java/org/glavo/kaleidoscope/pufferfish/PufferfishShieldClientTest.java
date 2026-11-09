// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.authlib.GameProfile;
import java.nio.file.Path;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.joml.Matrix4f;

public final class PufferfishShieldClientTest {
    private static final int LIGHT = 15728880;

    public static void verify(Minecraft minecraft) {
        var level = PufferfishHeadgearClientTest.level(minecraft);
        var player = new RemotePlayer(level, new GameProfile(UUID.randomUUID(), "shield-render-test")) {
            @Override
            public boolean isUsingItem() { return !getUseItem().isEmpty(); }
        };
        for (var entry : PufferfishShields.VARIANTS.entrySet()) {
            var stack = new ItemStack(entry.getValue().get());
            var model = minecraft.getItemRenderer().getModel(stack, level, player, 0);
            var texture = KaleidoscopePufferfish.id("block/" + entry.getKey().replace("_shield", ""));
            if (!model.isCustomRenderer() || !model.getParticleIcon().contents().name().equals(texture)
                    || stack.getHoverName().getString().startsWith("item.")) throw new IllegalStateException("Missing skin shield assets");
            player.setItemInHand(InteractionHand.OFF_HAND, stack);
            player.startUsingItem(InteractionHand.OFF_HAND);
            var blocking = minecraft.getItemRenderer().getModel(stack, level, player, 0);
            if (blocking == model || blocking == minecraft.getModelManager().getMissingModel()) {
                throw new IllegalStateException("Skin shield did not select its blocking model");
            }
            player.stopUsingItem();
            for (var context : ItemDisplayContext.values()) {
                var capture = new Capture();
                var pose = new PoseStack();
                IClientItemExtensions.of(stack).getCustomRenderer().renderByItem(stack, context, pose, capture, LIGHT, OverlayTexture.NO_OVERLAY);
                if (capture.vertices != 52 || !pose.clear()) throw new IllegalStateException("Shield frame, grip or skin did not render");
            }
        }
        preview(minecraft);
    }

    private static void preview(Minecraft minecraft) {
        var target = new TextureTarget(720, 360, true, Minecraft.ON_OSX);
        target.setClearColor(0.55F, 0.55F, 0.55F, 1);
        target.clear(Minecraft.ON_OSX);
        target.bindWrite(true);
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, 720, 360, 0, -1000, 1000), VertexSorting.ORTHOGRAPHIC_Z);
        var view = RenderSystem.getModelViewStack();
        view.pushMatrix().identity();
        RenderSystem.applyModelViewMatrix();
        Lighting.setupForFlatItems();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        try {
            int index = 0;
            for (var item : PufferfishShields.VARIANTS.values()) {
                var pose = new PoseStack();
                pose.translate(60 + index % 6 * 120, 55 + index / 6 * 120, 0);
                pose.scale(72, -72, 72);
                var buffers = minecraft.renderBuffers().bufferSource();
                minecraft.getItemRenderer().renderStatic(new ItemStack(item.get()), ItemDisplayContext.GUI, LIGHT,
                        OverlayTexture.NO_OVERLAY, pose, buffers, null, 0);
                buffers.endBatch();
                index++;
            }
            try (var image = Screenshot.takeScreenshot(target)) {
                image.writeToFile(Path.of("pufferfish-skin-shields-preview.png"));
            }
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Could not save shield preview", exception);
        } finally {
            view.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.restoreProjectionMatrix();
            target.destroyBuffers();
            minecraft.getMainRenderTarget().bindWrite(true);
        }
    }

    private static final class Capture implements MultiBufferSource, VertexConsumer {
        private int vertices;
        @Override public VertexConsumer getBuffer(RenderType type) { return this; }
        @Override public VertexConsumer addVertex(float x, float y, float z) {
            if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) throw new IllegalStateException("Non-finite shield vertex");
            vertices++;
            return this;
        }
        @Override public VertexConsumer setColor(int r, int g, int b, int a) { return this; }
        @Override public VertexConsumer setUv(float u, float v) { return this; }
        @Override public VertexConsumer setUv1(int u, int v) { return this; }
        @Override public VertexConsumer setUv2(int u, int v) { return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { return this; }
    }
}
