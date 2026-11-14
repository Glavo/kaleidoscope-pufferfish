// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import com.mojang.serialization.MapCodec;
import java.util.function.Supplier;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNullByDefault;

/// Upgrades a netherite pufferfish while preserving its components and durability fraction.
@NotNullByDefault
public record PufferfishUpgradeRecipe(Item addition, Item result, RecipeSerializer<?> serializer) implements SmithingRecipe {
    /// Requires the pufferfish template, a netherite fish, and this upgrade's addition.
    @Override
    public boolean matches(SmithingRecipeInput input, Level level) { return accepts(input); }

    /// Validates ingredients without requiring a world during result assembly.
    private boolean accepts(SmithingRecipeInput input) {
        return isTemplateIngredient(input.template()) && isBaseIngredient(input.base()) && isAdditionIngredient(input.addition());
    }

    /// Produces one upgraded fish without modifying the input.
    @Override
    public ItemStack assemble(SmithingRecipeInput input, HolderLookup.Provider registries) {
        return accepts(input) ? PufferfishDurability.transfer(input.base(), input.base().transmuteCopy(result, 1)) : ItemStack.EMPTY;
    }

    /// Returns the unmodified recipe-book preview.
    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) { return new ItemStack(result); }

    /// Accepts the shared pufferfish upgrade template.
    @Override
    public boolean isTemplateIngredient(ItemStack stack) { return stack.is(PufferfishItems.UPGRADE_TEMPLATE.get()); }

    /// Accepts a netherite pufferfish.
    @Override
    public boolean isBaseIngredient(ItemStack stack) { return PufferfishItems.isNetherite(stack); }

    /// Accepts this upgrade's addition.
    @Override
    public boolean isAdditionIngredient(ItemStack stack) { return stack.is(addition); }

    /// Fixed ingredients are always registered.
    @Override
    public boolean isIncomplete() { return false; }

    /// Retains the registered recipe type for this upgrade.
    @Override
    public RecipeSerializer<?> getSerializer() { return serializer; }

    /// Keeps fixed upgrades fieldless in data packs and on the network.
    @NotNullByDefault
    public static final class Serializer implements RecipeSerializer<PufferfishUpgradeRecipe> {
        /// Resolves item suppliers after registries have loaded.
        private final MapCodec<PufferfishUpgradeRecipe> codec;
        /// Transmits no data because the serializer identifies the upgrade.
        private final StreamCodec<RegistryFriendlyByteBuf, PufferfishUpgradeRecipe> streamCodec;

        /// Binds one ingredient and result to the registered serializer.
        public Serializer(Item addition, Supplier<? extends Item> result) {
            Supplier<PufferfishUpgradeRecipe> factory = () -> new PufferfishUpgradeRecipe(addition, result.get(), this);
            codec = MapCodec.unit(factory);
            streamCodec = StreamCodec.of((buffer, recipe) -> {}, buffer -> factory.get());
        }

        /// Reads and writes fieldless recipe data.
        @Override
        public MapCodec<PufferfishUpgradeRecipe> codec() { return codec; }

        /// Reads and writes fieldless recipe packets.
        @Override
        public StreamCodec<RegistryFriendlyByteBuf, PufferfishUpgradeRecipe> streamCodec() { return streamCodec; }
    }
}
