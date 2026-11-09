// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.glavo.kaleidoscope.pufferfish.PufferfishLamps;

public final class PufferfishLampItemRenderer extends BlockEntityWithoutLevelRenderer implements IClientItemExtensions {
    private PufferfishFlailModel model;

    public PufferfishLampItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public BlockEntityWithoutLevelRenderer getCustomRenderer() { return this; }

    @Override
    public void onResourceManagerReload(ResourceManager manager) { model = null; }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (!(stack.getItem() instanceof BlockItem item)) return;
        if (model == null) model = new PufferfishFlailModel();
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        model.renderHead(PufferfishLamps.fish(item.getBlock()), pose, buffers, 15728880, overlay, 0);
        pose.popPose();
    }
}
