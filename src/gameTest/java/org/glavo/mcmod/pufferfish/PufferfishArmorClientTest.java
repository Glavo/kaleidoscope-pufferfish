// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

public final class PufferfishArmorClientTest {
    public static void verify(Minecraft minecraft) {
        for (var entry : PufferfishArmor.VARIANTS.entrySet()) {
            var stack = new ItemStack(entry.getValue().get());
            var model = minecraft.getItemRenderer().getModel(stack, null, null, 0);
            if (model == minecraft.getModelManager().getMissingModel()
                    || !model.getParticleIcon().contents().name().equals(PufferfishPlease.id("item/" + entry.getKey()))
                    || stack.getHoverName().getString().startsWith("item.")) {
                throw new IllegalStateException("Missing pufferfish armor model or translation");
            }
            var item = entry.getValue().get();
            var texture = item.getMaterial().value().layers().getFirst().texture(item.getType() == ArmorItem.Type.LEGGINGS);
            if (!(minecraft.getTextureManager().getTexture(texture) instanceof DynamicTexture dynamic)
                    || dynamic.getPixels() == null || dynamic.getPixels().getWidth() != 64) {
                throw new IllegalStateException("Missing generated pufferfish armor texture");
            }
        }
    }

}
