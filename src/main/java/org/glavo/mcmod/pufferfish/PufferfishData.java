// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.glavo.mcmod.pufferfish.entity.MaterialPufferfish;

/// Bridges live fish and item data without changing their persistent formats.
public final class PufferfishData {
    private PufferfishData() {}

    /// Borrowed, read-only storage; empty for ordinary fish without individual data.
    public static ItemStack stored(Pufferfish fish) {
        if (fish instanceof MaterialPufferfish material) return material.fish();
        var stack = fish.getExistingDataOrNull(PufferfishPlease.FISH_ITEM);
        return stack == null ? ItemStack.EMPTY : stack;
    }

    /// A detached item snapshot with the live fish's current color.
    public static ItemStack copy(Pufferfish fish) {
        var stored = stored(fish);
        var stack = stored.isEmpty() ? new ItemStack(Items.PUFFERFISH) : stored.copyWithCount(1);
        if (fish instanceof MaterialPufferfish) return stack;
        var color = PufferfishColors.get(fish);
        if (color != null) return PufferfishColors.dye(stack, color);
        stack.remove(PufferfishPlease.COLOR.get());
        var name = stack.get(DataComponents.ITEM_NAME);
        if (name != null && name.getContents() instanceof TranslatableContents translated
                && translated.getKey().equals("item.pufferfish_please.dyed_pufferfish")) stack.remove(DataComponents.ITEM_NAME);
        return stack;
    }

    /// Commits a snapshot and refreshes derived live state. The input is never retained.
    public static void set(Pufferfish fish, ItemStack stack) {
        if (fish instanceof MaterialPufferfish material) {
            material.setFish(stack);
        } else if (stack.is(Items.PUFFERFISH)) {
            fish.setData(PufferfishPlease.FISH_ITEM, stack.copyWithCount(1));
            PufferfishColors.set(fish, PufferfishColors.get(stack));
            PufferfishSize.update(fish);
            PufferfishHealth.update(fish);
        }
    }
}
