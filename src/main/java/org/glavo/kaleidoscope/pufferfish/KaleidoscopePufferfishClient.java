// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterSpriteSourceTypesEvent;
import org.glavo.kaleidoscope.pufferfish.client.PufferfishSpriteSource;
import org.glavo.kaleidoscope.pufferfish.client.PufferfishTextures;

@Mod(value = KaleidoscopePufferfish.MODID, dist = Dist.CLIENT)
public class KaleidoscopePufferfishClient {
    public KaleidoscopePufferfishClient(IEventBus bus) {
        bus.addListener(this::registerSprites);
        bus.addListener(this::registerModels);
        bus.addListener(this::registerReloadListeners);
    }

    private void registerSprites(RegisterSpriteSourceTypesEvent event) {
        event.register(KaleidoscopePufferfish.id("dyed_pufferfish"), PufferfishSpriteSource.TYPE);
    }

    private void registerModels(ModelEvent.RegisterAdditional event) {
        for (DyeColor color : DyeColor.values()) {
            event.register(ModelResourceLocation.standalone(PufferfishTextures.item(color, false)));
            event.register(ModelResourceLocation.standalone(PufferfishTextures.item(color, true)));
        }
    }

    private void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new PufferfishTextures());
    }
}
