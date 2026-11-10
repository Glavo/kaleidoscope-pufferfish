// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import java.util.EnumMap;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModList;
import org.glavo.kaleidoscope.pufferfish.compat.create.client.CreateClientIntegration;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.event.RegisterSpriteSourceTypesEvent;
import org.glavo.kaleidoscope.pufferfish.client.PufferfishItemModel;
import org.glavo.kaleidoscope.pufferfish.client.PufferfishHeadRenderer;
import org.glavo.kaleidoscope.pufferfish.client.PufferfishGloveRenderer;
import org.glavo.kaleidoscope.pufferfish.client.PufferfishSpriteSource;
import org.glavo.kaleidoscope.pufferfish.client.PufferfishTextures;
import org.glavo.kaleidoscope.pufferfish.client.PufferfishConduitRenderer;
import org.glavo.kaleidoscope.pufferfish.client.PufferfishFlailHeadRenderer;
import org.glavo.kaleidoscope.pufferfish.client.PufferfishFlailItemRenderer;
import org.glavo.kaleidoscope.pufferfish.client.PufferfishTooltips;

@Mod(value = KaleidoscopePufferfish.MODID, dist = Dist.CLIENT)
public class KaleidoscopePufferfishClient {
    /// The item renderer shared by both flail variants and refreshed with client resources.
    private PufferfishFlailItemRenderer flailRenderer;
    private org.glavo.kaleidoscope.pufferfish.client.PufferfishLampItemRenderer lampRenderer;
    private org.glavo.kaleidoscope.pufferfish.client.PufferfishShieldRenderer shieldRenderer;
    private org.glavo.kaleidoscope.pufferfish.client.PufferfishBoatItemRenderer boatRenderer;

    public KaleidoscopePufferfishClient(IEventBus bus) {
        if (ModList.get().isLoaded("create")) {
            CreateClientIntegration.register(bus);
        }
        bus.addListener(this::registerSprites);
        bus.addListener(this::clientSetup);
        bus.addListener(this::registerModels);
        bus.addListener(EventPriority.LOWEST, this::wrapModels);
        bus.addListener(this::registerReloadListeners);
        bus.addListener(this::registerRenderers);
        bus.addListener(this::registerItemExtensions);
        bus.addListener((net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item event) -> event.register((stack, tint) -> {
            var color = stack.get(KaleidoscopePufferfish.COLOR.get());
            return color == null ? ((net.minecraft.world.item.SpawnEggItem) stack.getItem()).getColor(tint)
                    : tint == 0 ? color.getTextureDiffuseColor() : 0xFFF0D5;
        }, net.minecraft.world.item.Items.PUFFERFISH_SPAWN_EGG));
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, PufferfishHeadRenderer::beforePlayer);
        NeoForge.EVENT_BUS.addListener(PufferfishHeadRenderer::afterPlayer);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, PufferfishGloveRenderer::renderHand);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, org.glavo.kaleidoscope.pufferfish.client.PufferfishRowingAnimation::renderHands);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, org.glavo.kaleidoscope.pufferfish.client.PufferfishFlailArmAnimation::renderHand);
        NeoForge.EVENT_BUS.addListener(org.glavo.kaleidoscope.pufferfish.client.PufferfishFlailTether::beginFrame);
        NeoForge.EVENT_BUS.addListener(org.glavo.kaleidoscope.pufferfish.client.PufferfishFlailInput::attack);
        NeoForge.EVENT_BUS.addListener(org.glavo.kaleidoscope.pufferfish.client.PufferfishFlailTether::captureWorld);
        NeoForge.EVENT_BUS.addListener(org.glavo.kaleidoscope.pufferfish.client.HeavyPufferfishVisuals::tick);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, PufferfishTooltips::hideCreativeCategory);
    }

    private org.glavo.kaleidoscope.pufferfish.client.PufferfishBoatItemRenderer boatRenderer() {
        if (boatRenderer == null) boatRenderer = new org.glavo.kaleidoscope.pufferfish.client.PufferfishBoatItemRenderer();
        return boatRenderer;
    }

    private org.glavo.kaleidoscope.pufferfish.client.PufferfishShieldRenderer shieldRenderer() {
        if (shieldRenderer == null) shieldRenderer = new org.glavo.kaleidoscope.pufferfish.client.PufferfishShieldRenderer();
        return shieldRenderer;
    }

    private void clientSetup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(() -> PufferfishShields.VARIANTS.values().forEach(item ->
                net.minecraft.client.renderer.item.ItemProperties.register(item.get(), ResourceLocation.withDefaultNamespace("blocking"),
                        (stack, level, entity, seed) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1 : 0)));
    }

    /// Creates the flail renderer on demand after the client dispatchers are initialized.
    private PufferfishFlailItemRenderer flailRenderer() {
        if (flailRenderer == null) flailRenderer = new PufferfishFlailItemRenderer();
        return flailRenderer;
    }

    private void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        PufferfishMobs.VARIANTS.values().forEach(variant -> event.registerEntityRenderer(variant.entity().get(),
                org.glavo.kaleidoscope.pufferfish.client.MaterialPufferfishRenderer::new));
        PufferfishBoats.VARIANTS.values().forEach(variant -> event.registerEntityRenderer(variant.entity().get(),
                org.glavo.kaleidoscope.pufferfish.client.PufferfishBoatRenderer::new));
        event.registerEntityRenderer(KaleidoscopePufferfish.THROWN_PUFFERFISH.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(KaleidoscopePufferfish.FLAIL_HEAD.get(), PufferfishFlailHeadRenderer::new);
        event.registerBlockEntityRenderer(PufferfishBlocks.CONDUIT_ENTITY.get(), PufferfishConduitRenderer::new);
        event.registerBlockEntityRenderer(PufferfishBlocks.PLACED_FISH_ENTITY.get(), org.glavo.kaleidoscope.pufferfish.client.PlacedPufferfishRenderer::new);
    }

    private org.glavo.kaleidoscope.pufferfish.client.PufferfishLampItemRenderer lampRenderer() {
        if (lampRenderer == null) lampRenderer = new org.glavo.kaleidoscope.pufferfish.client.PufferfishLampItemRenderer();
        return lampRenderer;
    }

    private void registerItemExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(lampRenderer(), PufferfishLamps.VARIANTS.values().stream()
                .map(variant -> variant.block().get().asItem()).toArray(net.minecraft.world.item.Item[]::new));
        event.registerItem(boatRenderer(), PufferfishBoats.VARIANTS.values().stream().map(variant -> variant.item().get()).toArray(net.minecraft.world.item.Item[]::new));
        event.registerItem(PufferfishConduitRenderer.itemExtensions(), PufferfishBlocks.CONDUIT.get().asItem());
        event.registerItem(shieldRenderer(), PufferfishShields.VARIANTS.values().stream().map(holder -> holder.get()).toArray(net.minecraft.world.item.Item[]::new));
        event.registerItem(flailRenderer(), PufferfishItems.FLAIL_VARIANTS.stream().map(item -> item.get()).toArray(net.minecraft.world.item.Item[]::new));
    }

    private void registerSprites(RegisterSpriteSourceTypesEvent event) {
        event.register(KaleidoscopePufferfish.id("dyed_pufferfish"), PufferfishSpriteSource.TYPE);
    }

    private void registerModels(ModelEvent.RegisterAdditional event) {
        event.register(ModelResourceLocation.standalone(KaleidoscopePufferfish.id("item/raw_pufferfish_baozi_steamer")));
        event.register(ModelResourceLocation.standalone(KaleidoscopePufferfish.id("item/pufferfish_baozi_steamer")));
        for (DyeColor color : DyeColor.values()) {
            event.register(ModelResourceLocation.standalone(PufferfishTextures.item(color, false)));
            event.register(ModelResourceLocation.standalone(PufferfishTextures.item(color, true)));
        }
    }

    private void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(boatRenderer());
        event.registerReloadListener(new PufferfishTextures());
        event.registerReloadListener(new org.glavo.kaleidoscope.pufferfish.client.PufferfishBoatTextures());
        event.registerReloadListener(new org.glavo.kaleidoscope.pufferfish.client.PufferfishArmorTextures());
        event.registerReloadListener(flailRenderer());
        event.registerReloadListener(lampRenderer());
        event.registerReloadListener(shieldRenderer());
        event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener) manager -> PufferfishGloveRenderer.reset());
    }

    private void wrapModels(ModelEvent.ModifyBakingResult event) {
        for (boolean bucket : new boolean[] {false, true}) {
            var id = new ModelResourceLocation(ResourceLocation.withDefaultNamespace(bucket ? "pufferfish_bucket" : "pufferfish"), "inventory");
            var models = new EnumMap<DyeColor, BakedModel>(DyeColor.class);
            for (DyeColor color : DyeColor.values()) {
                models.put(color, event.getModels().get(ModelResourceLocation.standalone(PufferfishTextures.item(color, bucket))));
            }
            event.getModels().computeIfPresent(id, (key, original) -> new PufferfishItemModel(original, models));
        }
    }
}
