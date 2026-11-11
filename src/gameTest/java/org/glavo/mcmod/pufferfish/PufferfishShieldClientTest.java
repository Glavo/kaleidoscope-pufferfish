// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.glavo.mcmod.pufferfish.test.ClientTestLevel;

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
                var capture = new Capture();
                var pose = new PoseStack();
                IClientItemExtensions.of(stack).getCustomRenderer().renderByItem(stack, context, pose, capture, LIGHT, OverlayTexture.NO_OVERLAY);
                if (capture.vertices != (PufferfishItems.isRadiant(stack) ? 180 : 52) || !pose.clear()) throw new IllegalStateException("Shield frame, grip, skin or radiant trails did not render");
            }
        }
    }

    private static final class Capture implements MultiBufferSource, VertexConsumer {
        private int vertices;
        @Override public VertexConsumer getBuffer(RenderType type) { return this; }
        @Override public VertexConsumer addVertex(float x, float y, float z) {
            if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) throw new IllegalStateException("Non-finite shield vertex");
            vertices++;
            return this;
        }
        @Override public VertexConsumer setColor(int r, int g, int b, int a) { return this; }
        @Override public VertexConsumer setUv(float u, float v) { return this; }
        @Override public VertexConsumer setUv1(int u, int v) { return this; }
        @Override public VertexConsumer setUv2(int u, int v) { return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { return this; }
    }
}
