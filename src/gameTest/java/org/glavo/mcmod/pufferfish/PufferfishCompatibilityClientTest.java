// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.StatsCounter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.extensions.common.ClientExtensionsManager;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.glavo.mcmod.pufferfish.client.PufferfishFlailArmAnimation;
import org.glavo.mcmod.pufferfish.client.PufferfishGloveRenderer;
import org.glavo.mcmod.pufferfish.client.PufferfishRowingAnimation;
import org.glavo.mcmod.pufferfish.test.ClientTestLevel;

public final class PufferfishCompatibilityClientTest {
    public static void verify(Minecraft minecraft) {
        try {
            verifyItemExtensions(minecraft);
            verifyPlayerRenderer(minecraft);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to install compatibility fixtures", exception);
        }
    }

    @SuppressWarnings("unchecked")
    private static void verifyItemExtensions(Minecraft minecraft) throws ReflectiveOperationException {
        var field = ClientExtensionsManager.class.getDeclaredField("ITEM_EXTENSIONS");
        field.setAccessible(true);
        var extensions = (Map<Item, IClientItemExtensions>) field.get(null);
        var external = new IClientItemExtensions() {};
        var previous = extensions.put(Items.PUFFERFISH, external);
        try {
            var constructor = RegisterClientExtensionsEvent.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            net.neoforged.fml.ModList.get().getModContainerById(PufferfishPlease.MODID).orElseThrow()
                    .getEventBus().post(constructor.newInstance());
            if (IClientItemExtensions.of(Items.PUFFERFISH) != external) {
                throw new IllegalStateException("Existing item extensions were replaced");
            }
            var stack = new ItemStack(Items.PUFFERFISH);
            PufferfishDurability.activate(stack);
            stack.setDamageValue(stack.getMaxDamage() / 2);
            if (minecraft.getItemRenderer().getModel(stack, null, null, 0).isCustomRenderer()) {
                throw new IllegalStateException("Worn fish forced another extension to use the pufferfish renderer");
            }
        } finally {
            if (previous == null) extensions.remove(Items.PUFFERFISH);
            else extensions.put(Items.PUFFERFISH, previous);
        }
    }

    private static void verifyPlayerRenderer(Minecraft minecraft) throws ReflectiveOperationException {
        var dispatcher = minecraft.getEntityRenderDispatcher();
        var field = EntityRenderDispatcher.class.getDeclaredField("playerRenderers");
        field.setAccessible(true);
        var previousRenderers = field.get(dispatcher);
        var previousPlayer = minecraft.player;
        var level = ClientTestLevel.level(minecraft);
        var player = new LocalPlayer(minecraft, level, ClientTestLevel.connection(minecraft),
                new StatsCounter(), new ClientRecipeBook(), false, false) {
            @Override public boolean isSpectator() { return false; }
        };
        var context = new EntityRendererProvider.Context(dispatcher, minecraft.getItemRenderer(), minecraft.getBlockRenderer(),
                dispatcher.getItemInHandRenderer(), minecraft.getResourceManager(), minecraft.getEntityModels(), minecraft.font);
        var replacement = new EntityRenderer<AbstractClientPlayer>(context) {
            @Override public ResourceLocation getTextureLocation(AbstractClientPlayer entity) { return entity.getSkin().texture(); }
        };
        try {
            field.set(dispatcher, Map.of(PlayerSkin.Model.WIDE, replacement, PlayerSkin.Model.SLIM, replacement));
            minecraft.player = player;
            var fish = new ItemStack(Items.PUFFERFISH);
            player.setItemInHand(InteractionHand.MAIN_HAND, fish);
            verifyHand(minecraft, PufferfishGloveRenderer::renderHand, InteractionHand.MAIN_HAND, fish);
            var flail = new ItemStack(PufferfishItems.NETHERITE_FLAIL.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, flail);
            player.startUsingItem(InteractionHand.MAIN_HAND);
            if (PufferfishFlailArmAnimation.activeHand(player) == null) throw new IllegalStateException("Flail fixture is inactive");
            verifyHand(minecraft, PufferfishFlailArmAnimation::renderHand, InteractionHand.MAIN_HAND, flail);
            player.stopUsingItem();
            var boat = PufferfishBoats.VARIANTS.get("pufferfish_skin_boat").entity().get().create(level);
            player.startRiding(boat, true);
            boat.setPaddleState(true, true);
            if (!PufferfishRowingAnimation.isRowing(player)) throw new IllegalStateException("Rowing fixture is inactive");
            for (var hand : InteractionHand.values()) verifyHand(minecraft, PufferfishRowingAnimation::renderHands, hand, ItemStack.EMPTY);
        } finally {
            player.stopRiding();
            minecraft.player = previousPlayer;
            field.set(dispatcher, previousRenderers);
        }
    }

    private static void verifyHand(Minecraft minecraft, Consumer<RenderHandEvent> renderer, InteractionHand hand, ItemStack stack) {
        var pose = new PoseStack();
        var before = new org.joml.Matrix4f(pose.last().pose());
        var event = new RenderHandEvent(hand, pose, minecraft.renderBuffers().bufferSource(), 15728880, 0, 0, 0, 0, stack);
        renderer.accept(event);
        if (event.isCanceled() || !pose.clear() || !before.equals(pose.last().pose())) {
            throw new IllegalStateException("Unsupported player renderer lost native hand rendering");
        }
    }
}
