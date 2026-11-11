// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.test;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import org.glavo.mcmod.pufferfish.PufferfishColors;
import org.glavo.mcmod.pufferfish.PufferfishItems;
import org.glavo.mcmod.pufferfish.PufferfishSkinBlock;

public final class CuttingAssertions {
    private CuttingAssertions() {}

    public static ItemStack fish(DyeColor color) {
        var stack = new ItemStack(Items.PUFFERFISH, 2);
        return color == null ? stack : PufferfishColors.dye(stack, color);
    }

    public static void verifyDrops(GameTestHelper helper, BlockPos pos, PufferfishSkinBlock skin) {
        var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2));
        helper.assertTrue(drops.size() == 2
                && drops.stream().allMatch(entity -> entity.getItem().getCount() == 1)
                && drops.stream().anyMatch(entity -> entity.getItem().is(PufferfishItems.SASHIMI.get()))
                && drops.stream().anyMatch(entity -> entity.getItem().is(skin.asItem())),
                "Cutting must drop exactly one sashimi and one matching skin");
        drops.forEach(ItemEntity::discard);
    }
}
