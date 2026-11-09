// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
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
            pose.translate(0, 0.23, 0);
            model.renderChain(stack, head.add(0, -0.23 + 0.19, 0), 0.12, pose, buffers, light);
            pose.translate(head.x, head.y - 0.23, head.z);
            pose.mulPose(Axis.ZP.rotationDegrees(held ? (float) (-8 + sway * 100) : -8));
            model.renderHead(stack, pose, buffers, light, overlay, age);
        }
        pose.popPose();
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
