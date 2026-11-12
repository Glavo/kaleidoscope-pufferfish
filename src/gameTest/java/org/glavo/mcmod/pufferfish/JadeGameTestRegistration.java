// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

@EventBusSubscriber(modid = PufferfishPlease.MODID)
public final class JadeGameTestRegistration {
    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        if (ModList.get().isLoaded("jade")) event.register(PufferfishJadeGameTests.class);
    }
}
