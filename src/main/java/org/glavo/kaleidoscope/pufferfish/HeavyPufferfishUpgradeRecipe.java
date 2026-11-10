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

/// Upgrades one netherite pufferfish with one heavy core and a pufferfish upgrade template.
public final class HeavyPufferfishUpgradeRecipe implements SmithingRecipe {
    /// Creates the fixed-ingredient heavy pufferfish upgrade recipe.
    public HeavyPufferfishUpgradeRecipe() {}

    @Override
    public boolean matches(SmithingRecipeInput input, Level level) {
        return isTemplateIngredient(input.template()) && isBaseIngredient(input.base()) && isAdditionIngredient(input.addition());
    }

    /// Creates one heavy pufferfish preserving the base stack's component changes, or an empty stack for invalid input.
    @Override
    public ItemStack assemble(SmithingRecipeInput input, HolderLookup.Provider registries) {
        return matches(input, null) ? PufferfishDurability.transfer(input.base(), input.base().transmuteCopy(PufferfishItems.HEAVY.get(), 1)) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return new ItemStack(PufferfishItems.HEAVY.get());
    }

    @Override
    public boolean isTemplateIngredient(ItemStack stack) { return stack.is(PufferfishItems.UPGRADE_TEMPLATE.get()); }

    @Override
    public boolean isBaseIngredient(ItemStack stack) { return PufferfishItems.isNetherite(stack); }

    @Override
    public boolean isAdditionIngredient(ItemStack stack) { return stack.is(Items.HEAVY_CORE); }

    /// Returns false because all required ingredients are always registered.
    @Override
    public boolean isIncomplete() { return false; }

    @Override
    public RecipeSerializer<?> getSerializer() { return KaleidoscopePufferfish.HEAVY_UPGRADE.get(); }

    /// Encodes the fixed recipe without additional data fields or network payload.
    public static final class Serializer implements RecipeSerializer<HeavyPufferfishUpgradeRecipe> {
        /// The recipe's data-pack codec.
        private static final MapCodec<HeavyPufferfishUpgradeRecipe> CODEC = MapCodec.unit(HeavyPufferfishUpgradeRecipe::new);
        /// The recipe's network codec.
        private static final StreamCodec<RegistryFriendlyByteBuf, HeavyPufferfishUpgradeRecipe> STREAM_CODEC =
                StreamCodec.of((buffer, recipe) -> {}, buffer -> new HeavyPufferfishUpgradeRecipe());

        /// Creates the fixed-recipe serializer.
        public Serializer() {}

        @Override
        public MapCodec<HeavyPufferfishUpgradeRecipe> codec() { return CODEC; }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, HeavyPufferfishUpgradeRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
