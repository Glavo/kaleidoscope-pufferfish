// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.math.Axis;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.glavo.kaleidoscope.pufferfish.client.PufferfishTextures;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/// Checks the complete third-person orbit against the body and previews the native player hand layer.
public final class PufferfishFlailOrbitClientTest {
    private static final int LIGHT = 15728880;

    private PufferfishFlailOrbitClientTest() {}

    public static void verify(Minecraft minecraft) {
        var oldLevel = minecraft.level;
        var level = PufferfishHeadgearClientTest.level(minecraft);
        var player = new RemotePlayer(level, new GameProfile(UUID.randomUUID(), "flail-orbit-test")) {
            @Override
            protected PlayerInfo getPlayerInfo() { return null; }
            @Override
            public boolean isUsingItem() { return !getUseItem().isEmpty(); }
        };
        minecraft.level = level;
        level.addEntity(player);
        var context = new EntityRendererProvider.Context(minecraft.getEntityRenderDispatcher(), minecraft.getItemRenderer(),
                minecraft.getBlockRenderer(), minecraft.getEntityRenderDispatcher().getItemInHandRenderer(),
                minecraft.getResourceManager(), minecraft.getEntityModels(), minecraft.font);
        try {
            var remaining = LivingEntity.class.getDeclaredField("useItemRemaining");
            remaining.setAccessible(true);
            for (boolean slim : new boolean[] {false, true}) {
                var renderer = new PlayerRenderer(context, slim);
                var model = renderer.getModel();
                model.young = false;
                var layer = new ItemInHandLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>(renderer, context.getItemInHandRenderer());
                for (var arm : HumanoidArm.values()) {
                    player.setMainArm(arm);
                    for (var hand : InteractionHand.values()) {
                        var usedArm = hand == InteractionHand.MAIN_HAND ? arm : arm.getOpposite();
                        int side = usedArm == HumanoidArm.RIGHT ? -1 : 1;
                        var stack = new ItemStack(PufferfishItems.NETHERITE_FLAIL.get());
                        player.setItemInHand(hand, stack);
                        player.startUsingItem(hand);
                        model.rightArmPose = usedArm == HumanoidArm.RIGHT ? HumanoidModel.ArmPose.ITEM : HumanoidModel.ArmPose.EMPTY;
                        model.leftArmPose = usedArm == HumanoidArm.LEFT ? HumanoidModel.ArmPose.ITEM : HumanoidModel.ArmPose.EMPTY;
                        for (boolean crouching : new boolean[] {false, true}) {
                            model.crouching = crouching;
                            var min = new Vector3f(Float.POSITIVE_INFINITY);
                            var max = new Vector3f(Float.NEGATIVE_INFINITY);
                            int duration = PufferfishFlailItem.chargeTicks(stack);
                            for (int ticks = 4; ticks <= duration + 24; ticks++) {
                                remaining.setInt(player, stack.getUseDuration(player) - ticks);
                                model.setupAnim(player, 1.2F, 0.6F, ticks, 35, 45);
                                var capture = new Capture();
                                var pose = new PoseStack();
                                layer.render(pose, capture, LIGHT, player, 0, 0, 0, ticks, 0, 0);
                                if (capture.head.isEmpty() || !pose.clear()) throw new IllegalStateException("Missing third-person charging head");
                                var center = new Vector3f();
                                for (var vertex : capture.head) {
                                    if (side * vertex.x < 0.50F) throw new IllegalStateException("Charging flail crossed the player's body");
                                    center.add(vertex);
                                }
                                center.div(capture.head.size());
                                min.min(center);
                                max.max(center);
                                if (!slim && arm == HumanoidArm.RIGHT && hand == InteractionHand.MAIN_HAND && !crouching
                                        && ticks >= duration && ticks < duration + 12 && (ticks - duration) % 3 == 0) {
                                    preview(minecraft, player, model, layer, ticks - duration);
                                }
                            }
                            if (max.x - min.x > 0.03F || max.y - min.y < 1.0F || max.z - min.z < 1.0F) {
                                throw new IllegalStateException("Third-person flail did not orbit vertically beside the body");
                            }
                        }
                        player.stopUsingItem();
                        player.setItemInHand(hand, ItemStack.EMPTY);
                    }
                }
            }
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not set up charging orbit checks", exception);
        } finally {
            player.discard();
            minecraft.level = oldLevel;
        }
    }

    private static void preview(Minecraft minecraft, AbstractClientPlayer player, PlayerModel<AbstractClientPlayer> model,
            ItemInHandLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> layer, int frame) {
        var target = new TextureTarget(1000, 440, true, Minecraft.ON_OSX);
        target.setClearColor(0.12F, 0.15F, 0.18F, 1);
        target.clear(Minecraft.ON_OSX);
        target.bindWrite(true);
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, 1000, 440, 0, -1000, 1000), VertexSorting.ORTHOGRAPHIC_Z);
        var view = RenderSystem.getModelViewStack();
        view.pushMatrix().identity();
        RenderSystem.applyModelViewMatrix();
        Lighting.setupFor3DItems();
        try {
            for (int panel = 0; panel < 2; panel++) {
                var pose = new PoseStack();
                pose.translate(250 + panel * 500, 120, 0);
                pose.scale(170, 170, -170);
                pose.mulPose(Axis.YP.rotationDegrees(panel == 0 ? 160 : 90));
                var buffers = minecraft.renderBuffers().bufferSource();
                model.renderToBuffer(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(player.getSkin().texture())), LIGHT, OverlayTexture.NO_OVERLAY);
                layer.render(pose, buffers, LIGHT, player, 0, 0, 0, 0, 0, 0);
                buffers.endBatch();
            }
            try (var image = Screenshot.takeScreenshot(target)) {
                image.writeToFile(Path.of("pufferfish-flail-side-orbit-" + frame + ".png"));
            }
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Could not save side-orbit preview", exception);
        } finally {
            view.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.restoreProjectionMatrix();
            target.destroyBuffers();
            minecraft.getMainRenderTarget().bindWrite(true);
        }
    }

    private static final class Capture implements MultiBufferSource, VertexConsumer {
        private final List<Vector3f> head = new ArrayList<>();
        private boolean recording;
        @Override
        public VertexConsumer getBuffer(RenderType type) {
            recording = type == RenderType.entityCutoutNoCull(PufferfishTextures.netheriteEntity());
            return this;
        }
        @Override
        public VertexConsumer addVertex(float x, float y, float z) { if (recording) head.add(new Vector3f(x, y, z)); return this; }
        @Override
        public VertexConsumer setColor(int r, int g, int b, int a) { return this; }
        @Override
        public VertexConsumer setUv(float u, float v) { return this; }
        @Override
        public VertexConsumer setUv1(int u, int v) { return this; }
        @Override
        public VertexConsumer setUv2(int u, int v) { return this; }
        @Override
        public VertexConsumer setNormal(float x, float y, float z) { return this; }
    }
}
