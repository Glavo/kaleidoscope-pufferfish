// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;

/// Retains the ingredient fish's cultivation without turning the lamp into a weapon.
public final class PufferfishLampRecipe extends ShapelessRecipe {
    public PufferfishLampRecipe(ShapelessRecipe recipe) {
        super(recipe.getGroup(), recipe.category(), recipe.getResultItem(null), recipe.getIngredients());
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        var result = super.assemble(input, registries);
        for (var stack : input.items()) {
            if (PufferfishItems.isPufferfish(stack)) {
                var cultivation = stack.get(PufferfishPlease.CULTIVATION.get());
                if (cultivation != null) result.set(PufferfishPlease.CULTIVATION.get(), cultivation);
                break;
            }
        }
        return result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() { return PufferfishPlease.LAMP_RECIPE.get(); }

    public static final class Serializer implements RecipeSerializer<PufferfishLampRecipe> {
        private static final MapCodec<PufferfishLampRecipe> CODEC = new ShapelessRecipe.Serializer().codec()
                .xmap(PufferfishLampRecipe::new, recipe -> recipe);
        private static final StreamCodec<RegistryFriendlyByteBuf, PufferfishLampRecipe> STREAM_CODEC = new ShapelessRecipe.Serializer().streamCodec()
                .map(PufferfishLampRecipe::new, recipe -> recipe);

        @Override public MapCodec<PufferfishLampRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, PufferfishLampRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
