// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class CopperPufferfishWaxRecipe extends CustomRecipe {
    public CopperPufferfishWaxRecipe(CraftingBookCategory category) { super(category); }

    /// Checks ingredients without copying or converting the fish or skin.
    @Override
    public boolean matches(CraftingInput input, Level level) {
        return !findFish(input).isEmpty();
    }

    /// Borrows the unwaxed fish or skin from a valid honeycomb recipe.
    private static ItemStack findFish(CraftingInput input) {
        ItemStack fish = ItemStack.EMPTY;
        boolean honeycomb = false;
        for (var stack : input.items()) {
            if (stack.isEmpty()) continue;
            int index = CopperPufferfish.index(stack);
            if (index < 0) index = CopperPufferfish.skinIndex(stack);
            if (index >= 0 && index < 4 && fish.isEmpty()) fish = stack;
            else if (stack.is(Items.HONEYCOMB) && !honeycomb) honeycomb = true;
            else return ItemStack.EMPTY;
        }
        return honeycomb ? fish : ItemStack.EMPTY;
    }

    /// Waxes one fish or skin, preserving its components and leaving the ingredients untouched.
    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        var fish = findFish(input);
        if (fish.isEmpty()) return ItemStack.EMPTY;
        int skin = CopperPufferfish.skinIndex(fish);
        var result = skin >= 0 ? PufferfishBlocks.COPPER_SKINS.get(skin + 4).get().asItem()
                : PufferfishItems.COPPER_VARIANTS.get(CopperPufferfish.index(fish) + 4).get();
        return fish.transmuteCopy(result, 1);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) { return width * height >= 2; }

    @Override
    public RecipeSerializer<?> getSerializer() { return PufferfishPlease.WAX_RECIPE.get(); }
}
