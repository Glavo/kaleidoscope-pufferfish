// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.compat.create.client;

import net.minecraft.client.resources.model.ModelResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.glavo.kaleidoscope.pufferfish.KaleidoscopePufferfish;
import org.glavo.kaleidoscope.pufferfish.compat.create.CreateIntegration;

public final class CreateClientIntegration {
    public static final String[] PARTS = {"piston", "shaft", "flywheel", "flap", "water"};

    private CreateClientIntegration() {}

    public static ModelResourceLocation model(String part) {
        return ModelResourceLocation.standalone(KaleidoscopePufferfish.id("block/pufferfish_engine/" + part));
    }

    public static void register(IEventBus bus) {
        bus.addListener(CreateClientIntegration::registerModels);
        bus.addListener(CreateClientIntegration::registerRenderers);
    }

    private static void registerModels(ModelEvent.RegisterAdditional event) {
        for (String part : PARTS) {
            event.register(model(part));
        }
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(CreateIntegration.ENGINE_ENTITY.get(), PufferfishEngineRenderer::new);
    }
}
