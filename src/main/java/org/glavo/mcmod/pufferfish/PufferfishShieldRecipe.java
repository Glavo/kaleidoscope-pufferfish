// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;

/// Covers a shield without repairing it or discarding its stored components.
public final class PufferfishShieldRecipe extends ShapelessRecipe {
    public PufferfishShieldRecipe(ShapelessRecipe recipe) {
        super(recipe.getGroup(), recipe.category(), recipe.getResultItem(null), recipe.getIngredients());
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        for (var stack : input.items()) {
            if (stack.is(Items.SHIELD)) return stack.transmuteCopy(getResultItem(registries).getItem(), 1);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<?> getSerializer() { return PufferfishPlease.SHIELD_RECIPE.get(); }

    public static final class Serializer implements RecipeSerializer<PufferfishShieldRecipe> {
        private static final MapCodec<PufferfishShieldRecipe> CODEC = RecipeSerializer.SHAPELESS_RECIPE.codec()
                .xmap(PufferfishShieldRecipe::new, recipe -> recipe);
        private static final StreamCodec<RegistryFriendlyByteBuf, PufferfishShieldRecipe> STREAM_CODEC = RecipeSerializer.SHAPELESS_RECIPE.streamCodec()
                .map(PufferfishShieldRecipe::new, recipe -> recipe);

        @Override
        public MapCodec<PufferfishShieldRecipe> codec() { return CODEC; }
        @Override
        public StreamCodec<RegistryFriendlyByteBuf, PufferfishShieldRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
