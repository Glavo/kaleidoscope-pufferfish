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

/// Turns a fish and one water bucket into a live fish bucket, consuming the original bucket.
public final class PufferfishBucketRecipe extends CustomRecipe {
    public PufferfishBucketRecipe(CraftingBookCategory category) { super(category); }

    @Override
    public boolean matches(CraftingInput input, Level level) { return !assemble(input, level.registryAccess()).isEmpty(); }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack fish = ItemStack.EMPTY;
        boolean water = false;
        for (var stack : input.items()) {
            if (stack.isEmpty()) continue;
            if (stack.is(Items.WATER_BUCKET) && !water) water = true;
            else if (PufferfishItems.isPufferfish(stack) && fish.isEmpty()) fish = stack.copyWithCount(1);
            else return ItemStack.EMPTY;
        }
        if (fish.isEmpty() || !water) return ItemStack.EMPTY;
        var variant = PufferfishMobs.variant(fish);
        var bucket = new ItemStack(variant == null ? Items.PUFFERFISH_BUCKET : variant.bucket().get());
        var stored = fish;
        CustomData.update(DataComponents.BUCKET_ENTITY_DATA, bucket, tag -> tag.put("Fish", stored.save(registries)));
        if (variant == null && PufferfishColors.get(fish) != null) PufferfishColors.dye(bucket, PufferfishColors.get(fish));
        if (fish.has(DataComponents.CUSTOM_NAME)) bucket.set(DataComponents.CUSTOM_NAME, fish.get(DataComponents.CUSTOM_NAME));
        return bucket;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) { return NonNullList.withSize(input.size(), ItemStack.EMPTY); }

    @Override
    public boolean canCraftInDimensions(int width, int height) { return width * height >= 2; }

    @Override
    public RecipeSerializer<?> getSerializer() { return PufferfishPlease.BUCKET_RECIPE.get(); }
}
