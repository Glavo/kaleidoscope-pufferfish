// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexSorting;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.glavo.mcmod.pufferfish.client.PufferfishFlailHeadRenderer;
import org.glavo.mcmod.pufferfish.client.PufferfishTextures;
import org.glavo.mcmod.pufferfish.entity.PufferfishFlailHead;
import org.glavo.mcmod.pufferfish.test.ClientTestLevel;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/// Checks the complete third-person orbit against the body and native player hand layer.
public final class PufferfishFlailOrbitClientTest {
    private static final int LIGHT = 15728880;

    private PufferfishFlailOrbitClientTest() {}

    public static void verify(Minecraft minecraft) {
        var oldLevel = minecraft.level;
        var level = ClientTestLevel.level(minecraft);
        var player = new RemotePlayer(level, new GameProfile(UUID.randomUUID(), "flail-orbit-test")) {
            @Override
            protected PlayerInfo getPlayerInfo() { return null; }
            @Override
            public boolean isUsingItem() { return !getUseItem().isEmpty(); }
        };
        player.setPos(0, 0, 0);
        player.setOldPosAndRot();
        minecraft.level = level;
        level.addEntity(player);
        var context = new EntityRendererProvider.Context(minecraft.getEntityRenderDispatcher(), minecraft.getItemRenderer(),
                minecraft.getBlockRenderer(), minecraft.getEntityRenderDispatcher().getItemInHandRenderer(),
                minecraft.getResourceManager(), minecraft.getEntityModels(), minecraft.font);
        var headRenderer = new PufferfishFlailHeadRenderer(context);
        var armor = new HumanoidModel<LivingEntity>(minecraft.getEntityModels().bakeLayer(net.minecraft.client.model.geom.ModelLayers.PLAYER_INNER_ARMOR));
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f(), VertexSorting.ORTHOGRAPHIC_Z);
        var view = RenderSystem.getModelViewStack();
        view.pushMatrix().identity();
        RenderSystem.applyModelViewMatrix();
        captureWorld(minecraft, new Matrix4f());
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
                        var head = new PufferfishFlailHead(level, player, hand);
                        head.beginCharging();
                        level.addEntity(head);
                        model.rightArmPose = usedArm == HumanoidArm.RIGHT ? HumanoidModel.ArmPose.ITEM : HumanoidModel.ArmPose.EMPTY;
                        model.leftArmPose = usedArm == HumanoidArm.LEFT ? HumanoidModel.ArmPose.ITEM : HumanoidModel.ArmPose.EMPTY;
                        for (boolean crouching : new boolean[] {false, true}) {
                            player.setPose(crouching ? Pose.CROUCHING : Pose.STANDING);
                            model.crouching = crouching;
                            var min = new Vector3f(Float.POSITIVE_INFINITY);
                            var max = new Vector3f(Float.NEGATIVE_INFINITY);
                            var wristMin = new Vector3f(Float.POSITIVE_INFINITY);
                            var wristMax = new Vector3f(Float.NEGATIVE_INFINITY);
                            int duration = PufferfishFlailItem.chargeTicks(stack);
                            for (int ticks = 4; ticks <= duration + 24; ticks++) {
                                remaining.setInt(player, stack.getUseDuration(player) - ticks);
                                model.setupAnim(player, 1.2F, 0.6F, ticks, 35, 45);
                                var capture = new Capture();
                                var pose = bodyPose(player);
                                layer.render(pose, capture, LIGHT, player, 0, 0, 0, ticks, 0, 0);
                                var worldPose = new PoseStack();
                                renderHead(headRenderer, head, worldPose, capture);
                                var armPose = bodyPose(player);
                                var part = usedArm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
                                model.translateToHand(usedArm, armPose);
                                var wrist = armPose.last().pose().transformPosition(new Vector3f(usedArm == HumanoidArm.RIGHT ? -0.0625F : 0.0625F, 0.625F, -0.125F));
                                var direction = armPose.last().pose().transformDirection(new Vector3f(0, 1, 0)).normalize();
                                capture.verifyTether(head.renderPosition(0), wrist, direction);
                                wristMin.min(wrist);
                                wristMax.max(wrist);
                                armor.crouching = crouching;
                                armor.setupAnim(player, 1.2F, 0.6F, ticks, 35, 45);
                                var armorArm = usedArm == HumanoidArm.RIGHT ? armor.rightArm : armor.leftArm;
                                if (armorArm.xRot != part.xRot || armorArm.yRot != part.yRot || armorArm.zRot != part.zRot) {
                                    throw new IllegalStateException("Armor did not follow the flail arm");
                                }
                                var sleeve = usedArm == HumanoidArm.RIGHT ? model.rightSleeve : model.leftSleeve;
                                if (sleeve.xRot != part.xRot || sleeve.yRot != part.yRot || sleeve.zRot != part.zRot) {
                                    throw new IllegalStateException("Flail sleeve did not follow the rotating arm");
                                }
                                if (capture.head.isEmpty() || !worldPose.clear()) throw new IllegalStateException("Missing third-person charging head");
                                var center = new Vector3f();
                                for (var vertex : capture.head) {
                                    if (side * vertex.x < 0.50F) throw new IllegalStateException("Charging flail crossed the player's body: vertex=" + vertex + ", side=" + side + ", arm=" + arm + ", hand=" + hand + ", ticks=" + ticks + ", position=" + head.renderPosition(0) + ", yaw=" + player.getYRot() + ", size=" + capture.head.size());
                                    center.add(vertex);
                                }
                                center.div(capture.head.size());
                                min.min(center);
                                max.max(center);
                            }
                            if (wristMax.y - wristMin.y < 0.90F || wristMax.z - wristMin.z < 0.90F) {
                                throw new IllegalStateException("Flail grip rotated in place instead of orbiting the shoulder");
                            }
                            if (max.x - min.x > 0.03F || max.y - min.y < 1.0F || max.z - min.z < 1.0F) {
                                throw new IllegalStateException("Third-person flail did not orbit vertically beside the body");
                            }
                        }
                        head.discard();
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
            view.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.restoreProjectionMatrix();
            NeoForge.EVENT_BUS.post(new RenderFrameEvent.Pre(DeltaTracker.ZERO));
        }
    }

    private static PoseStack bodyPose(AbstractClientPlayer player) {
        var pose = new PoseStack();
        float scale = player.getScale();
        pose.translate(0, player.isCrouching() ? -0.125 * scale : 0, 0);
        pose.scale(0.9375F * scale, -0.9375F * scale, -0.9375F * scale);
        pose.translate(0, -1.501, 0);
        return pose;
    }

    private static void captureWorld(Minecraft minecraft, Matrix4f view) {
        var camera = new Camera() { { setPosition(Vec3.ZERO); } };
        NeoForge.EVENT_BUS.post(new RenderLevelStageEvent(RenderLevelStageEvent.Stage.AFTER_SKY,
                minecraft.levelRenderer, null, view, RenderSystem.getProjectionMatrix(), 0, DeltaTracker.ZERO, camera, null));
    }

    private static void renderHead(PufferfishFlailHeadRenderer renderer, PufferfishFlailHead head, PoseStack pose, MultiBufferSource buffers) {
        pose.pushPose();
        var position = head.getPosition(0);
        pose.translate(position.x, position.y, position.z);
        renderer.render(head, 0, 0, pose, buffers, LIGHT);
        pose.popPose();
    }

    private static final class Capture implements MultiBufferSource, VertexConsumer {
        private final List<Vector3f> head = new ArrayList<>();
        private boolean recording;
        private final List<List<Vector3f>> groups = new ArrayList<>();
        private List<Vector3f> current;
        @Override
        public VertexConsumer getBuffer(RenderType type) {
            current = new ArrayList<>();
            groups.add(current);
            recording = type == RenderType.entityCutoutNoCull(PufferfishTextures.netheriteEntity());
            return this;
        }
        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            var vertex = new Vector3f(x, y, z);
            current.add(vertex);
            if (recording) head.add(vertex);
            return this;
        }

        void verifyTether(Vec3 position, Vector3f wrist, Vector3f direction) {
            if (groups.size() != 4) throw new IllegalStateException("Third-person swing lost its grip, tether, or head");
            var bands = groups.get(1);
            var chain = groups.get(2);
            var bottom = new Vector3f();
            var top = new Vector3f();
            var first = new Vector3f();
            var last = new Vector3f();
            for (int i = 0; i < 24; i++) { bottom.add(bands.get(i)); top.add(bands.get(i + 24)); }
            for (int i = 0; i < 96; i++) { first.add(chain.get(i)); last.add(chain.get(chain.size() - 96 + i)); }
            first.div(96);
            last.div(96);
            if (last.distance(position.toVector3f()) > 0.30F || top.div(24).distance(first) > 0.13F) {
                throw new IllegalStateException("Third-person chain detached from the grip or world head: head=" + position + ", end=" + last + ", grip=" + top + ", start=" + first + ", groups=" + groups.stream().map(List::size).toList());
            }
            var center = new Vector3f();
            for (var vertex : groups.getFirst()) center.add(vertex);
            center.div(groups.getFirst().size());
            if (center.distance(wrist) > 0.04F || top.sub(bottom.div(24)).normalize().dot(direction) < 0.995F) {
                throw new IllegalStateException("Grip moved relative to the palm: center=" + center + ", wrist=" + wrist + ", axis=" + top + ", arm=" + direction);
            }
        }
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
