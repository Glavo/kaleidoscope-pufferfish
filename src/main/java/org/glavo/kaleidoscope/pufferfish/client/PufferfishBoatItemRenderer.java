// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.glavo.kaleidoscope.pufferfish.KaleidoscopePufferfish;

/// Uses the world hull for a three-quarter inventory view and held items.
public final class PufferfishBoatItemRenderer extends BlockEntityWithoutLevelRenderer implements IClientItemExtensions {
    private PufferfishBoatModel model;

    public PufferfishBoatItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public BlockEntityWithoutLevelRenderer getCustomRenderer() { return this; }

    @Override
    public void onResourceManagerReload(ResourceManager manager) { model = null; }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (model == null) model = new PufferfishBoatModel();
        var texture = KaleidoscopePufferfish.id("textures/entity/boat/" + BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath() + ".png");
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(Axis.ZP.rotationDegrees(40));
        pose.mulPose(Axis.XP.rotationDegrees(40));
        if (context == ItemDisplayContext.GUI) pose.scale(0.34F, -0.41F, -0.52F);
        else pose.scale(0.34F, -0.34F, -0.34F);
        model.restPaddle();
        model.renderToBuffer(pose, ItemRenderer.getFoilBufferDirect(buffers, model.renderType(texture), true, stack.hasFoil()), light, overlay);
        pose.popPose();
    }
}
