// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.glavo.kaleidoscope.pufferfish.entity.PufferfishFlailHead;
import org.glavo.kaleidoscope.pufferfish.PufferfishFlailItem;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/// Renders a held or displayed flail, leaving only its grip while the head is in flight.
public final class PufferfishFlailItemRenderer extends BlockEntityWithoutLevelRenderer implements IClientItemExtensions {
    /// Lazily created geometry, invalidated when resources reload.
    private PufferfishFlailModel model;
    private static Matrix4f bodyPose;

    /// Scopes the player's body transform around the native third-person hand renderer.
    public static Matrix4f setBodyPose(Matrix4f value) {
        var previous = bodyPose;
        bodyPose = value;
        return previous;
    }

    /// Creates a renderer using the current client dispatchers.
    public PufferfishFlailItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public BlockEntityWithoutLevelRenderer getCustomRenderer() { return this; }

    @Override
    public boolean applyForgeHandTransform(PoseStack pose, LocalPlayer player, HumanoidArm arm, ItemStack stack,
            float partialTick, float equip, float swing) {
        if (player.isUsingItem() && player.getUseItem() == stack) {
            float lift = Mth.clamp((player.getTicksUsingItem() + partialTick) / 4, 0, 1);
            pose.translate((arm == HumanoidArm.RIGHT ? 0.12 : -0.12) * lift, 0.08 * lift, -0.05 * lift);
        }
        return false;
    }

    /// Invalidates the baked fish geometry after a resource reload.
    @Override
    public void onResourceManagerReload(ResourceManager manager) { model = null; }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (model == null) model = new PufferfishFlailModel();
        if (context == ItemDisplayContext.GUI) {
            renderIcon(stack, pose, buffers, light, overlay);
            return;
        }
        pose.pushPose();
        boolean held = context.firstPerson() || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
        pose.translate(held ? 0.5 : 0.28, held ? 0.5 : 0.70, 0.5);
        var flying = flyingHead(stack, context);
        model.renderGrip(stack, pose, buffers, light, overlay);
        if (flying != null && context.firstPerson()) {
            PufferfishFlailTether.render(model, flying, pose, buffers, light);
        }
        float age = held && Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.getGameTime() + Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false) : 0;
        double sway = held ? Mth.sin(age * 0.09F) * 0.035 : 0;
        var head = new Vec3(0.47 + sway, -0.24, 0.02);
        if (flying == null) {
            var chargingPlayer = held ? chargingPlayer(stack) : null;
            float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
            float charging = chargingPlayer == null ? -1 : chargingPlayer.getTicksUsingItem() + partialTick;
            var anchor = new Vec3(0, 0.20, 0);
            if (charging >= 0) {
                double angle = PufferfishFlailItem.spinAngle(stack, charging);
                var orbit = chargingOrbit(context, chargingPlayer, partialTick, angle, anchor, pose);
                head = head.lerp(orbit, Mth.clamp(charging / 4, 0, 1));
            }
            var offset = head.subtract(anchor);
            var end = charging >= 0 ? offset.subtract(offset.normalize().scale(0.20)) : offset.add(0, 0.19, 0);
            pose.translate(anchor.x, anchor.y, anchor.z);
            model.renderChain(stack, end, charging >= 0 ? 0 : 0.12, pose, buffers, light);
            pose.translate(offset.x, offset.y, offset.z);
            pose.mulPose(Axis.ZP.rotationDegrees(held ? (float) (-8 + sway * 100) : -8));
            model.renderHead(stack, pose, buffers, light, overlay, age);
        }
        pose.popPose();
    }

    private static Vec3 chargingOrbit(ItemDisplayContext context, Player player, float partialTick, double angle,
            Vec3 anchor, PoseStack pose) {
        boolean right = context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
        if (!context.firstPerson() && bodyPose != null) {
            var itemToBody = new Matrix4f(bodyPose).invert().mul(pose.last().pose());
            var grip = itemToBody.transformPosition(anchor.toVector3f());
            // Player model coordinates have downward Y and negative X on the right side.
            var center = new Vector3f((right ? -1 : 1) * Math.max(0.90F, Math.abs(grip.x) + 0.40F),
                    grip.y + (float) (0.65 * Math.cos(angle)), grip.z + (float) (0.65 * Math.sin(angle)));
            itemToBody.invert().transformPosition(center);
            return new Vec3(center);
        }
        float yaw = (context.firstPerson() ? player.getViewYRot(partialTick)
                : Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot)) * Mth.DEG_TO_RAD;
        double outward = right ? -0.40 : 0.40;
        double forward = 0.65 * Math.sin(angle);
        var worldOffset = new Vector3f((float) (outward * Mth.cos(yaw) - forward * Mth.sin(yaw)),
                (float) (-0.65 * Math.cos(angle)), (float) (outward * Mth.sin(yaw) + forward * Mth.cos(yaw)));
        var viewOffset = RenderSystem.getModelViewMatrix().transformDirection(worldOffset);
        var localOffset = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(pose.last().pose()).invert()
                .transformDirection(viewOffset);
        return anchor.add(localOffset.x, localOffset.y, localOffset.z);
    }

    private static Player chargingPlayer(ItemStack stack) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return null;
        for (var player : minecraft.level.players()) {
            if (player.isUsingItem() && player.getUseItem() == stack) {
                return player;
            }
        }
        return null;
    }

    /// Keeps the face upright and large enough to read in a 16-pixel inventory slot.
    private void renderIcon(ItemStack stack, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        pose.translate(0.20, 0.23, 0.5);
        pose.mulPose(Axis.ZP.rotationDegrees(-35));
        model.renderGrip(stack, pose, buffers, light, overlay);
        pose.popPose();
        pose.pushPose();
        pose.translate(0.332, 0.418, 0.5);
        model.renderChain(stack, new Vec3(0.18, 0.13, 0), 0, pose, buffers, light);
        pose.popPose();
        pose.pushPose();
        pose.translate(0.65, 0.65, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-15));
        pose.mulPose(Axis.XP.rotationDegrees(10));
        pose.scale(1.1F, 1.1F, 1.1F);
        model.renderHead(stack, pose, buffers, light, overlay, 0);
        pose.popPose();
    }

    /// Finds the head belonging to this exact held stack.
    private static PufferfishFlailHead flyingHead(ItemStack stack, ItemDisplayContext context) {
        if (context != ItemDisplayContext.FIRST_PERSON_LEFT_HAND && context != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                && context != ItemDisplayContext.THIRD_PERSON_LEFT_HAND && context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) return null;
        var level = Minecraft.getInstance().level;
        if (level == null) return null;
        for (var player : level.players()) {
            for (var hand : InteractionHand.values()) {
                if (player.getItemInHand(hand) == stack) {
                    return level.getEntitiesOfClass(PufferfishFlailHead.class, player.getBoundingBox().inflate(16),
                            head -> head.getOwner() == player && head.getHand() == hand).stream().findFirst().orElse(null);
                }
            }
        }
        return null;
    }
}
