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
import net.minecraft.world.item.crafting.ShapedRecipe;
import org.jetbrains.annotations.Nullable;

/// Retains the ingredient fish's cultivation without turning the lamp into a weapon.
public final class PufferfishLampRecipe extends ShapedRecipe {
    /// Retains the pattern, output, and recipe-book settings of the supplied shaped recipe.
    public PufferfishLampRecipe(ShapedRecipe recipe) {
        super(recipe.getGroup(), recipe.category(), recipe.pattern, recipe.getResultItem(null), recipe.showNotification());
    }

    /// Copies cultivation to the matching lamp without transferring weapon durability.
    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        if (!pattern.matches(input)) return ItemStack.EMPTY;
        var result = super.assemble(input, registries);
        for (var stack : input.items()) {
            if (PufferfishItems.isPufferfish(stack)) {
                @Nullable var cultivation = stack.get(PufferfishPlease.CULTIVATION.get());
                if (cultivation != null) result.set(PufferfishPlease.CULTIVATION.get(), cultivation);
                break;
            }
        }
        return result;
    }

    /// Uses the cultivation-preserving lamp serializer.
    @Override
    public RecipeSerializer<?> getSerializer() { return PufferfishPlease.LAMP_RECIPE.get(); }

    /// Uses vanilla shaped-recipe formats for data packs and network synchronization.
    public static final class Serializer implements RecipeSerializer<PufferfishLampRecipe> {
        /// The data-pack representation of a lamp recipe.
        private static final MapCodec<PufferfishLampRecipe> CODEC = ShapedRecipe.Serializer.CODEC
                .xmap(PufferfishLampRecipe::new, recipe -> recipe);
        /// The network representation of a lamp recipe.
        private static final StreamCodec<RegistryFriendlyByteBuf, PufferfishLampRecipe> STREAM_CODEC = ShapedRecipe.Serializer.STREAM_CODEC
                .map(PufferfishLampRecipe::new, recipe -> recipe);

        /// Returns the data-pack codec.
        @Override public MapCodec<PufferfishLampRecipe> codec() { return CODEC; }
        /// Returns the network codec.
        @Override public StreamCodec<RegistryFriendlyByteBuf, PufferfishLampRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
