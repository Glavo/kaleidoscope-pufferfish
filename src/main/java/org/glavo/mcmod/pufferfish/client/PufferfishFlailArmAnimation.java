// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import org.glavo.mcmod.pufferfish.PufferfishFlailItem;
import org.glavo.mcmod.pufferfish.PufferfishItems;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/// Moves the casting arm about its shoulder; the grip stays fixed relative to the palm.
public final class PufferfishFlailArmAnimation {
    private static PufferfishFlailModel weaponModel;

    private PufferfishFlailArmAnimation() {}

    public static void reset() { weaponModel = null; }

    public static InteractionHand activeHand(Player player) {
        var head = PufferfishFlailItem.activeHead(player);
        if (head != null && head.hasSwing()) return head.getHand();
        return player.isUsingItem() && PufferfishItems.isFlail(player.getUseItem()) ? player.getUsedItemHand() : null;
    }

    public static void poseArm(HumanoidModel<?> model, Player player, float partialTick, float bodyYaw) {
        var hand = activeHand(player);
        if (hand == null) return;
        var head = PufferfishFlailItem.activeHead(player);
        var arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        float angle = head != null ? head.armAngle(partialTick)
                : (float) PufferfishFlailItem.spinAngle(player.getUseItem(), player.getTicksUsingItem() + partialTick);
        float yaw = head != null ? head.swingYaw(partialTick) : player.getViewYRot(partialTick);
        var rotation = new Quaternionf().rotationY((yaw - bodyYaw) * Mth.DEG_TO_RAD)
                .rotateX(angle).rotateZ(arm == HumanoidArm.RIGHT ? 0.45F : -0.45F).getEulerAnglesZYX(new Vector3f());
        var part = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
        part.xRot = rotation.x;
        part.yRot = rotation.y;
        part.zRot = rotation.z;
        if (model instanceof PlayerModel<?> playerModel) {
            (arm == HumanoidArm.RIGHT ? playerModel.rightSleeve : playerModel.leftSleeve).copyFrom(part);
        }
    }

    public static void renderHand(RenderHandEvent event) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || activeHand(player) != event.getHand() || !PufferfishItems.isFlail(event.getItemStack())) return;
        if (!(minecraft.getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer)) return;
        var pose = event.getPoseStack();
        pose.pushPose();
        if (!PufferfishFlailTether.transformToView(pose)) {
            pose.popPose();
            return;
        }
        float partialTick = PufferfishFlailTether.partialTick(event.getPartialTick());
        var head = PufferfishFlailItem.activeHead(player);
        float yaw = head != null ? head.swingYaw(partialTick) : player.getViewYRot(partialTick);
        var model = renderer.getModel();
        var arm = event.getHand() == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        pose.translate(arm == HumanoidArm.RIGHT ? 0.32 : -0.32, -0.28, 0.22);
        pose.scale(-0.9375F, -0.9375F, 0.9375F);
        var part = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
        var sleeve = arm == HumanoidArm.RIGHT ? model.rightSleeve : model.leftSleeve;
        var previousArm = part.storePose();
        var previousSleeve = sleeve.storePose();
        try {
            part.loadPose(part.getInitialPose());
            part.setPos(0, 0, 0);
            float angle = head != null ? head.armAngle(partialTick)
                    : (float) PufferfishFlailItem.spinAngle(player.getUseItem(), player.getTicksUsingItem() + partialTick);
            var rotation = new Quaternionf().rotationX(angle).rotateZ(arm == HumanoidArm.RIGHT ? 0.10F : -0.10F)
                    .getEulerAnglesZYX(new Vector3f());
            part.xRot = rotation.x;
            part.yRot = rotation.y;
            part.zRot = rotation.z;
            sleeve.copyFrom(part);
            var buffers = event.getMultiBufferSource();
            int light = event.getPackedLight();
            if (!player.isInvisible()) {
                var skin = buffers.getBuffer(RenderType.entityTranslucent(player.getSkin().texture()));
                part.render(pose, skin, light, OverlayTexture.NO_OVERLAY);
                if (player.isModelPartShown(arm == HumanoidArm.RIGHT ? PlayerModelPart.RIGHT_SLEEVE : PlayerModelPart.LEFT_SLEEVE)) {
                    sleeve.render(pose, skin, light, OverlayTexture.NO_OVERLAY);
                }
            }
            model.translateToHand(arm, pose);
            pose.translate(arm == HumanoidArm.RIGHT ? -0.0625 : 0.0625, 0.625, -0.125);
            pose.scale(0.8F, 0.8F, 0.8F);
            if (weaponModel == null) weaponModel = new PufferfishFlailModel();
            var worldHead = head != null ? head.renderPosition(partialTick)
                    : org.glavo.mcmod.pufferfish.PufferfishFlailMotion.orbit(player, event.getHand(), event.getItemStack(),
                    player.getTicksUsingItem() + partialTick, yaw, partialTick);
            PufferfishFlailItemRenderer.renderFixedGrip(weaponModel, event.getItemStack(), worldHead, head == null,
                    pose, buffers, light, OverlayTexture.NO_OVERLAY, player.tickCount + partialTick);
            event.setCanceled(true);
        } finally {
            part.loadPose(previousArm);
            sleeve.loadPose(previousSleeve);
            pose.popPose();
        }
    }
}
