// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;

/// Upgrades one netherite pufferfish with one nether star and an empty smithing template slot.
public final class RadiantPufferfishUpgradeRecipe implements SmithingRecipe {
    /// Creates the fixed-ingredient radiant pufferfish upgrade recipe.
    public RadiantPufferfishUpgradeRecipe() {}

    @Override
    public boolean matches(SmithingRecipeInput input, Level level) {
        return input.template().isEmpty() && isBaseIngredient(input.base()) && isAdditionIngredient(input.addition());
    }

    /// Creates one radiant pufferfish preserving the base stack's component changes, or an empty stack for invalid input.
    @Override
    public ItemStack assemble(SmithingRecipeInput input, HolderLookup.Provider registries) {
        return matches(input, null) ? input.base().transmuteCopy(PufferfishItems.RADIANT.get(), 1) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return new ItemStack(PufferfishItems.RADIANT.get());
    }

    /// Returns false because this recipe does not accept a template item.
    @Override
    public boolean isTemplateIngredient(ItemStack stack) { return false; }

    @Override
    public boolean isBaseIngredient(ItemStack stack) { return PufferfishItems.isNetherite(stack); }

    @Override
    public boolean isAdditionIngredient(ItemStack stack) { return stack.is(Items.NETHER_STAR); }

    /// Returns false because both required ingredients are always registered.
    @Override
    public boolean isIncomplete() { return false; }

    @Override
    public RecipeSerializer<?> getSerializer() { return KaleidoscopePufferfish.RADIANT_UPGRADE.get(); }

    /// Encodes the fixed recipe without additional data fields or network payload.
    public static final class Serializer implements RecipeSerializer<RadiantPufferfishUpgradeRecipe> {
        /// The recipe's data-pack codec.
        private static final MapCodec<RadiantPufferfishUpgradeRecipe> CODEC = MapCodec.unit(RadiantPufferfishUpgradeRecipe::new);
        /// The recipe's network codec.
        private static final StreamCodec<RegistryFriendlyByteBuf, RadiantPufferfishUpgradeRecipe> STREAM_CODEC =
                StreamCodec.of((buffer, recipe) -> {}, buffer -> new RadiantPufferfishUpgradeRecipe());

        /// Creates the fixed-recipe serializer.
        public Serializer() {}

        @Override
        public MapCodec<RadiantPufferfishUpgradeRecipe> codec() { return CODEC; }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, RadiantPufferfishUpgradeRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
