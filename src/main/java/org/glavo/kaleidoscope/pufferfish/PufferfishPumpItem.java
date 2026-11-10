// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/// A reusable foot pump that retains its item metadata when placed and recovered.
public final class PufferfishPumpItem extends BlockItem {

    public PufferfishPumpItem(Properties properties) { super(PufferfishBlocks.PUMP.get(), properties); }

    public static boolean canInflate(ItemStack fish) {
        return PufferfishItems.isPufferfish(fish) && fish.getCount() == 1 && fish.isDamaged();
    }

    static void removeDurability(ItemStack stack) {
        stack.remove(DataComponents.MAX_DAMAGE);
        stack.remove(DataComponents.DAMAGE);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!level.isClientSide) removeDurability(stack);
    }
}
