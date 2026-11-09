// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexSorting;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.stats.StatsCounter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.glavo.kaleidoscope.pufferfish.client.PufferfishFlailHeadRenderer;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/// Exercises the native first-person item transforms with separate world and hand projections.
public final class PufferfishFlailFirstPersonClientTest {
    private static final int LIGHT = 15728880;

    private PufferfishFlailFirstPersonClientTest() {}

    public static void verify(Minecraft minecraft) {
        var oldLevel = minecraft.level;
        var oldPlayer = minecraft.player;
        var oldCamera = minecraft.getCameraEntity();
        var oldCameraType = minecraft.options.getCameraType();
        boolean oldHideGui = minecraft.options.hideGui;
        var view = RenderSystem.getModelViewStack();
        RenderSystem.backupProjectionMatrix();
        view.pushMatrix();
        LocalPlayer player = null;
        TextureTarget target = null;
        try {
            var level = PufferfishHeadgearClientTest.level(minecraft);
            var connectionField = ClientLevel.class.getDeclaredField("connection");
            connectionField.setAccessible(true);
            player = new LocalPlayer(minecraft, level, (ClientPacketListener) connectionField.get(level),
                    new StatsCounter(), new ClientRecipeBook(), false, false) {
                @Override
                public boolean isSpectator() { return false; }
            };
            player.setPos(0, 64, 0);
            level.addEntity(player);
            minecraft.level = level;
            minecraft.player = player;
            minecraft.setCameraEntity(player);
            minecraft.options.setCameraType(CameraType.FIRST_PERSON);
            minecraft.options.hideGui = false;
            var nativeRender = ItemInHandRenderer.class.getDeclaredMethod("renderArmWithItem", AbstractClientPlayer.class,
                    float.class, float.class, InteractionHand.class, float.class, ItemStack.class, float.class,
                    PoseStack.class, MultiBufferSource.class, int.class);
            nativeRender.setAccessible(true);
            var context = new EntityRendererProvider.Context(minecraft.getEntityRenderDispatcher(), minecraft.getItemRenderer(),
                    minecraft.getBlockRenderer(), minecraft.getEntityRenderDispatcher().getItemInHandRenderer(),
                    minecraft.getResourceManager(), minecraft.getEntityModels(), minecraft.font);
            var renderer = new PufferfishFlailHeadRenderer(context);
            target = new TextureTarget(640, 360, true, Minecraft.ON_OSX);
            target.setClearColor(0.35F, 0.55F, 0.8F, 1);
            Lighting.setupFor3DItems();
            for (var arm : HumanoidArm.values()) {
                player.setMainArm(arm);
                for (var hand : InteractionHand.values()) {
                    for (int fov : new int[] {40, 70, 110}) {
                        for (float pitch : new float[] {-35, 0, 60}) {
                            for (float swing : new float[] {0, 0.3F, 0.75F}) {
                                player.setXRot(pitch);
                                player.setYRot(60);
                                player.setOldPosAndRot();
                                var camera = new Camera() {
                                    void position(Vec3 eye) { setPosition(eye); setRotation(60, pitch); }
                                };
                                camera.position(player.getEyePosition());
                                var rotation = new Matrix4f().rotation(camera.rotation().conjugate(new Quaternionf()));
                                var worldProjection = new Matrix4f().perspective((float) Math.toRadians(fov), 640F / 360, 0.05F, 256);
                                var handProjection = new Matrix4f().perspective((float) Math.toRadians(70), 640F / 360, 0.05F, 256);
                                var stack = new ItemStack(PufferfishItems.NETHERITE_FLAIL.get());
                                player.setItemInHand(hand, stack);
                                var head = new org.glavo.kaleidoscope.pufferfish.entity.PufferfishFlailHead(level, player, hand);
                                head.setPos(player.getEyePosition().add(player.getLookAngle().scale(3.5)));
                                head.setOldPosAndRot();
                                level.addEntity(head);
                                if (swing == 0.75F) {
                                    head.tickCount = 8;
                                    head.tick();
                                }
                                try {
                                    NeoForge.EVENT_BUS.post(new RenderFrameEvent.Pre(DeltaTracker.ZERO));
                                    NeoForge.EVENT_BUS.post(new RenderLevelStageEvent(RenderLevelStageEvent.Stage.AFTER_LEVEL,
                                            minecraft.levelRenderer, null, rotation, worldProjection, 0, DeltaTracker.ZERO, camera, null));
                                    view.set(rotation);
                                    RenderSystem.applyModelViewMatrix();
                                    RenderSystem.setProjectionMatrix(handProjection, VertexSorting.DISTANCE_TO_ORIGIN);
                                    var pose = new PoseStack();
                                    pose.mulPose(new Matrix4f(rotation).invert());
                                    // Include camera motion as well as the native swing and equip transforms.
                                    pose.translate(0.02, -0.015, 0);
                                    var capture = new Capture();
                                    nativeRender.invoke(context.getItemInHandRenderer(), player, 0F, pitch, hand, swing, stack,
                                            swing * 0.5F, pose, capture, LIGHT);
                                    capture.verify(new Matrix4f(handProjection).mul(rotation),
                                            new Matrix4f(worldProjection).mul(rotation).transformProject(
                                                    head.getPosition(0).subtract(camera.getPosition()).toVector3f()));
                                    var worldCapture = new Capture();
                                    renderer.render(head, 0, 0, new PoseStack(), worldCapture, LIGHT);
                                    if (worldCapture.groups.size() != 1) {
                                        throw new IllegalStateException("First-person flail retained a second world-space chain");
                                    }
                                    if (fov == 70 && pitch == -35 && hand == InteractionHand.MAIN_HAND && (swing == 0 || swing == 0.3F)) {
                                        target.clear(Minecraft.ON_OSX);
                                        target.bindWrite(true);
                                        var buffers = minecraft.renderBuffers().bufferSource();
                                        RenderSystem.setProjectionMatrix(worldProjection, VertexSorting.DISTANCE_TO_ORIGIN);
                                        var headPose = new PoseStack();
                                        var relative = head.position().subtract(camera.getPosition());
                                        headPose.translate(relative.x, relative.y, relative.z);
                                        renderer.render(head, 0, 0, headPose, buffers, LIGHT);
                                        buffers.endBatch();
                                        RenderSystem.clear(256, Minecraft.ON_OSX);
                                        RenderSystem.setProjectionMatrix(handProjection, VertexSorting.DISTANCE_TO_ORIGIN);
                                        nativeRender.invoke(context.getItemInHandRenderer(), player, 0F, pitch, hand, swing, stack,
                                                swing * 0.5F, pose, buffers, LIGHT);
                                        buffers.endBatch();
                                        try (var image = Screenshot.takeScreenshot(target)) {
                                            image.writeToFile(Path.of("pufferfish-flail-first-person-" + arm.name().toLowerCase(java.util.Locale.ROOT)
                                                    + (swing == 0 ? "-idle.png" : "-swing.png")));
                                        }
                                    }
                                    NeoForge.EVENT_BUS.post(new RenderFrameEvent.Pre(DeltaTracker.ZERO));
                                    var stale = new Capture();
                                    nativeRender.invoke(context.getItemInHandRenderer(), player, 0F, pitch, hand, swing, stack,
                                            swing * 0.5F, pose, stale, LIGHT);
                                    if (stale.groups.size() != 2) throw new IllegalStateException("Flail reused a previous frame's camera transform");
                                    if (fov == 70 && pitch == -35 && hand == InteractionHand.MAIN_HAND && swing == 0) {
                                        head.discard();
                                        verifyCharging(minecraft, player, nativeRender, pose, target, arm);
                                    }
                                } finally {
                                    head.discard();
                                }
                            }
                        }
                    }
                }
            }
        } catch (ReflectiveOperationException | java.io.IOException exception) {
            throw new IllegalStateException("Could not verify native first-person flail rendering", exception);
        } finally {
            if (player != null) player.discard();
            minecraft.player = oldPlayer;
            minecraft.level = oldLevel;
            minecraft.setCameraEntity(oldCamera);
            minecraft.options.setCameraType(oldCameraType);
            minecraft.options.hideGui = oldHideGui;
            NeoForge.EVENT_BUS.post(new RenderFrameEvent.Pre(DeltaTracker.ZERO));
            view.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.restoreProjectionMatrix();
            if (target != null) target.destroyBuffers();
            minecraft.getMainRenderTarget().bindWrite(true);
        }
    }

    private static void verifyCharging(Minecraft minecraft, LocalPlayer player, java.lang.reflect.Method nativeRender,
            PoseStack pose, TextureTarget target, HumanoidArm arm) throws ReflectiveOperationException, java.io.IOException {
        var remaining = net.minecraft.world.entity.LivingEntity.class.getDeclaredField("useItemRemaining");
        remaining.setAccessible(true);
        for (var item : new PufferfishFlailItem[] {PufferfishItems.FLAIL.get(), PufferfishItems.NETHERITE_FLAIL.get()}) {
            var stack = new ItemStack(item);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            stack.use(player.level(), player, InteractionHand.MAIN_HAND);
            int duration = PufferfishFlailItem.chargeTicks(stack);
            double slow = PufferfishFlailItem.spinAngle(stack, 1) - PufferfishFlailItem.spinAngle(stack, 0);
            double medium = PufferfishFlailItem.spinAngle(stack, duration / 2F + 1) - PufferfishFlailItem.spinAngle(stack, duration / 2F);
            double fast = PufferfishFlailItem.spinAngle(stack, duration + 1) - PufferfishFlailItem.spinAngle(stack, duration);
            double held = PufferfishFlailItem.spinAngle(stack, duration + 21) - PufferfishFlailItem.spinAngle(stack, duration + 20);
            if (!(slow < medium && medium < fast && Math.abs(fast - held) < 0.000001)) {
                throw new IllegalStateException("Flail spin did not accelerate to a stable full-charge speed");
            }
            List<Vector3f> previous = null;
            try {
                for (int ticks : new int[] {0, duration / 4, duration / 2, duration, duration + 10}) {
                    remaining.setInt(player, stack.getUseDuration(player) - ticks);
                    var capture = new Capture();
                    nativeRender.invoke(minecraft.getEntityRenderDispatcher().getItemInHandRenderer(), player, 0F, -35F,
                            InteractionHand.MAIN_HAND, 0F, stack, 0F, pose, capture, LIGHT);
                    if (capture.groups.size() != 4 || capture.groups.get(3).equals(previous)) {
                        throw new IllegalStateException("Charging flail did not render a moving head");
                    }
                    capture.verifyGrip();
                    previous = capture.groups.get(3);
                    if (arm == HumanoidArm.RIGHT) {
                        target.clear(Minecraft.ON_OSX);
                        target.bindWrite(true);
                        var buffers = minecraft.renderBuffers().bufferSource();
                        nativeRender.invoke(minecraft.getEntityRenderDispatcher().getItemInHandRenderer(), player, 0F, -35F,
                                InteractionHand.MAIN_HAND, 0F, stack, 0F, pose, buffers, LIGHT);
                        buffers.endBatch();
                        try (var image = Screenshot.takeScreenshot(target)) {
                            image.writeToFile(Path.of("pufferfish-flail-charge-" + (item == PufferfishItems.FLAIL.get() ? "normal-" : "netherite-") + ticks + ".png"));
                        }
                    }
                }
            } finally {
                player.stopUsingItem();
            }
        }
    }

    private static final class Capture implements MultiBufferSource {
        private final List<List<Vector3f>> groups = new ArrayList<>();

        @Override
        public VertexConsumer getBuffer(RenderType type) {
            var vertices = new ArrayList<Vector3f>();
            groups.add(vertices);
            return new VertexConsumer() {
                @Override
                public VertexConsumer addVertex(float x, float y, float z) {
                    if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) throw new IllegalStateException("Non-finite flail geometry");
                    vertices.add(new Vector3f(x, y, z));
                    return this;
                }
                @Override
                public VertexConsumer setColor(int red, int green, int blue, int alpha) { return this; }
                @Override
                public VertexConsumer setUv(float u, float v) { return this; }
                @Override
                public VertexConsumer setUv1(int u, int v) { return this; }
                @Override
                public VertexConsumer setUv2(int u, int v) { return this; }
                @Override
                public VertexConsumer setNormal(float x, float y, float z) { return this; }
            };
        }

        void verify(Matrix4f handToClip, Vector3f headClip) {
            if (groups.size() != 3 || groups.get(1).size() != 48 || groups.get(2).size() < 192) {
                throw new IllegalStateException("First-person grip did not render its tether");
            }
            var chain = groups.get(2);
            verifyGrip();
            var end = new Vector3f();
            for (var vertex : chain.subList(chain.size() - 96, chain.size())) end.add(vertex);
            handToClip.transformProject(end.div(96));
            if (!end.isFinite() || Math.hypot(end.x - headClip.x, end.y - headClip.y) > 0.12) {
                throw new IllegalStateException("First-person chain missed the world-rendered head");
            }
        }

        void verifyGrip() {
            var bands = groups.get(1);
            var chain = groups.get(2);
            float gap = Float.POSITIVE_INFINITY;
            var closest = new Vector3f();
            for (int face = 24; face < 48; face += 4) {
                for (var link : chain.subList(0, 96)) {
                    for (int triangle = 1; triangle <= 2; triangle++) {
                        org.joml.Intersectionf.findClosestPointOnTriangle(bands.get(face), bands.get(face + triangle),
                                bands.get(face + triangle + 1), link, closest);
                        gap = Math.min(gap, closest.distance(link));
                    }
                }
            }
            if (gap > 0.015F) throw new IllegalStateException("First-person chain detached from the grip: " + gap);
        }
    }
}
