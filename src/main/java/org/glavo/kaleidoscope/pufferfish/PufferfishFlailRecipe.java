// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;

/// Crafts a flail using a shaped recipe while retaining the fish's component changes.
public class PufferfishFlailRecipe extends ShapedRecipe {
    /// Creates a recipe with the supplied pattern, output, and recipe-book settings.
    ///
    /// @param recipe the shaped recipe to extend
    public PufferfishFlailRecipe(ShapedRecipe recipe) {
        super(recipe.getGroup(), recipe.category(), recipe.pattern, recipe.getResultItem(null), recipe.showNotification());
    }

    /// Creates one flail retaining the fish's color, custom name, and enchantments.
    /// The generated dyed-fish item name is removed so the result uses its flail name.
    /// Returns an empty stack if the input does not match this recipe.
    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        if (!pattern.matches(input)) return ItemStack.EMPTY;
        for (var stack : input.items()) {
            if (PufferfishItems.isPufferfish(stack)) {
                var result = stack.transmuteCopy(getResultItem(registries).getItem(), 1);
                var name = result.get(DataComponents.ITEM_NAME);
                if (name != null && name.getContents() instanceof TranslatableContents translated
                        && translated.getKey().equals("item.kaleidoscope_pufferfish.dyed_pufferfish")) {
                    result.remove(DataComponents.ITEM_NAME);
                }
                return PufferfishDurability.transfer(stack, result);
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return KaleidoscopePufferfish.FLAIL_RECIPE.get();
    }

    /// Encodes flail recipes using the vanilla shaped-recipe data and network formats.
    public static final class Serializer implements RecipeSerializer<PufferfishFlailRecipe> {
        /// The data-pack representation of a flail recipe.
        private static final MapCodec<PufferfishFlailRecipe> CODEC = ShapedRecipe.Serializer.CODEC
                .xmap(PufferfishFlailRecipe::new, recipe -> recipe);
        /// The network representation of a flail recipe.
        private static final StreamCodec<RegistryFriendlyByteBuf, PufferfishFlailRecipe> STREAM_CODEC = ShapedRecipe.Serializer.STREAM_CODEC
                .map(PufferfishFlailRecipe::new, recipe -> recipe);

        /// Creates a serializer for flail recipes.
        public Serializer() {}

        @Override
        public MapCodec<PufferfishFlailRecipe> codec() { return CODEC; }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, PufferfishFlailRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
