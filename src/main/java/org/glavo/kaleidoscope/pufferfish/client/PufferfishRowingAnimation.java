// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import org.glavo.kaleidoscope.pufferfish.entity.PufferfishBoat;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/// Shares paddle motion and grip positions between the boat and both player views.
public final class PufferfishRowingAnimation {
    private PufferfishRowingAnimation() {}

    public record PaddlePose(float x, float y, float z, float pitch, float yaw) {}

    public static boolean isRowing(Player player) {
        return !player.isSpectator() && !player.isUsingItem() && player.getVehicle() instanceof PufferfishBoat boat
                && boat.getControllingPassenger() == player && (boat.getPaddleState(0) || boat.getPaddleState(1));
    }

    public static PaddlePose paddle(Boat boat, float partialTick) {
        if (!boat.getPaddleState(0) && !boat.getPaddleState(1)) return new PaddlePose(0, -5.4F, 4.5F, 0, Mth.HALF_PI);
        float time = boat.getRowingTime(0, partialTick) + boat.getRowingTime(1, partialTick);
        float seat = PufferfishBoat.FRONT_SEAT * 16;
        return new PaddlePose(seat + 8, -6.4F, 0, Mth.sin(time) * 0.25F, Mth.cos(time) * 0.16F);
    }

    public static Vec3 grip(PufferfishBoat boat, HumanoidArm arm, float partialTick) {
        var value = paddle(boat, partialTick);
        var pose = new PoseStack();
        var position = boat.getPosition(partialTick);
        pose.translate(0, 0.375, 0);
        pose.mulPose(Axis.YP.rotationDegrees(180 - Mth.rotLerp(partialTick, boat.yRotO, boat.getYRot())));
        float hurt = boat.getHurtTime() - partialTick;
        if (hurt > 0) pose.mulPose(Axis.XP.rotationDegrees(Mth.sin(hurt) * hurt
                * Math.max(0, boat.getDamage() - partialTick) / 10 * boat.getHurtDir()));
        float bubble = boat.getBubbleAngle(partialTick);
        if (!Mth.equal(bubble, 0)) pose.mulPose(new Quaternionf().setAngleAxis(bubble * Mth.DEG_TO_RAD, 1, 0, 1));
        pose.scale(-1, -1, 1);
        pose.mulPose(Axis.YP.rotationDegrees(90));
        pose.translate(value.x / 16, value.y / 16, value.z / 16);
        pose.mulPose(Axis.YP.rotation(value.yaw));
        pose.mulPose(Axis.XP.rotation(value.pitch));
        var point = pose.last().pose().transformPosition(new Vector3f(0, 0, arm == HumanoidArm.RIGHT ? -5.5F / 16 : 5.5F / 16));
        return position.add(point.x, point.y, point.z);
    }

    public static Matrix4f playerTransform(Player player, float bodyYaw) {
        float scale = player.getScale() * 0.9375F;
        return new Matrix4f().rotationY((180 - bodyYaw) * Mth.DEG_TO_RAD)
                .scale(-scale, -scale, scale).translate(0, -1.501F, 0);
    }

    /// Keeps both passengers' legs parallel to the narrow kayak, including while idle or using items.
    public static void poseLegs(HumanoidModel<?> model, Player player) {
        if (!model.riding || !(player.getVehicle() instanceof PufferfishBoat)) return;
        model.rightLeg.xRot = model.leftLeg.xRot = -Mth.HALF_PI;
        model.rightLeg.yRot = model.leftLeg.yRot = 0;
        model.rightLeg.zRot = model.leftLeg.zRot = 0;
        if (model instanceof PlayerModel<?> playerModel) {
            playerModel.rightPants.copyFrom(model.rightLeg);
            playerModel.leftPants.copyFrom(model.leftLeg);
        }
    }

    public static boolean poseArms(HumanoidModel<?> model, Player player, float partialTick, float bodyYaw) {
        if (!isRowing(player)) return false;
        var inverse = playerTransform(player, bodyYaw).invert();
        for (var arm : HumanoidArm.values()) {
            var part = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
            var target = grip((PufferfishBoat) player.getVehicle(), arm, partialTick).subtract(player.getPosition(partialTick));
            var local = inverse.transformPosition(new Vector3f((float) target.x, (float) target.y, (float) target.z)).mul(16);
            var direction = local.sub(part.x, part.y, part.z);
            var palm = new Vector3f(arm == HumanoidArm.RIGHT ? -1 : 1, 9, 0);
            var rotation = new Quaternionf().rotationTo(palm.normalize(), direction.normalize()).getEulerAnglesZYX(new Vector3f());
            part.xRot = rotation.x;
            part.yRot = rotation.y;
            part.zRot = rotation.z;
            if (model instanceof PlayerModel<?> playerModel) {
                (arm == HumanoidArm.RIGHT ? playerModel.rightSleeve : playerModel.leftSleeve).copyFrom(part);
            }
        }
        return true;
    }

    public static void renderHands(RenderHandEvent event) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || !isRowing(player)) return;
        if (event.getHand() == InteractionHand.OFF_HAND) {
            event.setCanceled(true);
            return;
        }
        var pose = event.getPoseStack();
        pose.pushPose();
        try {
            if (!PufferfishFlailTether.transformToView(pose)) return;
            float partial = PufferfishFlailTether.partialTick(event.getPartialTick());
            var model = ((PlayerRenderer) minecraft.getEntityRenderDispatcher().getRenderer(player)).getModel();
            for (var arm : HumanoidArm.values()) {
                var target = PufferfishFlailTether.localPosition(grip((PufferfishBoat) player.getVehicle(), arm, partial), pose);
                if (target == null) continue;
                var part = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
                var sleeve = arm == HumanoidArm.RIGHT ? model.rightSleeve : model.leftSleeve;
                var oldArm = part.storePose();
                var oldSleeve = sleeve.storePose();
                boolean armVisible = part.visible;
                boolean sleeveVisible = sleeve.visible;
                pose.pushPose();
                try {
                    var shoulder = new Vector3f(arm == HumanoidArm.RIGHT ? 0.38F : -0.38F, -0.65F, -0.45F);
                    var direction = new Vector3f((float) target.x, (float) target.y, (float) target.z).sub(shoulder);
                    var palm = new Vector3f(arm == HumanoidArm.RIGHT ? -1 : 1, 9, 0);
                    float length = direction.length() * 16;
                    float reach = Mth.sqrt(Math.max(0.001F, length * length - 0.65F * 0.65F)) / 9;
                    pose.translate(shoulder.x, shoulder.y, shoulder.z);
                    pose.mulPose(new Quaternionf().rotationTo(palm.mul(0.65F, reach, 0.65F).normalize(), new Vector3f(direction).normalize()));
                    pose.scale(0.65F, reach, 0.65F);
                    part.loadPose(part.getInitialPose());
                    part.setPos(0, 0, 0);
                    sleeve.copyFrom(part);
                    part.visible = sleeve.visible = true;
                    if (!player.isInvisible()) {
                        var buffer = event.getMultiBufferSource().getBuffer(RenderType.entityTranslucent(player.getSkin().texture()));
                        part.render(pose, buffer, event.getPackedLight(), OverlayTexture.NO_OVERLAY);
                        if (player.isModelPartShown(arm == HumanoidArm.RIGHT ? PlayerModelPart.RIGHT_SLEEVE : PlayerModelPart.LEFT_SLEEVE)) {
                            sleeve.render(pose, buffer, event.getPackedLight(), OverlayTexture.NO_OVERLAY);
                        }
                    }
                } finally {
                    part.loadPose(oldArm);
                    sleeve.loadPose(oldSleeve);
                    part.visible = armVisible;
                    sleeve.visible = sleeveVisible;
                    pose.popPose();
                }
            }
            event.setCanceled(true);
        } finally {
            pose.popPose();
        }
    }
}
