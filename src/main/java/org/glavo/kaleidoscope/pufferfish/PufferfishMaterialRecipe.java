// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;

/// Material upgrades retain enchantments and proportional wear, while removing dye.
public final class PufferfishMaterialRecipe extends PufferfishFlailRecipe {
    public PufferfishMaterialRecipe(ShapedRecipe recipe) { super(recipe); }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        var result = super.assemble(input, registries);
        result.remove(KaleidoscopePufferfish.COLOR.get());
        return result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() { return KaleidoscopePufferfish.MATERIAL_RECIPE.get(); }

    public static final class Serializer implements RecipeSerializer<PufferfishMaterialRecipe> {
        private static final MapCodec<PufferfishMaterialRecipe> CODEC = ShapedRecipe.Serializer.CODEC.xmap(PufferfishMaterialRecipe::new, recipe -> recipe);
        private static final StreamCodec<RegistryFriendlyByteBuf, PufferfishMaterialRecipe> STREAM_CODEC =
                ShapedRecipe.Serializer.STREAM_CODEC.map(PufferfishMaterialRecipe::new, recipe -> recipe);
        @Override public MapCodec<PufferfishMaterialRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, PufferfishMaterialRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
