// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

/// A reusable foot pump that retains its item metadata when placed and recovered.
public final class PufferfishPumpItem extends BlockItem {

    public PufferfishPumpItem(Properties properties) { super(PufferfishBlocks.PUMP.get(), properties); }

    public static boolean canInflate(ItemStack fish) {
        return PufferfishItems.isPufferfish(fish) && fish.getCount() == 1 && fish.isDamaged();
    }
}
