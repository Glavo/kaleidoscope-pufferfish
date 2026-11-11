// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

public record PufferfishIngredient(Optional<DyeColor> color) implements ICustomIngredient {
    public static final MapCodec<PufferfishIngredient> CODEC = DyeColor.CODEC.optionalFieldOf("color")
            .xmap(PufferfishIngredient::new, PufferfishIngredient::color);

    @Override
    public boolean test(ItemStack stack) {
        return stack.is(Items.PUFFERFISH) && PufferfishColors.get(stack) == color.orElse(null);
    }

    @Override
    public Stream<ItemStack> getItems() {
        var stack = new ItemStack(Items.PUFFERFISH);
        color.ifPresent(value -> PufferfishColors.dye(stack, value));
        return Stream.of(stack);
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public IngredientType<?> getType() {
        return PufferfishPlease.PUFFERFISH_INGREDIENT.get();
    }
}
