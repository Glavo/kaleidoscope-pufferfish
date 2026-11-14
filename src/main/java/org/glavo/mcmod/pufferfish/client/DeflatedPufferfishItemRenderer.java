// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.glavo.mcmod.pufferfish.PufferfishDurability;
import org.glavo.mcmod.pufferfish.PufferfishItems;

/// Shows the same puff state in inventory, held items, dropped items, and projectiles.
public final class DeflatedPufferfishItemRenderer extends BlockEntityWithoutLevelRenderer implements IClientItemExtensions {
    private PufferfishFlailModel model;

    public DeflatedPufferfishItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override public BlockEntityWithoutLevelRenderer getCustomRenderer() { return this; }
    @Override public void onResourceManagerReload(ResourceManager manager) { model = null; }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (model == null) model = new PufferfishFlailModel();
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        float scale = 1.25F;
        if (context == ItemDisplayContext.GUI) {
            scale = switch (PufferfishDurability.deflationStage(stack)) { case 1 -> 1.55F; case 2 -> 1.85F; default -> 2.5F; };
            pose.mulPose(Axis.XP.rotationDegrees(15));
            pose.mulPose(Axis.YP.rotationDegrees(25));
        }
        if (context == ItemDisplayContext.GUI) {
            pose.pushPose();
            pose.scale(scale, scale, scale);
            model.renderBody(stack, pose, buffers, light, overlay, 0, 0, 1);
            pose.popPose();
            if (PufferfishItems.isRadiant(stack)) {
                // Keep the full animated aura within the slot, independently of puff-state magnification.
                pose.scale(0.6F, -0.6F, -0.6F);
                RadiantPufferfishVisuals.renderCentered(pose, buffers, overlay);
            }
        } else {
            pose.scale(scale, scale, scale);
            model.renderHead(stack, pose, buffers, light, overlay, 0);
        }
        pose.popPose();
    }
}
