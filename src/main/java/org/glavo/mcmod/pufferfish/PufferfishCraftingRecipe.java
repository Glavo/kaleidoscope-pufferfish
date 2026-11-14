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
import org.jetbrains.annotations.NotNullByDefault;

/// Crafts fish upgrades and flails while preserving component changes and proportional wear.
@NotNullByDefault
public final class PufferfishCraftingRecipe extends ShapedRecipe {
    /// Identifies the recipe and whether its result retains dye.
    private final Serializer serializer;

    /// Retains the vanilla shaped pattern, output, and recipe-book settings.
    private PufferfishCraftingRecipe(ShapedRecipe recipe, Serializer serializer) {
        super(recipe.getGroup(), recipe.category(), recipe.pattern, recipe.getResultItem(null), recipe.showNotification());
        this.serializer = serializer;
    }

    /// Copies one ingredient fish's data, replacing its generated name with the result's name.
    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        if (!pattern.matches(input)) return ItemStack.EMPTY;
        for (var stack : input.items()) {
            if (PufferfishItems.isPufferfish(stack)) {
                var result = stack.transmuteCopy(getResultItem(registries).getItem(), 1);
                PufferfishColors.clearGeneratedName(result);
                if (!serializer.retainDye) result.remove(PufferfishPlease.COLOR.get());
                return PufferfishDurability.transfer(stack, result);
            }
        }
        return ItemStack.EMPTY;
    }

    /// Preserves the registered recipe type used by existing data packs.
    @Override
    public RecipeSerializer<?> getSerializer() { return serializer; }

    /// Uses vanilla shaped-recipe formats with a fixed dye policy per registered type.
    @NotNullByDefault
    public static final class Serializer implements RecipeSerializer<PufferfishCraftingRecipe> {
        /// Flails retain dye; material upgrades discard it.
        private final boolean retainDye;
        /// Vanilla shaped data with component-preserving assembly.
        private final MapCodec<PufferfishCraftingRecipe> codec;
        /// Vanilla shaped recipe synchronization.
        private final StreamCodec<RegistryFriendlyByteBuf, PufferfishCraftingRecipe> streamCodec;

        /// Binds the result's dye policy to this recipe type.
        public Serializer(boolean retainDye) {
            this.retainDye = retainDye;
            codec = ShapedRecipe.Serializer.CODEC.xmap(recipe -> new PufferfishCraftingRecipe(recipe, this), recipe -> recipe);
            streamCodec = ShapedRecipe.Serializer.STREAM_CODEC.map(recipe -> new PufferfishCraftingRecipe(recipe, this), recipe -> recipe);
        }

        /// Returns the data-pack codec.
        @Override public MapCodec<PufferfishCraftingRecipe> codec() { return codec; }

        /// Returns the network codec.
        @Override public StreamCodec<RegistryFriendlyByteBuf, PufferfishCraftingRecipe> streamCodec() { return streamCodec; }
    }
}
