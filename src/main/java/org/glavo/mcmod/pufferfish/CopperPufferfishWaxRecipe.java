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

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return !assemble(input, level.registryAccess()).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
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
        if (fish.isEmpty() || !honeycomb) return ItemStack.EMPTY;
        int skin = CopperPufferfish.skinIndex(fish);
        return skin >= 0 ? fish.copyWithCount(1).transmuteCopy(PufferfishBlocks.COPPER_SKINS.get(skin + 4).get().asItem())
                : CopperPufferfish.convert(fish.copyWithCount(1), CopperPufferfish.index(fish) + 4);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) { return width * height >= 2; }

    @Override
    public RecipeSerializer<?> getSerializer() { return PufferfishPlease.WAX_RECIPE.get(); }
}
