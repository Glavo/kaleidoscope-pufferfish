// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.glavo.mcmod.pufferfish.test.RenderCapture;

public final class RadiantPufferfishClientTest {
    public static void verify(Minecraft minecraft) {
        verifyRadiantSprite(minecraft.getItemRenderer().getModel(PufferfishItems.RADIANT.toStack(), null, null, 0));
        var stars = RenderType.entityTranslucentEmissive(net.minecraft.resources.ResourceLocation.withDefaultNamespace("textures/particle/flash.png"));
        for (var item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
            var stack = new ItemStack(item);
            if (!PufferfishItems.isRadiant(stack) || PufferfishItems.isPufferfish(stack)) continue;
            for (var context : ItemDisplayContext.values()) {
                var pose = new PoseStack();
                var capture = new RenderCapture();
                minecraft.getItemRenderer().renderStatic(stack, context, 15728880, OverlayTexture.NO_OVERLAY, pose, capture, null, 0);
                if (capture.count(stars) == 0 || !pose.clear()) {
                    throw new IllegalStateException("Radiant item lost its trails or leaked its transform: " + stack + ", " + context);
                }
            }
        }
    }

    private static void verifyRadiantSprite(net.minecraft.client.resources.model.BakedModel model) {
        var sprite = model.getParticleIcon().contents();
        if (model.isGui3d() || model.isCustomRenderer() || sprite.getUniqueFrames().count() < 2) {
            throw new IllegalStateException("Radiant item must remain flat and animate its star trails");
        }
        var source = sprite.getOriginalImage();
        int size = sprite.width();
        var hashes = new java.util.HashSet<Long>();
        for (int frame : sprite.getUniqueFrames().toArray()) {
            long hash = 1;
            for (int y = 0; y < size; y++) for (int x = 0; x < size; x++) {
                hash = hash * 31 + source.getPixelRGBA(x, frame * size + y);
            }
            hashes.add(hash);
        }
        if (hashes.size() < 2) throw new IllegalStateException("Radiant star trail animation is static");
    }
}
