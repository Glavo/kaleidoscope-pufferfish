// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class PufferfishDyeRecipe extends CustomRecipe {
    public PufferfishDyeRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return !assemble(input, level.registryAccess()).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack fish = ItemStack.EMPTY;
        DyeColor color = null;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (PufferfishColors.supports(stack) && fish.isEmpty()) {
                fish = stack;
            } else if (DyeColor.getColor(stack) != null && color == null) {
                color = DyeColor.getColor(stack);
            } else {
                return ItemStack.EMPTY;
            }
        }
        return fish.isEmpty() || color == null ? ItemStack.EMPTY : PufferfishColors.dye(fish.copyWithCount(1), color);
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
        return KaleidoscopePufferfish.DYE_RECIPE.get();
    }
}
