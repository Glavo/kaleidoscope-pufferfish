// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.compat.chinesefood;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.glavo.kaleidoscope.pufferfish.KaleidoscopePufferfish;

public final class ChineseFoodIntegration {
    private static final DeferredRegister<RecipeSerializer<?>> RECIPES = DeferredRegister.create(Registries.RECIPE_SERIALIZER, KaleidoscopePufferfish.MODID);
    public static final DeferredHolder<RecipeSerializer<?>, PufferfishAspicRecipe.Serializer> ASPIC_RECIPE =
            RECIPES.register("refrigerating_pufferfish_aspic", PufferfishAspicRecipe.Serializer::new);

    private ChineseFoodIntegration() {}

    public static void register(IEventBus bus) { RECIPES.register(bus); }
}
