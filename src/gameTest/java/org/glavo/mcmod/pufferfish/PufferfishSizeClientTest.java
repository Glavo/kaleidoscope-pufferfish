// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import org.glavo.mcmod.pufferfish.client.PlacedPufferfishRenderer;
import org.glavo.mcmod.pufferfish.client.PufferfishFlailModel;
import org.glavo.mcmod.pufferfish.test.RenderCapture;

public final class PufferfishSizeClientTest {
    public static void verify(Minecraft minecraft) {
        for (var original : java.util.List.of(new ItemStack(Items.PUFFERFISH), PufferfishItems.NETHERITE.toStack())) {
            for (int stage = 0; stage < 4; stage++) {
                var base = original.copy();
                PufferfishDurability.activate(base);
                base.setDamageValue((base.getMaxDamage() * stage + 3) / 4);
                var grown = base.copy();
                grown.set(PufferfishPlease.CULTIVATION.get(), new PufferfishCultivation(PufferfishLevel.LEVEL_10, 0.5, Long.MAX_VALUE, Long.MAX_VALUE));
                for (var context : ItemDisplayContext.values()) {
                    float expected = context == ItemDisplayContext.GUI ? 1 : context == ItemDisplayContext.GROUND || context == ItemDisplayContext.FIXED ? 2 : 1.25F;
                    compare(item(minecraft, base, context), item(minecraft, grown, context), expected);
                }
                var renderer = new PlacedPufferfishRenderer(minecraft.getEntityModels());
                var state = PufferfishBlocks.PLACED_FISH.get().defaultBlockState();
                var first = new RenderCapture();
                var second = new RenderCapture();
                renderer.render(base, state, new PoseStack(), first, 15728880, OverlayTexture.NO_OVERLAY);
                renderer.render(grown, state, new PoseStack(), second, 15728880, OverlayTexture.NO_OVERLAY);
                compare(first.bounds(), second.bounds(), 2);
                if (Math.abs(first.bounds().minY - second.bounds().minY) > 0.03) throw new IllegalStateException("Growing a placed fish moved it away from the floor");
            }
        }
        var base = PufferfishItems.FLAIL.toStack();
        var grown = base.copy();
        grown.set(PufferfishPlease.CULTIVATION.get(), new PufferfishCultivation(PufferfishLevel.LEVEL_10, 0.5, 0, 0));
        compare(item(minecraft, base, ItemDisplayContext.GUI), item(minecraft, grown, ItemDisplayContext.GUI), 1);
        var model = new PufferfishFlailModel();
        var first = new RenderCapture();
        var second = new RenderCapture();
        model.renderHead(base, new PoseStack(), first, 15728880, OverlayTexture.NO_OVERLAY, 0);
        model.renderHead(grown, new PoseStack(), second, 15728880, OverlayTexture.NO_OVERLAY, 0);
        compare(first.bounds(), second.bounds(), 1.25);
    }

    private static AABB item(Minecraft minecraft, ItemStack stack, ItemDisplayContext context) {
        var capture = new RenderCapture();
        var pose = new PoseStack();
        minecraft.getItemRenderer().renderStatic(stack, context, 15728880, OverlayTexture.NO_OVERLAY, pose, capture, null, 0);
        if (!pose.clear()) throw new IllegalStateException("Growth leaked item transforms");
        return capture.bounds();
    }

    private static void compare(AABB original, AABB grown, double scale) {
        if (Math.abs(grown.getXsize() - original.getXsize() * scale) > 0.0001
                || Math.abs(grown.getYsize() - original.getYsize() * scale) > 0.0001
                || Math.abs(grown.getZsize() - original.getZsize() * scale) > 0.0001) {
            throw new IllegalStateException("Rank geometry has the wrong context scale: expected " + scale + ", original=" + original + ", grown=" + grown);
        }
    }
}
