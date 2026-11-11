// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.math.Axis;
import java.nio.file.Path;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.phys.Vec3;
import org.glavo.mcmod.pufferfish.client.PufferfishBoatRenderer;
import org.glavo.mcmod.pufferfish.client.PufferfishRowingAnimation;
import org.glavo.mcmod.pufferfish.entity.PufferfishBoat;
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
                                if (frame == 0) verifySeating(minecraft, model, player, boat, yaw);
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
                        if (tandem) {
                            boat.positionRider(passenger);
                            passenger.setOldPosAndRot();
                            passenger.yBodyRot = passenger.yBodyRotO = yaw;
                            model.setupAnim(passenger, 0, 0, 0, 0, 0);
                            verifySeating(minecraft, model, passenger, boat, yaw);
                        }
                    }
                    if (PufferfishRowingAnimation.isRowing(passenger)) throw new IllegalStateException("Rear passenger was rowing");
                    passenger.stopRiding();
                }
                boat.setPaddleState(false, false);
                if (PufferfishRowingAnimation.poseArms(model, player, 1, 0)) throw new IllegalStateException("Idle boat retained rowing arms");
                model.setupAnim(player, 0, 0, 0, 0, 0);
                verifySeating(minecraft, model, player, boat, boat.getYRot());
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BOW));
                player.startUsingItem(net.minecraft.world.InteractionHand.MAIN_HAND);
                model.setupAnim(player, 0, 0, 0, 0, 0);
                verifySeating(minecraft, model, player, boat, boat.getYRot());
                player.stopUsingItem();
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
            }
            preview(minecraft, boat, player, phases);
            seatingPreview(minecraft, boat, player, passenger);
            firstPerson(minecraft);
            player.stopRiding();
            if (PufferfishRowingAnimation.isRowing(player)) throw new IllegalStateException("Dismounted player retained rowing pose");
            var model = new PlayerModel<RemotePlayer>(minecraft.getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
            model.setupAnim(player, 0, 0, 0, 0, 0);
            if (Math.abs(model.leftLeg.xRot) > 0.01) throw new IllegalStateException("Dismounted player retained kayak legs");
            player.startRiding(new Boat(level, 0, 0, 0), true);
            model.riding = true;
            model.setupAnim(player, 0, 0, 0, 0, 0);
            if (model.leftLeg.yRot >= -0.3F) throw new IllegalStateException("Kayak pose changed vanilla boats");
            player.stopRiding();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not exercise paddle phases", exception);
        }
    }

    private static void verifySeating(Minecraft minecraft, PlayerModel<RemotePlayer> model, RemotePlayer player,
            PufferfishBoat boat, float yaw) {
        var offset = player.position().subtract(boat.position());
        var pose = new PoseStack();
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.translate(offset.x, offset.y, offset.z);
        pose.mulPose(PufferfishRowingAnimation.playerTransform(player, yaw));
        var hull = new java.util.ArrayList<Vector3f>();
        new org.glavo.mcmod.pufferfish.client.PufferfishBoatModel().renderToBuffer(new PoseStack(),
                new Vertices(hull::add), 15728880, OverlayTexture.NO_OVERLAY, -1);
        var deck = new java.util.ArrayList<Vector3f>();
        for (int i = 0; i < hull.size(); i += 4) {
            var quad = hull.subList(i, i + 4);
            if (quad.stream().allMatch(v -> v.x >= 10F / 16 && v.x <= 1 && v.y < 0)) deck.addAll(quad);
        }
        float[] clearance = {Float.POSITIVE_INFINITY};
        var vertices = new Vertices(point -> {
            if (point.y < 0.25F - 0.001F || Math.abs(point.x) > 5F / 16 || point.z > 1) {
                throw new IllegalStateException("Kayak passenger leg escaped the footwell: " + point);
            }
            if (point.z > 10F / 16) clearance[0] = Math.min(clearance[0], deckClearance(point, deck));
            if (boat.getControllingPassenger() != player && point.z > 0.7F / 16) {
                throw new IllegalStateException("Rear passenger foot intersected the front backrest: " + point);
            }
        });
        for (var part : new net.minecraft.client.model.geom.ModelPart[] {model.leftLeg, model.rightLeg, model.leftPants, model.rightPants}) {
            part.render(pose, vertices, 15728880, OverlayTexture.NO_OVERLAY);
        }
        var armor = new HumanoidModel<RemotePlayer>(minecraft.getEntityModels().bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR));
        model.copyPropertiesTo(armor);
        armor.leftLeg.render(pose, vertices, 15728880, OverlayTexture.NO_OVERLAY);
        armor.rightLeg.render(pose, vertices, 15728880, OverlayTexture.NO_OVERLAY);
        if (clearance[0] < 0.015F) {
            throw new IllegalStateException("Kayak leg intersects the sloped deck: clearance=" + clearance[0]);
        }
    }

    private static float deckClearance(Vector3f point, java.util.List<Vector3f> deck) {
        boolean covered = false;
        float clearance = Float.POSITIVE_INFINITY;
        for (int i = 0; i < deck.size(); i += 4) for (int corner = 1; corner <= 2; corner++) {
            var a = deck.get(i);
            var b = deck.get(i + corner);
            var c = deck.get(i + corner + 1);
            float determinant = (b.z - c.z) * (a.x - c.x) + (c.x - b.x) * (a.z - c.z);
            if (Math.abs(determinant) < 1.0E-6F) continue;
            float u = ((b.z - c.z) * (point.z - c.x) + (c.x - b.x) * (point.x - c.z)) / determinant;
            float v = ((c.z - a.z) * (point.z - c.x) + (a.x - c.x) * (point.x - c.z)) / determinant;
            if (u < -0.0001F || v < -0.0001F || u + v > 1.0001F) continue;
            covered = true;
            float roof = 0.375F - (u * a.y + v * b.y + (1 - u - v) * c.y);
            clearance = Math.min(clearance, roof - point.y);
        }
        if (!covered) throw new IllegalStateException("Kayak foot extends beyond the covered footwell");
        return clearance;
    }

    private record Vertices(Consumer<Vector3f> check) implements VertexConsumer {
        @Override public VertexConsumer addVertex(float x, float y, float z) { check.accept(new Vector3f(x, y, z)); return this; }
        @Override public VertexConsumer setColor(int r, int g, int b, int a) { return this; }
        @Override public VertexConsumer setUv(float u, float v) { return this; }
        @Override public VertexConsumer setUv1(int u, int v) { return this; }
        @Override public VertexConsumer setUv2(int u, int v) { return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { return this; }
    }

    private static void seatingPreview(Minecraft minecraft, PufferfishBoat boat, RemotePlayer player, RemotePlayer passenger) {
        boat.setPos(Vec3.ZERO);
        boat.setYRot(0);
        boat.setOldPosAndRot();
        boat.setPaddleState(false, false);
        var model = new PlayerModel<RemotePlayer>(minecraft.getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
        model.riding = true;
        model.young = false;
        var armor = new HumanoidModel<RemotePlayer>(minecraft.getEntityModels().bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR));
        var target = new TextureTarget(1200, 660, true, Minecraft.ON_OSX);
        target.setClearColor(0.16F, 0.18F, 0.21F, 1);
        target.clear(Minecraft.ON_OSX);
        target.bindWrite(true);
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, 1200, 660, 0, -1000, 1000), VertexSorting.ORTHOGRAPHIC_Z);
        var view = RenderSystem.getModelViewStack();
        view.pushMatrix().identity();
        RenderSystem.applyModelViewMatrix();
        Lighting.setupForFlatItems();
        try {
            for (int row = 0; row < 2; row++) {
                if (row == 1) passenger.startRiding(boat, true);
                for (int column = 0; column < 3; column++) {
                    var pose = new PoseStack();
                    pose.translate(200 + column * 400, 265 + row * 330, 0);
                    pose.scale(105, -105, 105);
                    pose.mulPose(Axis.XP.rotationDegrees(column == 0 ? 0 : column == 1 ? 20 : 65));
                    pose.mulPose(Axis.YP.rotationDegrees(column == 0 ? -90 : -35));
                    var buffers = minecraft.renderBuffers().bufferSource();
                    ((PufferfishBoatRenderer) minecraft.getEntityRenderDispatcher().getRenderer(boat)).render(boat, 0, 1, pose, buffers, 15728880);
                    for (var rider : boat.getPassengers()) {
                        var seated = (RemotePlayer) rider;
                        boat.positionRider(seated);
                        seated.setOldPosAndRot();
                        seated.yBodyRot = seated.yBodyRotO = 0;
                        model.setupAnim(seated, 0, 0, 0, 0, 0);
                        pose.pushPose();
                        pose.translate(seated.getX(), seated.getY(), seated.getZ());
                        pose.mulPose(PufferfishRowingAnimation.playerTransform(seated, 0));
                        model.renderToBuffer(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(seated.getSkin().texture())), 15728880, OverlayTexture.NO_OVERLAY);
                        if (column == 2) {
                            model.copyPropertiesTo(armor);
                            armor.setAllVisible(false);
                            armor.leftLeg.visible = armor.rightLeg.visible = true;
                            armor.renderToBuffer(pose, buffers.getBuffer(RenderType.armorCutoutNoCull(
                                    net.minecraft.resources.ResourceLocation.withDefaultNamespace("textures/models/armor/iron_layer_1.png"))), 15728880, OverlayTexture.NO_OVERLAY);
                        }
                        pose.popPose();
                    }
                    buffers.endBatch();
                }
            }
            try (var image = Screenshot.takeScreenshot(target)) { image.writeToFile(Path.of("pufferfish-kayak-seating.png")); }
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Could not save kayak seating preview", exception);
        } finally {
            passenger.stopRiding();
            view.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.restoreProjectionMatrix();
            target.destroyBuffers();
            minecraft.getMainRenderTarget().bindWrite(true);
        }
    }

    private static void preview(Minecraft minecraft, org.glavo.mcmod.pufferfish.entity.PufferfishBoat boat,
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
