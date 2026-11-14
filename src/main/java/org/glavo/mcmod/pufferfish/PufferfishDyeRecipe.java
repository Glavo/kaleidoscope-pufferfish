// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

public class PufferfishDyeRecipe extends CustomRecipe {
    public PufferfishDyeRecipe(CraftingBookCategory category) {
        super(category);
    }

    /// Checks ingredients without copying fish data or updating bucket NBT.
    @Override
    public boolean matches(CraftingInput input, Level level) {
        return ingredients(input) != null;
    }

    /// A borrowed fish and the single dye selected for this craft.
    @NotNullByDefault
    private record Ingredients(ItemStack fish, DyeColor color) {}

    /// Validates exactly one dyeable fish or bucket and one dye.
    private static @Nullable Ingredients ingredients(CraftingInput input) {
        ItemStack fish = ItemStack.EMPTY;
        @Nullable DyeColor color = null;
        for (var stack : input.items()) {
            if (stack.isEmpty()) {
                continue;
            }
            if (PufferfishColors.supports(stack) && fish.isEmpty()) {
                fish = stack;
            } else if (color == null) {
                color = DyeColor.getColor(stack);
                if (color == null) return null;
            } else {
                return null;
            }
        }
        return fish.isEmpty() || color == null ? null : new Ingredients(fish, color);
    }

    /// Dyes a single copy, preserving custom names and bucket contents.
    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        @Nullable var ingredients = ingredients(input);
        return ingredients == null ? ItemStack.EMPTY
                : PufferfishColors.dye(ingredients.fish().copyWithCount(1), ingredients.color());
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        // The filled bucket is the result; returning its remainder would duplicate buckets.
        return NonNullList.withSize(input.size(), ItemStack.EMPTY);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return PufferfishPlease.DYE_RECIPE.get();
    }
}
