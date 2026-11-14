// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/// Turns a fish and one water bucket into a live fish bucket, consuming the original bucket.
public final class PufferfishBucketRecipe extends CustomRecipe {
    public PufferfishBucketRecipe(CraftingBookCategory category) { super(category); }

    /// Checks ingredients without creating or serializing a bucket.
    @Override
    public boolean matches(CraftingInput input, Level level) { return !findFish(input).isEmpty(); }

    /// Borrows the fish from a valid fish-and-water-bucket input, or returns an empty stack.
    private static ItemStack findFish(CraftingInput input) {
        ItemStack fish = ItemStack.EMPTY;
        boolean water = false;
        for (var stack : input.items()) {
            if (stack.isEmpty()) continue;
            if (stack.is(Items.WATER_BUCKET) && !water) water = true;
            else if (PufferfishItems.isPufferfish(stack) && fish.isEmpty()) fish = stack;
            else return ItemStack.EMPTY;
        }
        return water ? fish : ItemStack.EMPTY;
    }

    /// Stores one fish's data in its matching bucket without modifying the ingredients.
    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        var fish = findFish(input);
        if (fish.isEmpty()) return ItemStack.EMPTY;
        @Nullable var variant = PufferfishMobs.variant(fish);
        var bucket = new ItemStack(variant == null ? Items.PUFFERFISH_BUCKET : variant.bucket().get());
        var stored = fish.copyWithCount(1);
        CustomData.update(DataComponents.BUCKET_ENTITY_DATA, bucket, tag -> tag.put("Fish", stored.save(registries)));
        @Nullable var color = PufferfishColors.get(fish);
        if (variant == null && color != null) PufferfishColors.dye(bucket, color);
        @Nullable var name = fish.get(DataComponents.CUSTOM_NAME);
        if (name != null) bucket.set(DataComponents.CUSTOM_NAME, name);
        return bucket;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) { return NonNullList.withSize(input.size(), ItemStack.EMPTY); }

    @Override
    public boolean canCraftInDimensions(int width, int height) { return width * height >= 2; }

    @Override
    public RecipeSerializer<?> getSerializer() { return PufferfishPlease.BUCKET_RECIPE.get(); }
}
