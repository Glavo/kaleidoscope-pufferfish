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
import org.glavo.mcmod.pufferfish.PufferfishCultivation;
import org.glavo.mcmod.pufferfish.PufferfishDurability;
import org.glavo.mcmod.pufferfish.PufferfishLevel;
import org.glavo.mcmod.pufferfish.PufferfishPlease;
import org.glavo.mcmod.pufferfish.PufferfishVariant;

public final class CuttingAssertions {
    private CuttingAssertions() {}

    public static ItemStack fish(DyeColor color) {
        var stack = new ItemStack(Items.PUFFERFISH, 2);
        return color == null ? stack : PufferfishColors.dye(stack, color);
    }

    public static void verifyDrops(GameTestHelper helper, BlockPos pos, PufferfishSkinBlock skin) {
        verifyDrops(helper, pos, skin, PufferfishLevel.LEVEL_0);
    }

    public static ItemStack rankedFish(PufferfishVariant variant, int individual) {
        var fish = variant.fish();
        PufferfishDurability.activate(fish);
        fish.setDamageValue(individual + 1);
        fish.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Fish " + individual));
        fish.set(PufferfishPlease.CULTIVATION.get(), new PufferfishCultivation(PufferfishLevel.LEVEL_10,
                individual == 0 ? 0.1 : 0.9, (individual + 1L) * 10_000_000, (individual + 2L) * 30_000_000));
        return fish;
    }

    public static void verifyDrops(GameTestHelper helper, BlockPos pos, PufferfishSkinBlock skin, PufferfishLevel rank) {
        var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2));
        helper.assertTrue(drops.size() == 2
                && drops.stream().allMatch(entity -> entity.getItem().getCount() == 1)
                && drops.stream().anyMatch(entity -> entity.getItem().is(PufferfishItems.SASHIMI.get()))
                && drops.stream().anyMatch(entity -> entity.getItem().is(skin.asItem())),
                "Cutting must drop exactly one sashimi and one matching skin");
        var result = drops.stream().map(ItemEntity::getItem).filter(stack -> stack.is(skin.asItem())).findFirst().orElseThrow();
        var expected = new ItemStack(skin);
        if (rank != PufferfishLevel.LEVEL_0) expected.set(PufferfishPlease.SKIN_LEVEL.get(), rank);
        helper.assertTrue(ItemStack.matches(expected, result) && result.getMaxStackSize() == 64,
                "Skin did not retain only rank or became unstackable");
        var food = drops.stream().map(ItemEntity::getItem).filter(stack -> stack.is(PufferfishItems.SASHIMI.get())).findFirst().orElseThrow();
        helper.assertTrue(ItemStack.matches(food, new ItemStack(PufferfishItems.SASHIMI.get())), "Sashimi inherited individual fish data");
        drops.forEach(ItemEntity::discard);
    }
}
