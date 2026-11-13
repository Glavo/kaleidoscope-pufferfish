// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/// Data-driven table processing; only explicitly opted-in recipes transfer rank to skin outputs.
public record FishProcessingRecipe(Ingredient ingredient, Ingredient tool, int strokes,
        List<ItemStack> results, boolean inheritSkinLevel) implements Recipe<FishProcessingRecipe.Input> {
    public static final MapCodec<FishProcessingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(FishProcessingRecipe::ingredient),
            Ingredient.CODEC_NONEMPTY.fieldOf("tool").forGetter(FishProcessingRecipe::tool),
            Codec.intRange(1, 64).optionalFieldOf("strokes", 3).forGetter(FishProcessingRecipe::strokes),
            ItemStack.CODEC.listOf(1, 8).fieldOf("results").forGetter(FishProcessingRecipe::results),
            Codec.BOOL.optionalFieldOf("inherit_skin_level", false).forGetter(FishProcessingRecipe::inheritSkinLevel)
    ).apply(instance, FishProcessingRecipe::new));

    public record Input(ItemStack fish, ItemStack tool) implements RecipeInput {
        @Override public ItemStack getItem(int index) {
            return switch (index) { case 0 -> fish; case 1 -> tool; default -> throw new IllegalArgumentException("Invalid input slot"); };
        }
        @Override public int size() { return 2; }
    }

    public List<ItemStack> process(ItemStack fish) {
        return results.stream().map(result -> inheritSkinLevel
                ? PufferfishSkinBlock.inheritLevel(fish, result.copy()) : result.copy()).toList();
    }

    @Override public boolean matches(Input input, Level level) { return ingredient.test(input.fish()) && tool.test(input.tool()); }
    @Override public ItemStack assemble(Input input, HolderLookup.Provider registries) { return process(input.fish()).getFirst(); }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return results.getFirst().copy(); }
    @Override public boolean canCraftInDimensions(int width, int height) { return true; }
    @Override public boolean isSpecial() { return true; }
    @Override public RecipeType<?> getType() { return PufferfishPlease.FISH_PROCESSING_TYPE.get(); }
    @Override public RecipeSerializer<?> getSerializer() { return PufferfishPlease.FISH_PROCESSING_RECIPE.get(); }

    public static final class Serializer implements RecipeSerializer<FishProcessingRecipe> {
        @Override public MapCodec<FishProcessingRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, FishProcessingRecipe> streamCodec() {
            return ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());
        }
    }
}
