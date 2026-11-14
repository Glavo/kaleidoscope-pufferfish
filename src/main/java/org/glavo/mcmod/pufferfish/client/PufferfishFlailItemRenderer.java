// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.client;

import com.mojang.blaze3d.vertex.PoseStack;
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
import org.glavo.mcmod.pufferfish.entity.PufferfishFlailHead;
import org.glavo.mcmod.pufferfish.PufferfishFlailItem;
import org.joml.Vector3f;
import org.joml.Quaternionf;
import org.glavo.mcmod.pufferfish.PufferfishFlailMotion;

/// Renders a held or displayed flail, leaving only its grip while the head is in flight.
public final class PufferfishFlailItemRenderer extends BlockEntityWithoutLevelRenderer implements IClientItemExtensions {
    /// Lazily created geometry, invalidated when resources reload.
    private PufferfishFlailModel model;
    /// Creates a renderer using the current client dispatchers.
    public PufferfishFlailItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public BlockEntityWithoutLevelRenderer getCustomRenderer() { return this; }

    @Override
    public boolean applyForgeHandTransform(PoseStack pose, LocalPlayer player, HumanoidArm arm, ItemStack stack,
            float partialTick, float equip, float swing) {
        var head = PufferfishFlailItem.activeHead(player);
        if ((player.isUsingItem() && player.getUseItem() == stack) || head != null) {
            float lift = head != null ? 1 : Mth.clamp((player.getTicksUsingItem() + partialTick) / 4, 0, 1);
            pose.translate((arm == HumanoidArm.RIGHT ? 0.12 : -0.12) * lift, 0.08 * lift, -0.05 * lift);
        }
        return false;
    }

    /// Invalidates the baked fish geometry after a resource reload.
    @Override
    public void onResourceManagerReload(ResourceManager manager) { model = null; PufferfishFlailArmAnimation.reset(); }

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
        float partialTick = PufferfishFlailTether.partialTick(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
        var player = flying != null && flying.getOwner() instanceof Player owner ? owner : held ? chargingPlayer(stack) : null;
        Vec3 target = null;
        if (player != null) {
            var world = flying != null ? flying.renderPosition(partialTick)
                    : PufferfishFlailMotion.orbit(player, player.getUsedItemHand(), stack,
                    player.getTicksUsingItem() + partialTick, player.getViewYRot(partialTick), partialTick);
            target = PufferfishFlailTether.localPosition(world, pose);
        }
        boolean armSwing = player != null && PufferfishFlailArmAnimation.activeHand(player) != null;
        if (target != null && (armSwing && !context.firstPerson() || flying != null && flying.isShortCast())) {
            if (armSwing && !context.firstPerson()) pose.mulPose(Axis.ZP.rotationDegrees(-90));
            var world = flying != null ? flying.renderPosition(partialTick)
                    : PufferfishFlailMotion.orbit(player, player.getUsedItemHand(), stack,
                    player.getTicksUsingItem() + partialTick, player.getViewYRot(partialTick), partialTick);
            renderFixedGrip(model, stack, world, flying == null, pose, buffers, light, overlay, player.tickCount + partialTick);
        } else if (target != null) {
            var direction = target.normalize();
            pose.pushPose();
            pose.mulPose(new Quaternionf().rotationTo(new Vector3f(0, 1, 0), direction.toVector3f()));
            model.renderGrip(stack, pose, buffers, light, overlay);
            pose.popPose();
            var anchor = direction.scale(0.20);
            var end = target.subtract(anchor);
            pose.translate(anchor.x, anchor.y, anchor.z);
            model.renderChain(stack, end.subtract(direction.scale(Math.min(0.20, end.length()))), 0, pose, buffers, light);
            if (flying == null) {
                pose.translate(end.x, end.y, end.z);
                model.renderHead(stack, pose, buffers, light, overlay, player.tickCount + partialTick);
            }
        } else {
            model.renderGrip(stack, pose, buffers, light, overlay);
            if (flying == null) {
                float age = held && Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() + partialTick : 0;
                double sway = held ? Mth.sin(age * 0.09F) * 0.035 : 0;
                var offset = new Vec3(0.47 + sway, -0.44, 0.02);
                pose.translate(0, 0.20, 0);
                model.renderChain(stack, offset.add(0, 0.19, 0), 0.12, pose, buffers, light);
                pose.translate(offset.x, offset.y, offset.z);
                pose.mulPose(Axis.ZP.rotationDegrees((float) (-8 + sway * 100)));
                model.renderHead(stack, pose, buffers, light, overlay, age);
            }
        }
        pose.popPose();
    }

    /// Keeps the handle fixed in the palm while the tether follows the world head.
    public static void renderFixedGrip(PufferfishFlailModel model, ItemStack stack, Vec3 worldHead, boolean drawHead,
            PoseStack pose, MultiBufferSource buffers, int light, int overlay, float age) {
        model.renderGrip(stack, pose, buffers, light, overlay);
        var target = PufferfishFlailTether.localPosition(worldHead, pose);
        if (target == null) return;
        var offset = target.subtract(0, 0.20, 0);
        pose.pushPose();
        pose.translate(0, 0.20, 0);
        model.renderChain(stack, offset.subtract(offset.normalize().scale(Math.min(0.20, offset.length()))), 0, pose, buffers, light);
        if (drawHead) {
            pose.translate(offset.x, offset.y, offset.z);
            model.renderHead(stack, pose, buffers, light, overlay, age);
        }
        pose.popPose();
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
        pose.translate(0.18, 0.30, 0.5);
        pose.mulPose(Axis.ZP.rotationDegrees(-15));
        model.renderGrip(stack, pose, buffers, light, overlay);
        pose.popPose();
        pose.pushPose();
        pose.translate(0.232, 0.493, 0.5);
        var chain = new Vec3(0.258, 0.107, 0);
        var direction = chain.normalize();
        // Show both link planes and keep them legible at native inventory resolution.
        pose.mulPose(new Quaternionf().rotationAxis((float) Math.PI / 4,
                (float) direction.x, (float) direction.y, (float) direction.z));
        pose.scale(1.75F, 1.75F, 1.75F);
        model.renderChain(stack, chain.scale(1 / 1.75), 0, pose, buffers, light);
        pose.popPose();
        pose.pushPose();
        pose.translate(0.70, 0.68, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-22));
        pose.mulPose(Axis.XP.rotationDegrees(10));
        pose.scale(0.92F, 0.92F, 0.92F);
        model.renderHead(stack, pose, buffers, light, overlay, 0, 0, 1);
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
