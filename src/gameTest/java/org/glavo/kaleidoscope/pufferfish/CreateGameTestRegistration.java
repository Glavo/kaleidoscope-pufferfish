// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.glavo.kaleidoscope.pufferfish.compat.create.PufferfishEngineGameTests;
import org.glavo.kaleidoscope.pufferfish.compat.create.PufferfishCarpetPressingGameTests;

@EventBusSubscriber(modid = KaleidoscopePufferfish.MODID)
@GameTestHolder(KaleidoscopePufferfish.MODID)
@PrefixGameTestTemplate(false)
public final class CreateGameTestRegistration {
    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        if (ModList.get().isLoaded("create")) {
            event.register(PufferfishEngineGameTests.class);
            event.register(PufferfishCarpetPressingGameTests.class);
        }
    }

    @GameTest(template = "empty")
    public static void optionalContentMatchesLoadedMods(GameTestHelper helper) {
        boolean create = ModList.get().isLoaded("create");
        var id = KaleidoscopePufferfish.id("pufferfish_engine");
        helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(id) == create, "Engine block optional registration failed");
        helper.assertTrue(BuiltInRegistries.ITEM.containsKey(id) == create, "Engine item optional registration failed");
        helper.assertTrue(helper.getLevel().getRecipeManager().byKey(id).isPresent() == create, "Engine recipe condition failed");
        for (int color = -1; color < 16; color++) {
            var name = PufferfishBlocks.carpetName(color < 0 ? null : net.minecraft.world.item.DyeColor.byId(color));
            helper.assertTrue(helper.getLevel().getRecipeManager().byKey(KaleidoscopePufferfish.id("pressing/" + name)).isPresent() == create,
                    "Carpet pressing recipe condition failed");
            helper.assertTrue(helper.getLevel().getRecipeManager().byKey(KaleidoscopePufferfish.id(name)).isEmpty(),
                    "Removed skin crafting recipe is still loaded");
        }
        helper.succeed();
    }
}
