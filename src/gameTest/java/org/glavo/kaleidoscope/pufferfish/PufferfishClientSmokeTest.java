// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.glavo.kaleidoscope.pufferfish.client.PufferfishTextures;

@EventBusSubscriber(modid = KaleidoscopePufferfish.MODID, value = Dist.CLIENT)
public final class PufferfishClientSmokeTest {
    private static int phase;

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!Boolean.getBoolean("kaleidoscope_pufferfish.clientSmokeTest") || phase == 2
                || minecraft.screen == null || minecraft.getOverlay() != null) {
            return;
        }
        for (DyeColor color : DyeColor.values()) {
            for (boolean bucket : new boolean[] {false, true}) {
                ItemStack stack = PufferfishColors.dye(new ItemStack(bucket ? Items.PUFFERFISH_BUCKET : Items.PUFFERFISH), color);
                var model = minecraft.getItemRenderer().getModel(stack, null, null, 0);
                var expected = minecraft.getModelManager().getModel(ModelResourceLocation.standalone(PufferfishTextures.item(color, bucket)));
                if (model != expected || model == minecraft.getModelManager().getMissingModel()
                        || model.getQuads(null, null, RandomSource.create()).isEmpty()
                        || !model.getParticleIcon().contents().name().equals(PufferfishTextures.item(color, bucket))) {
                    throw new IllegalStateException("Missing dyed pufferfish model or sprite: " + color + ", bucket=" + bucket);
                }
                if (stack.getHoverName().getString().contains("color.minecraft.")) {
                    throw new IllegalStateException("Missing dye color translation: " + color);
                }
            }
            var texture = PufferfishTextures.entity(color);
            if (!texture.getNamespace().equals(KaleidoscopePufferfish.MODID)) {
                throw new IllegalStateException("Missing dyed pufferfish entity texture: " + color);
            }
        }
        phase++;
        if (phase == 1) {
            minecraft.reloadResourcePacks();
        } else {
            LogUtils.getLogger().info("Pufferfish client smoke test passed: 32 models, 16 entity textures, and resource reload");
            minecraft.stop();
        }
    }
}
