// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.minecraft.world.level.Level;

public record NetheritePufferfishUpgradeRecipe(SmithingTransformRecipe delegate) implements SmithingRecipe {
    @Override
    public boolean matches(SmithingRecipeInput input, Level level) {
        return delegate.matches(input, level);
    }

    @Override
    public ItemStack assemble(SmithingRecipeInput input, HolderLookup.Provider registries) {
        var result = delegate.assemble(input, registries);
        result.remove(PufferfishPlease.COLOR.get());
        var name = result.get(DataComponents.ITEM_NAME);
        if (name != null && name.getContents() instanceof TranslatableContents translated
                && translated.getKey().equals("item.pufferfish_please.dyed_pufferfish")) {
            result.remove(DataComponents.ITEM_NAME);
        }
        return PufferfishDurability.transfer(input.base(), result);
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return delegate.getResultItem(registries);
    }

    @Override
    public boolean isTemplateIngredient(ItemStack stack) { return delegate.isTemplateIngredient(stack); }

    @Override
    public boolean isBaseIngredient(ItemStack stack) { return delegate.isBaseIngredient(stack); }

    @Override
    public boolean isAdditionIngredient(ItemStack stack) { return delegate.isAdditionIngredient(stack); }

    @Override
    public boolean isIncomplete() { return delegate.isIncomplete(); }

    @Override
    public RecipeSerializer<?> getSerializer() { return PufferfishPlease.NETHERITE_UPGRADE.get(); }

    public static final class Serializer implements RecipeSerializer<NetheritePufferfishUpgradeRecipe> {
        private static final MapCodec<NetheritePufferfishUpgradeRecipe> CODEC = RecipeSerializer.SMITHING_TRANSFORM.codec()
                .xmap(NetheritePufferfishUpgradeRecipe::new, NetheritePufferfishUpgradeRecipe::delegate);
        private static final StreamCodec<RegistryFriendlyByteBuf, NetheritePufferfishUpgradeRecipe> STREAM_CODEC =
                RecipeSerializer.SMITHING_TRANSFORM.streamCodec()
                        .map(NetheritePufferfishUpgradeRecipe::new, NetheritePufferfishUpgradeRecipe::delegate);

        @Override
        public MapCodec<NetheritePufferfishUpgradeRecipe> codec() { return CODEC; }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, NetheritePufferfishUpgradeRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
