// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.glavo.mcmod.pufferfish.test.ClientTestLevel;
import org.glavo.mcmod.pufferfish.test.RenderCapture;
import org.glavo.mcmod.pufferfish.client.PufferfishTextures;

public final class PufferfishShieldClientTest {
    private static final int LIGHT = 15728880;

    public static void verify(Minecraft minecraft) {
        var level = ClientTestLevel.level(minecraft);
        var player = new RemotePlayer(level, new GameProfile(UUID.randomUUID(), "shield-render-test")) {
            @Override
            public boolean isUsingItem() { return !getUseItem().isEmpty(); }
        };
        for (var entry : PufferfishShields.VARIANTS.entrySet()) {
            var stack = new ItemStack(entry.getValue().get());
            var model = minecraft.getItemRenderer().getModel(stack, level, player, 0);
            var texture = PufferfishPlease.id("block/" + entry.getKey().replace("_shield", ""));
            if (!model.isCustomRenderer() || !model.getParticleIcon().contents().name().equals(texture)
                    || stack.getHoverName().getString().startsWith("item.")) throw new IllegalStateException("Missing skin shield assets");
            player.setItemInHand(InteractionHand.OFF_HAND, stack);
            player.startUsingItem(InteractionHand.OFF_HAND);
            var blocking = minecraft.getItemRenderer().getModel(stack, level, player, 0);
            if (blocking == model || blocking == minecraft.getModelManager().getMissingModel()) {
                throw new IllegalStateException("Skin shield did not select its blocking model");
            }
            player.stopUsingItem();
            for (var context : ItemDisplayContext.values()) {
                var capture = new RenderCapture();
                var pose = new PoseStack();
                IClientItemExtensions.of(stack).getCustomRenderer().renderByItem(stack, context, pose, capture, LIGHT, OverlayTexture.NO_OVERLAY);
                int glow = capture.count(RenderType.entityTranslucentEmissive(PufferfishTextures.radiantGlow()));
                int stars = capture.count(RenderType.entityTranslucentEmissive(net.minecraft.resources.ResourceLocation.withDefaultNamespace("textures/particle/flash.png")));
                if (capture.count() - glow - stars != 52 || !pose.clear()) throw new IllegalStateException("Shield frame, grip or skin did not render");
            }
        }
    }

}
