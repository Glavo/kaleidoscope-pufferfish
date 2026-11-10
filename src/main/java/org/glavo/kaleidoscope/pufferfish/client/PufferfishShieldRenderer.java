// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ShieldModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.glavo.kaleidoscope.pufferfish.KaleidoscopePufferfish;
import org.glavo.kaleidoscope.pufferfish.PufferfishItems;

/// Keeps the vanilla shield frame and handle, covering its front plate with matching skin.
public final class PufferfishShieldRenderer extends BlockEntityWithoutLevelRenderer implements IClientItemExtensions {
    private ShieldModel model;

    public PufferfishShieldRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public BlockEntityWithoutLevelRenderer getCustomRenderer() { return this; }

    @Override
    public void onResourceManagerReload(ResourceManager manager) { model = null; }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (model == null) model = new ShieldModel(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.SHIELD));
        pose.pushPose();
        pose.scale(1, -1, -1);
        var base = ModelBakery.NO_PATTERN_SHIELD;
        model.renderToBuffer(pose, base.sprite().wrap(ItemRenderer.getFoilBufferDirect(buffers,
                model.renderType(base.atlasLocation()), true, stack.hasFoil())), light, overlay);
        var name = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        var skin = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(KaleidoscopePufferfish.id("block/" + name.substring(0, name.length() - "_shield".length())));
        var vertices = ItemRenderer.getFoilBufferDirect(buffers, RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS), true, stack.hasFoil());
        float z = -2.01F / 16;
        vertices.addVertex(pose.last(), -5F / 16, -10F / 16, z).setColor(-1).setUv(skin.getU0(), skin.getV0()).setOverlay(overlay).setLight(light).setNormal(pose.last(), 0, 0, -1);
        vertices.addVertex(pose.last(), -5F / 16, 10F / 16, z).setColor(-1).setUv(skin.getU0(), skin.getV1()).setOverlay(overlay).setLight(light).setNormal(pose.last(), 0, 0, -1);
        vertices.addVertex(pose.last(), 5F / 16, 10F / 16, z).setColor(-1).setUv(skin.getU1(), skin.getV1()).setOverlay(overlay).setLight(light).setNormal(pose.last(), 0, 0, -1);
        vertices.addVertex(pose.last(), 5F / 16, -10F / 16, z).setColor(-1).setUv(skin.getU1(), skin.getV0()).setOverlay(overlay).setLight(light).setNormal(pose.last(), 0, 0, -1);
        if (PufferfishItems.isRadiant(stack)) {
            pose.translate(0, 0, z);
            pose.scale(1, 1.4F, 1);
            RadiantPufferfishVisuals.renderCentered(pose, buffers, overlay);
        }
        pose.popPose();
    }
}
