// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexSorting;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.stats.StatsCounter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.glavo.mcmod.pufferfish.client.PufferfishFlailHeadRenderer;
import org.glavo.mcmod.pufferfish.test.ClientTestLevel;
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
        try {
            var level = ClientTestLevel.level(minecraft);
            player = new LocalPlayer(minecraft, level, ClientTestLevel.connection(minecraft),
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
            Lighting.setupFor3DItems();
            for (var arm : HumanoidArm.values()) {
                player.setMainArm(arm);
                for (var hand : InteractionHand.values()) {
                    for (int fov : new int[] {40, 70, 110}) {
                        for (float pitch : new float[] {-85, -35, 0, 60, 85}) {
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
                                var head = new org.glavo.mcmod.pufferfish.entity.PufferfishFlailHead(level, player, hand);
                                if (swing > 0) head.beginShortCast();
                                head.setPos(player.getEyePosition().add(player.getLookAngle().scale(head.isShortCast() ? 1.8 : 3.5)));
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
                                    capture.fixedGrip = head.isShortCast();
                                    renderHand(nativeRender, context.getItemInHandRenderer(), player, 0F, pitch, hand, swing, stack,
                                            swing * 0.5F, pose, capture, LIGHT);
                                    capture.verify(new Matrix4f(handProjection).mul(rotation),
                                            new Matrix4f(worldProjection).mul(rotation).transformProject(
                                                    head.getPosition(0).subtract(camera.getPosition()).toVector3f()));
                                    var worldCapture = new Capture();
                                    renderer.render(head, 0, 0, new PoseStack(), worldCapture, LIGHT);
                                    if (worldCapture.groups.size() != 1) {
                                        throw new IllegalStateException("First-person flail retained a second world-space chain");
                                    }
                                    NeoForge.EVENT_BUS.post(new RenderFrameEvent.Pre(DeltaTracker.ZERO));
                                    var stale = new Capture();
                                    renderHand(nativeRender, context.getItemInHandRenderer(), player, 0F, pitch, hand, swing, stack,
                                            swing * 0.5F, pose, stale, LIGHT);
                                    if (stale.groups.size() != 2) throw new IllegalStateException("Flail reused a previous frame's camera transform");
                                    if (fov == 70 && pitch == 0 && hand == InteractionHand.MAIN_HAND && swing == 0) {
                                        head.discard();
                                        NeoForge.EVENT_BUS.post(new RenderLevelStageEvent(RenderLevelStageEvent.Stage.AFTER_LEVEL,
                                                minecraft.levelRenderer, null, rotation, worldProjection, 0, DeltaTracker.ZERO, camera, null));
                                        verifyRelease(minecraft, player, nativeRender, pose);
                                    }
                                    if (fov == 70 && hand == InteractionHand.MAIN_HAND && swing == 0) {
                                        head.discard();
                                        NeoForge.EVENT_BUS.post(new RenderLevelStageEvent(RenderLevelStageEvent.Stage.AFTER_LEVEL,
                                                minecraft.levelRenderer, null, rotation, worldProjection, 0, DeltaTracker.ZERO, camera, null));
                                        verifyCharging(minecraft, player, nativeRender, pose, arm, renderer, camera);
                                    }
                                } finally {
                                    head.discard();
                                }
                            }
                        }
                    }
                }
            }
        } catch (ReflectiveOperationException exception) {
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
            minecraft.getMainRenderTarget().bindWrite(true);
        }
    }

    private static void renderHand(java.lang.reflect.Method nativeRender, ItemInHandRenderer renderer, AbstractClientPlayer player,
            float partial, float pitch, InteractionHand hand, float swing, ItemStack stack, float equip,
            PoseStack pose, MultiBufferSource buffers, int light) throws ReflectiveOperationException {
        var event = new RenderHandEvent(hand, pose, buffers, light, partial, pitch, swing, equip, stack);
        NeoForge.EVENT_BUS.post(event);
        if (!event.isCanceled()) nativeRender.invoke(renderer, player, partial, pitch, hand, swing, stack, equip, pose, buffers, light);
    }

    private static void verifyRelease(Minecraft minecraft, LocalPlayer player, java.lang.reflect.Method nativeRender,
            PoseStack pose) throws ReflectiveOperationException {
        var stack = new ItemStack(PufferfishItems.FLAIL.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        stack.use(player.level(), player, InteractionHand.MAIN_HAND);
        var remaining = net.minecraft.world.entity.LivingEntity.class.getDeclaredField("useItemRemaining");
        remaining.setAccessible(true);
        remaining.setInt(player, stack.getUseDuration(player) - 17);
        var head = new org.glavo.mcmod.pufferfish.entity.PufferfishFlailHead(player.level(), player, InteractionHand.MAIN_HAND);
        head.beginCharging();
        head.setPos(head.renderPosition(0));
        head.setOldPosAndRot();
        minecraft.level.addEntity(head);
        var beforeRelease = head.renderPosition(0);
        long gameTime = minecraft.level.getGameTime();
        player.releaseUsingItem();
        if (!head.isQueued() || head.renderPosition(0).distanceTo(beforeRelease) > 0.001) {
            throw new IllegalStateException("Releasing input replaced or teleported the spinning head");
        }
        try {
            boolean launched = false;
            for (int frame = 0; frame <= 12; frame++) {
                if (frame > 0) {
                    boolean queued = head.isQueued();
                    var previous = head.renderPosition(0.999F);
                    head.setOldPosAndRot();
                    head.tickCount++;
                    minecraft.level.setGameTime(minecraft.level.getGameTime() + 1);
                    head.tick();
                    if (queued && !head.isSpinning()) {
                        launched = true;
                        if (previous.distanceTo(head.renderPosition(0)) > 0.01) {
                            throw new IllegalStateException("Head jumped when the queued swing became flight");
                        }
                        var delayed = new org.glavo.mcmod.pufferfish.entity.PufferfishFlailHead(player.level(), player, InteractionHand.MAIN_HAND);
                        delayed.beginCharging();
                        delayed.tickCount = 500;
                        delayed.getEntityData().assignValues(head.getEntityData().packDirty());
                        delayed.setPos(head.position());
                        delayed.setDeltaMovement(head.getDeltaMovement());
                        delayed.tick();
                        if (delayed.isReturning()) throw new IllegalStateException("Late launch metadata counted charging ticks as flight time");
                        delayed.discard();
                    }
                }
                var capture = new Capture();
                renderHand(nativeRender, minecraft.getEntityRenderDispatcher().getItemInHandRenderer(), player, 0F, 0F,
                        InteractionHand.MAIN_HAND, 0F, stack, 0F, pose, capture, LIGHT);
                if (capture.groups.size() != 4) throw new IllegalStateException("Release lost its arm, grip, or tether");
                capture.verifyGrip();
            }
            if (!launched) throw new IllegalStateException("Queued client swing never released");
        } finally {
            minecraft.level.setGameTime(gameTime);
            head.discard();
            player.stopUsingItem();
        }
    }

    private static void verifyCharging(Minecraft minecraft, LocalPlayer player, java.lang.reflect.Method nativeRender,
            PoseStack pose, HumanoidArm arm, PufferfishFlailHeadRenderer renderer, Camera camera) throws ReflectiveOperationException {
        var remaining = net.minecraft.world.entity.LivingEntity.class.getDeclaredField("useItemRemaining");
        remaining.setAccessible(true);
        for (var item : new PufferfishFlailItem[] {PufferfishItems.FLAIL.get(), PufferfishItems.NETHERITE_FLAIL.get(), PufferfishItems.HEAVY_FLAIL.get()}) {
            var stack = new ItemStack(item);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            stack.use(player.level(), player, InteractionHand.MAIN_HAND);
            var head = new org.glavo.mcmod.pufferfish.entity.PufferfishFlailHead(player.level(), player, InteractionHand.MAIN_HAND);
            head.beginCharging();
            minecraft.level.addEntity(head);
            int duration = PufferfishFlailItem.chargeTicks(stack);
            double slow = PufferfishFlailItem.spinAngle(stack, 1) - PufferfishFlailItem.spinAngle(stack, 0);
            double medium = PufferfishFlailItem.spinAngle(stack, duration / 2F + 1) - PufferfishFlailItem.spinAngle(stack, duration / 2F);
            double fast = PufferfishFlailItem.spinAngle(stack, duration + 1) - PufferfishFlailItem.spinAngle(stack, duration);
            double held = PufferfishFlailItem.spinAngle(stack, duration + 21) - PufferfishFlailItem.spinAngle(stack, duration + 20);
            if (!(slow < medium && medium < fast && Math.abs(fast - held) < 0.000001)) {
                throw new IllegalStateException("Flail spin did not accelerate to a stable full-charge speed");
            }
            List<Vector3f> previous = null;
            var minimum = new Vector3f(Float.POSITIVE_INFINITY);
            var maximum = new Vector3f(Float.NEGATIVE_INFINITY);
            var wristMin = new Vector3f(Float.POSITIVE_INFINITY);
            var wristMax = new Vector3f(Float.NEGATIVE_INFINITY);
            try {
                for (int ticks = 0; ticks <= duration + 24; ticks++) {
                    remaining.setInt(player, stack.getUseDuration(player) - ticks);
                    var capture = new Capture();
                    renderHand(nativeRender, minecraft.getEntityRenderDispatcher().getItemInHandRenderer(), player, 0F, player.getXRot(),
                            InteractionHand.MAIN_HAND, 0F, stack, 0F, pose, capture, LIGHT);
                    var headPose = new PoseStack();
                    var relative = head.getPosition(0).subtract(camera.getPosition());
                    headPose.translate(relative.x, relative.y, relative.z);
                    var world = new Capture();
                    renderer.render(head, 0, 0, headPose, world, LIGHT);
                    if (capture.groups.size() != 4 || world.groups.size() != 1 || world.groups.getFirst().equals(previous)) {
                        throw new IllegalStateException("Charging flail did not render a moving head");
                    }
                    capture.verifyGrip();
                    var wrist = new Vector3f();
                    for (var vertex : capture.groups.get(1)) wrist.add(vertex);
                    wrist.div(capture.groups.get(1).size());
                    RenderSystem.getModelViewMatrix().transformPosition(wrist);
                    wristMin.min(wrist);
                    wristMax.max(wrist);
                    previous = world.groups.getFirst();
                    if (ticks >= 4) {
                        var center = new Vector3f();
                        for (var vertex : previous) {
                            var viewVertex = RenderSystem.getModelViewMatrix().transformPosition(new Vector3f(vertex));
                            if (viewVertex.x * (arm == HumanoidArm.RIGHT ? 1 : -1) < 0.60F) {
                                throw new IllegalStateException("Charging head entered the player's forward view");
                            }
                            center.add(viewVertex);
                        }
                        center.div(previous.size());
                        minimum.min(center);
                        maximum.max(center);
                    }
                }
                if (wristMax.y - wristMin.y < 0.85F || wristMax.z - wristMin.z < 0.85F) {
                    throw new IllegalStateException("First-person hand did not make a full shoulder swing");
                }
                if (maximum.x - minimum.x > 0.03F || maximum.y - minimum.y < 1.0F || maximum.z - minimum.z < 1.0F) {
                    throw new IllegalStateException("First-person flail did not rotate in a vertical plane beside the body");
                }
            } finally {
                head.discard();
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

        boolean fixedGrip;

        void verifyGrip() {
            var bands = groups.get(groups.size() - 2);
            var chain = groups.getLast();
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
            if (groups.size() == 4) {
                var palm = groups.getFirst();
                var grip = new Vector3f();
                for (var vertex : groups.get(1)) grip.add(vertex);
                grip.div(groups.get(1).size());
                float distance = Float.POSITIVE_INFINITY;
                for (int face = 0; face < 24; face += 4) for (int triangle = 1; triangle <= 2; triangle++) {
                    org.joml.Intersectionf.findClosestPointOnTriangle(palm.get(face), palm.get(face + triangle),
                            palm.get(face + triangle + 1), grip, closest);
                    distance = Math.min(distance, closest.distance(grip));
                }
                if (distance > 0.035F) throw new IllegalStateException("Flail handle detached from the visible palm: " + distance);
                int shoulderFace = 0;
                float farthest = -1;
                for (int face = 0; face < 24; face += 4) {
                    var center = new Vector3f();
                    for (int i = 0; i < 4; i++) center.add(palm.get(face + i));
                    float separation = center.div(4).distanceSquared(grip);
                    if (separation > farthest) { farthest = separation; shoulderFace = face; }
                }
                for (int i = 0; i < 4; i++) {
                    var vertex = RenderSystem.getModelViewMatrix().transformPosition(new Vector3f(palm.get(shoulderFace + i)));
                    if (vertex.z < -0.045F) throw new IllegalStateException("First-person shoulder entered the camera frustum: " + vertex);
                }
            }
            var bottom = new Vector3f();
            var top = new Vector3f();
            var first = new Vector3f();
            var last = new Vector3f();
            for (int i = 0; i < 24; i++) { bottom.add(bands.get(i)); top.add(bands.get(i + 24)); }
            for (int i = 0; i < 96; i++) { first.add(chain.get(i)); last.add(chain.get(chain.size() - 96 + i)); }
            if (!fixedGrip && groups.size() == 3 && chain.size() > 96 && top.sub(bottom).normalize().dot(last.sub(first).normalize()) < 0.995F) {
                throw new IllegalStateException("Grip remained upright instead of following chain tension");
            }
        }
    }
}
