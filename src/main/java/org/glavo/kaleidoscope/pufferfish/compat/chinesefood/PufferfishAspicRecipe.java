// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.compat.chinesefood;

import com.bmt.kaleidoscope_chinesefood.crafting.FreezerInput;
import com.bmt.kaleidoscope_chinesefood.crafting.RefrigeratingRecipe;
import com.github.ysbbbbbb.kaleidoscopecookery.item.quality.QualityUtils;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

/// Refrigerates broth without discarding its cooking quality.
public final class PufferfishAspicRecipe extends RefrigeratingRecipe {
    public PufferfishAspicRecipe(Ingredient input, ItemStack output, int time) { super(input, output, time); }

    @Override
    public ItemStack assemble(FreezerInput input, HolderLookup.Provider registries) {
        var result = super.assemble(input, registries);
        if (QualityUtils.hasQuality(input.getItem(0))) QualityUtils.setQuality(result, QualityUtils.getQuality(input.getItem(0)));
        return result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() { return ChineseFoodIntegration.ASPIC_RECIPE.get(); }

    public static final class Serializer implements RecipeSerializer<PufferfishAspicRecipe> {
        private static final MapCodec<PufferfishAspicRecipe> CODEC = buildCodec(PufferfishAspicRecipe::new);
        private static final StreamCodec<RegistryFriendlyByteBuf, PufferfishAspicRecipe> STREAM_CODEC = buildStreamCodec(PufferfishAspicRecipe::new);

        @Override
        public MapCodec<PufferfishAspicRecipe> codec() { return CODEC; }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, PufferfishAspicRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
