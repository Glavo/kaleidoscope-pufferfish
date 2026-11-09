// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.math.Axis;
import java.nio.file.Path;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.phys.Vec3;
import org.glavo.kaleidoscope.pufferfish.client.PufferfishBoatRenderer;
import org.glavo.kaleidoscope.pufferfish.client.PufferfishRowingAnimation;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class PufferfishRowingClientTest {
    public static void verify(Minecraft minecraft) {
        var level = PufferfishHeadgearClientTest.level(minecraft);
        var boat = PufferfishBoats.VARIANTS.get("pufferfish_skin_boat").entity().get().create(level);
        var player = new RemotePlayer(level, new GameProfile(UUID.randomUUID(), "rowing-test")) {
            @Override
            protected net.minecraft.client.multiplayer.PlayerInfo getPlayerInfo() { return null; }
        };
        var passenger = new RemotePlayer(level, new GameProfile(UUID.randomUUID(), "passenger-test")) {
            @Override
            protected net.minecraft.client.multiplayer.PlayerInfo getPlayerInfo() { return null; }
        };
        player.startRiding(boat, true);
        try {
            var field = Boat.class.getDeclaredField("paddlePositions");
            field.setAccessible(true);
            var phases = (float[]) field.get(boat);
            for (boolean slim : new boolean[] {false, true}) {
                var model = new PlayerModel<RemotePlayer>(minecraft.getEntityModels().bakeLayer(slim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER), slim);
                model.riding = true;
                model.young = false;
                for (boolean tandem : new boolean[] {false, true}) {
                    if (tandem) passenger.startRiding(boat, true);
                    for (float yaw : new float[] {0, 90, -140}) {
                        boat.setPos(12345, 64, -76543);
                        boat.setYRot(yaw);
                        boat.setOldPosAndRot();
                        boat.positionRider(player);
                        player.setOldPosAndRot();
                        player.yBodyRot = player.yBodyRotO = yaw;
                        for (int input = 1; input <= 3; input++) {
                            boat.setPaddleState((input & 1) != 0, (input & 2) != 0);
                            for (int frame = 0; frame < 16; frame++) {
                                phases[0] = phases[1] = frame * (float) Math.PI / 16;
                                model.setupAnim(player, 0, 0, frame, 0, 0);
                                if (!PufferfishRowingAnimation.poseArms(model, player, 1, yaw)) throw new IllegalStateException("Driver did not pose rowing arms");
                                for (var arm : HumanoidArm.values()) {
                                    var part = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
                                    var sleeve = arm == HumanoidArm.RIGHT ? model.rightSleeve : model.leftSleeve;
                                    if (sleeve.xRot != part.xRot || sleeve.zRot != part.zRot) throw new IllegalStateException("Sleeve missed rowing pose");
                                    var pose = new PoseStack();
                                    pose.mulPose(PufferfishRowingAnimation.playerTransform(player, yaw));
                                    part.translateAndRotate(pose);
                                    var point = pose.last().pose().transformPosition(new Vector3f(arm == HumanoidArm.RIGHT ? -1F / 16 : 1F / 16, 9F / 16, 0));
                                    var actual = player.position().add(point.x, point.y, point.z);
                                    double gap = actual.distanceTo(PufferfishRowingAnimation.grip(boat, arm, 1));
                                    if (gap > 0.13) throw new IllegalStateException("Rowing hand missed paddle: " + gap);
                                }
                            }
                        }
                    }
                    if (PufferfishRowingAnimation.isRowing(passenger)) throw new IllegalStateException("Rear passenger was rowing");
                    passenger.stopRiding();
                }
                boat.setPaddleState(false, false);
                if (PufferfishRowingAnimation.poseArms(model, player, 1, 0)) throw new IllegalStateException("Idle boat retained rowing arms");
            }
            preview(minecraft, boat, player, phases);
            firstPerson(minecraft);
            player.stopRiding();
            if (PufferfishRowingAnimation.isRowing(player)) throw new IllegalStateException("Dismounted player retained rowing pose");
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not exercise paddle phases", exception);
        }
    }

    private static void preview(Minecraft minecraft, org.glavo.kaleidoscope.pufferfish.entity.PufferfishBoat boat,
            RemotePlayer player, float[] phases) {
        boat.setPos(Vec3.ZERO);
        boat.setYRot(0);
        boat.setOldPosAndRot();
        boat.positionRider(player);
        player.setOldPosAndRot();
        player.yBodyRot = player.yBodyRotO = 0;
        boat.setPaddleState(true, true);
        var model = new PlayerModel<RemotePlayer>(minecraft.getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
        model.riding = true;
        model.young = false;
        var target = new TextureTarget(960, 360, true, Minecraft.ON_OSX);
        target.setClearColor(0.16F, 0.18F, 0.21F, 1);
        target.clear(Minecraft.ON_OSX);
        target.bindWrite(true);
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, 960, 360, 0, -1000, 1000), VertexSorting.ORTHOGRAPHIC_Z);
        var view = RenderSystem.getModelViewStack();
        view.pushMatrix().identity();
        RenderSystem.applyModelViewMatrix();
        Lighting.setupForFlatItems();
        try {
            for (int frame = 0; frame < 4; frame++) {
                phases[0] = phases[1] = frame * (float) Math.PI / 4;
                var pose = new PoseStack();
                pose.translate(120 + frame * 240, 245, 0);
                pose.scale(68, -68, 68);
                pose.mulPose(Axis.XP.rotationDegrees(15));
                pose.mulPose(Axis.YP.rotationDegrees(-45));
                var buffers = minecraft.renderBuffers().bufferSource();
                ((PufferfishBoatRenderer) minecraft.getEntityRenderDispatcher().getRenderer(boat)).render(boat, 0, 1, pose, buffers, 15728880);
                pose.translate(player.getX(), player.getY(), player.getZ());
                pose.mulPose(PufferfishRowingAnimation.playerTransform(player, 0));
                model.setupAnim(player, 0, 0, frame, 0, 0);
                PufferfishRowingAnimation.poseArms(model, player, 1, 0);
                model.renderToBuffer(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(player.getSkin().texture())), 15728880, OverlayTexture.NO_OVERLAY);
                buffers.endBatch();
            }
            try (var image = Screenshot.takeScreenshot(target)) { image.writeToFile(Path.of("pufferfish-rowing-preview.png")); }
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Could not save rowing preview", exception);
        } finally {
            view.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.restoreProjectionMatrix();
            target.destroyBuffers();
            minecraft.getMainRenderTarget().bindWrite(true);
        }
    }
    private static void firstPerson(Minecraft minecraft) throws ReflectiveOperationException {
        var oldLevel = minecraft.level;
        var oldPlayer = minecraft.player;
        var level = PufferfishHeadgearClientTest.level(minecraft);
        var connection = net.minecraft.client.multiplayer.ClientLevel.class.getDeclaredField("connection");
        connection.setAccessible(true);
        var player = new net.minecraft.client.player.LocalPlayer(minecraft, level,
                (net.minecraft.client.multiplayer.ClientPacketListener) connection.get(level), new net.minecraft.stats.StatsCounter(),
                new net.minecraft.client.ClientRecipeBook(), false, false) {
            @Override public boolean isSpectator() { return false; }
            @Override protected net.minecraft.client.multiplayer.PlayerInfo getPlayerInfo() { return null; }
        };
        var boat = PufferfishBoats.VARIANTS.get("pufferfish_skin_boat").entity().get().create(level);
        player.startRiding(boat, true);
        boat.positionRider(player);
        player.setOldPosAndRot();
        boat.setOldPosAndRot();
        boat.setPaddleState(true, true);
        minecraft.level = level;
        minecraft.player = player;
        var target = new TextureTarget(640, 360, true, Minecraft.ON_OSX);
        var view = RenderSystem.getModelViewStack();
        view.pushMatrix();
        RenderSystem.backupProjectionMatrix();
        try {
            for (int fov : new int[] {70, 110}) {
                target.setClearColor(0.38F, 0.62F, 0.82F, 1);
                target.clear(Minecraft.ON_OSX);
                target.bindWrite(true);
                var camera = new net.minecraft.client.Camera() {
                    void aim(Vec3 position) { setPosition(position); setRotation(0, 30); }
                };
                camera.aim(player.getEyePosition());
                var rotation = new Matrix4f().rotation(camera.rotation().conjugate(new org.joml.Quaternionf()));
                var projection = new Matrix4f().perspective((float) Math.toRadians(fov), 640F / 360, 0.05F, 256);
                view.set(rotation);
                RenderSystem.applyModelViewMatrix();
                RenderSystem.setProjectionMatrix(projection, VertexSorting.DISTANCE_TO_ORIGIN);
                net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.client.event.RenderLevelStageEvent(
                        net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_LEVEL,
                        minecraft.levelRenderer, null, rotation, projection, 0, net.minecraft.client.DeltaTracker.ZERO, camera, null));
                var buffers = minecraft.renderBuffers().bufferSource();
                var worldPose = new PoseStack();
                var eye = player.getEyePosition();
                worldPose.translate(-eye.x, -eye.y, -eye.z);
                ((PufferfishBoatRenderer) minecraft.getEntityRenderDispatcher().getRenderer(boat)).render(boat, 0, 0, worldPose, buffers, 15728880);
                buffers.endBatch();
                RenderSystem.clear(256, Minecraft.ON_OSX);
                RenderSystem.setProjectionMatrix(new Matrix4f().perspective((float) Math.toRadians(70), 640F / 360, 0.05F, 256), VertexSorting.DISTANCE_TO_ORIGIN);
                var pose = new PoseStack();
                pose.mulPose(new Matrix4f(rotation).invert());
                var event = new net.neoforged.neoforge.client.event.RenderHandEvent(net.minecraft.world.InteractionHand.MAIN_HAND,
                        pose, buffers, 15728880, 0, 30, 0, 0, net.minecraft.world.item.ItemStack.EMPTY);
                net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event);
                if (!event.isCanceled()) throw new IllegalStateException("First-person rowing did not replace the static hand");
                buffers.endBatch();
                try (var image = Screenshot.takeScreenshot(target)) { image.writeToFile(Path.of("pufferfish-rowing-first-person-" + fov + ".png")); }
            }
            boat.setPaddleState(false, false);
            var idle = new net.neoforged.neoforge.client.event.RenderHandEvent(net.minecraft.world.InteractionHand.MAIN_HAND,
                    new PoseStack(), minecraft.renderBuffers().bufferSource(), 15728880, 0, 0, 0, 0, net.minecraft.world.item.ItemStack.EMPTY);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(idle);
            if (idle.isCanceled()) throw new IllegalStateException("Stopped rowing retained custom first-person hands");
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Could not save first-person rowing preview", exception);
        } finally {
            player.stopRiding();
            minecraft.player = oldPlayer;
            minecraft.level = oldLevel;
            view.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.restoreProjectionMatrix();
            target.destroyBuffers();
            minecraft.getMainRenderTarget().bindWrite(true);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.client.event.RenderFrameEvent.Pre(net.minecraft.client.DeltaTracker.ZERO));
        }
    }

}
