// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
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
        pose.pushPose();
        pose.translate(0.5, 0.1, 0.5);
        boolean flying = isFlying(stack, context);
        model.renderChain(new Vec3(0, flying ? 0.2 : 0.7, 0), pose, buffers, light);
        if (!flying) {
            pose.translate(0, 0.8, 0);
            model.renderHead(stack, pose, buffers, light, overlay, 0);
        }
        pose.popPose();
    }

    /// Returns whether this exact held stack owns a visible head in the client world.
    private static boolean isFlying(ItemStack stack, ItemDisplayContext context) {
        if (context != ItemDisplayContext.FIRST_PERSON_LEFT_HAND && context != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                && context != ItemDisplayContext.THIRD_PERSON_LEFT_HAND && context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) return false;
        var level = Minecraft.getInstance().level;
        if (level == null) return false;
        for (var player : level.players()) {
            for (var hand : InteractionHand.values()) {
                if (player.getItemInHand(hand) == stack) {
                    return !level.getEntitiesOfClass(PufferfishFlailHead.class, player.getBoundingBox().inflate(16),
                            head -> head.getOwner() == player && head.getHand() == hand).isEmpty();
                }
            }
        }
        return false;
    }
}
