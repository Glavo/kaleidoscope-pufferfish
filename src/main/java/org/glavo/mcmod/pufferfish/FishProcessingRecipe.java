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
import org.jetbrains.annotations.Unmodifiable;

/// Data-driven table processing; only explicitly opted-in recipes transfer rank to skin outputs.
public record FishProcessingRecipe(Ingredient ingredient, Ingredient tool, int strokes,
        List<ItemStack> results, boolean inheritSkinLevel) implements Recipe<FishProcessingRecipe.Input> {
    public static final MapCodec<FishProcessingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(FishProcessingRecipe::ingredient),
            Ingredient.CODEC_NONEMPTY.fieldOf("tool").forGetter(FishProcessingRecipe::tool),
            Codec.intRange(1, 64).optionalFieldOf("strokes", 1).forGetter(FishProcessingRecipe::strokes),
            ItemStack.CODEC.listOf(1, 8).fieldOf("results").forGetter(FishProcessingRecipe::results),
            Codec.BOOL.optionalFieldOf("inherit_skin_level", false).forGetter(FishProcessingRecipe::inheritSkinLevel)
    ).apply(instance, FishProcessingRecipe::new));

    public record Input(ItemStack fish, ItemStack tool) implements RecipeInput {
        @Override public ItemStack getItem(int index) {
            return switch (index) { case 0 -> fish; case 1 -> tool; default -> throw new IllegalArgumentException("Invalid input slot"); };
        }
        @Override public int size() { return 2; }
    }

    /// Creates detached outputs, transferring rank only to eligible skins.
    public @Unmodifiable List<ItemStack> process(ItemStack fish) {
        return results.stream().map(result -> createResult(fish, result)).toList();
    }

    /// Copies one result and applies this recipe's rank inheritance policy.
    private ItemStack createResult(ItemStack fish, ItemStack result) {
        return inheritSkinLevel ? PufferfishSkinBlock.inheritLevel(fish, result) : result.copy();
    }

    @Override public boolean matches(Input input, Level level) { return ingredient.test(input.fish()) && tool.test(input.tool()); }
    /// Returns the primary output with the same rank inheritance as full processing.
    @Override public ItemStack assemble(Input input, HolderLookup.Provider registries) { return createResult(input.fish(), results.getFirst()); }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return results.getFirst().copy(); }
    @Override public boolean canCraftInDimensions(int width, int height) { return true; }
    @Override public boolean isSpecial() { return true; }
    @Override public RecipeType<?> getType() { return PufferfishPlease.FISH_PROCESSING_TYPE.get(); }
    @Override public RecipeSerializer<?> getSerializer() { return PufferfishPlease.FISH_PROCESSING_RECIPE.get(); }

    public static final class Serializer implements RecipeSerializer<FishProcessingRecipe> {
        /// Shares the data-pack format for recipe synchronization.
        private static final StreamCodec<RegistryFriendlyByteBuf, FishProcessingRecipe> STREAM_CODEC =
                ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());

        @Override public MapCodec<FishProcessingRecipe> codec() { return CODEC; }
        /// Reuses the registry-aware network codec across synchronizations.
        @Override public StreamCodec<RegistryFriendlyByteBuf, FishProcessingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
